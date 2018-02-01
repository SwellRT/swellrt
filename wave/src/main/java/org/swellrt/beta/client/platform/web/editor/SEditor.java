package org.swellrt.beta.client.platform.web.editor;


import java.util.function.Consumer;

import org.swellrt.beta.client.ServiceConnection;
import org.swellrt.beta.client.ServiceConnection.ConnectionHandler;
import org.swellrt.beta.client.ServiceConstants;
import org.swellrt.beta.client.platform.web.ServiceEntryPoint;
import org.swellrt.beta.client.platform.web.editor.annotation.AnnotationController;
import org.swellrt.beta.client.platform.web.editor.annotation.AnnotationRegistry;
import org.swellrt.beta.client.platform.web.editor.annotation.AnnotationValue;
import org.swellrt.beta.client.platform.web.editor.caret.CaretManager;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorImplWebkitMobile;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.FullContentView;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.Pretty;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.StringMap;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.DOM;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Editor")
public class SEditor implements EditorUpdateListener {


  public final static String COMPAT_MODE_NONE = "none";
  public final static String COMPAT_MODE_READONLY = "readonly";
  public final static String COMPAT_MODE_EDIT = "edit";

  @JsFunction
  public interface SelectionChangeHandler {
    void exec(SRange range, SEditor editor, SSelection node);
  }

  private static StringMap<SEditor> editors = CollectionUtils.createStringMap();

  /** Configure editor component with custom settings */
  public static void configure(SEditorConfig config) {
    if (config == null)
      return;
    SEditorStatics.setConfig(config);
  }

  /** create a editor instance of get an existing one */
  public static SEditor get(String elementId) {

    if (!SEditorStatics.isConfigured()) {
      // Just pass a empty javascript object. SEditorConfig overlay methods
      // will provide default values.
      SEditorStatics.setConfigDefault();
    }

    if (editors.containsKey(elementId)) {
      return editors.get(elementId);
    }

    Element containerElement = DOM.getElementById(elementId);

    if (containerElement == null) {
      throw new IllegalStateException("Element with id=" + elementId + " not found");
    }

    if (!containerElement.getNodeName().equalsIgnoreCase("div")) {
      throw new IllegalStateException("Element with id=" + elementId + " is not a DIV");
    }

    SEditor editor = new SEditor(containerElement);
    editors.put(elementId, editor);

    return editor;
  }


  public static SEditor create() {
    SEditor se = new SEditor();
    return se;
  }


  private LogicalPanel.Impl editorPanel;

  /** Don't use this prop directly, use getter instead */
  private EditorImpl editor;

  /** A service to listen to connection events */
  private final ServiceConnection service;

  private CaretManager caretManager;

  private SelectionChangeHandler selectionHandler = null;

  private boolean canEdit = true;

  private ConnectionHandler connectionHandler = new ConnectionHandler() {

    @Override
    public void exec(String state, SException e) {

      if (editor == null)
        return;

      canEdit = state.equals(ServiceConstants.STATUS_CONNECTED);
      edit(canEdit);
    }

  };

  /**
   * Create editor instance tied to a DOM element
   * @param containerElement
   */
  protected SEditor(final Element containerElement) {
    this.editorPanel = new LogicalPanel.Impl() {
      {
        setElement(containerElement);
      }
    };
    this.service = ServiceEntryPoint.getServiceConnection();
  }

  /**
   * Create editor instance no tied to a DOM element
   */
  protected SEditor() {
    this.editorPanel = new LogicalPanel.Impl() {
      {
        setElement(Document.get().createDivElement());
      }
    };
    this.service = ServiceEntryPoint.getServiceConnection();
  }

  /**
   * Attach the editor panel to an existing DOM element
   * iff the panel is not already attached.
   *
   * @param element the parent element
   */
  public void setParent(Element element) {

    if (editorPanel.getParent() != null) {
      editorPanel.getElement().removeFromParent();
    }

    if (element != null) {
      element.appendChild(editorPanel.getElement());
    }

  }

  /**
   * Attach a text object to this editor. The text will be
   * shown instantly.
   *
   * @param text a text object
   * @throws SException
   */
  public void set(STextWeb text) throws SException {

    if (checkBrowserCompat().equals(COMPAT_MODE_NONE))
      throw new SEditorException("Browser not supported");

    Editor e = getEditor();
    clean();

    ContentDocument doc = text.getContentDocument();
    // Ensure the document is rendered and listen for events
    // in a deattached DOM node
    text.setInteractive();

    // Add the document's root DOM node to the editor's panel
    editorPanel.getElement()
        .appendChild(doc.getFullContentView().getDocumentElement().getImplNodelet());

    // make editor aware of the document
    e.setContent(doc);

    // start live carets
    startCaretManager(text);

    AnnotationRegistry.muteHandlers(false);
  }

  private void startCaretManager(STextWeb text) {

    if (caretManager != null) {
      caretManager.stop();
    }

    caretManager = new CaretManager(SEditorStatics.getSSession(),
        text.getLiveCarets(), editor);
    caretManager.start();

  }

  private void stopCaretManager() {

    if (caretManager != null) {
      caretManager.stop();
    }
    caretManager = null;
  }

  /**
   * Enable or disable edit mode.
   * @param editOn
   */
  public void edit(boolean editOn) {
    if (editor != null && editor.hasDocument() && checkBrowserCompat().equals(COMPAT_MODE_EDIT)) {

      if (editOn)
        editor.addUpdateListener(this);
      else
        editor.removeUpdateListener(this);

      if (editor.isEditing() != editOn) {
        editor.setEditing(editOn);
      }
    }
  }

  /**
   * Reset the state of the editor, deattaching the document
   * if it is necessary.
   */
  public void clean() {
    if (editor != null && editor.hasDocument()) {

      stopCaretManager();

      editor.removeContentAndUnrender();
      editor.reset();
      editor.removeUpdateListener(this);
      AnnotationRegistry.muteHandlers(true);

    }
  }

  /**
   * @return true iff the editor is in edit mode
   */
  public boolean isEditing() {
    return editor != null && editor.isEditing();
  }

  /**
   * @return true iff a document is attached to this editor.
   */
  public boolean hasDocument() {
    return editor != null && editor.hasDocument();
  }

  public void focus() {
    if (editor != null && editor.hasDocument()) {
      editor.focus(false);
    }
  }

  public void blur() {
    if (editor != null && editor.hasDocument()) {
      editor.blur();
    }
  }

  public void setSelectionHandler(SelectionChangeHandler handler) {
    this.selectionHandler = handler;
  }


  //
  // Annotation methods
  //


  /**
   * Implements a safe check of range arguments.
   *
   * @param range
   * @return the original range or the current selection
   * @throws SEditorException
   *           if not valid range can be privided
   */
  protected Range rangeSafeCheck(Range range) throws SEditorException {

    if (Range.ALL.equals(range))
      range = SEditorHelper.getFullValidRange(editor);

    if (range == null)
      range = editor.getSelectionHelper().getOrderedSelectionRange();

    if (range == null)
      throw new SEditorException("A valid range must be provided or a selection must be active");

    return range;
  }

  /**
   * Set annotation in an specific doc range or in the current selection
   * otherwise.
   *
   * @param key
   *          annotation's name
   * @param value
   *          a valid value for the annotation
   * @param range
   *          a range or null
   */
  public AnnotationValue setAnnotation(String key, String value,
      @JsOptional SRange range)
      throws SEditorException {

    key = AnnotationRegistry.normalizeKey(key);

    if (!editor.isEditing())
      return null;

    final Range effectiveRange = rangeSafeCheck(range != null ? range.asRange() : null);
    final Editor editor = getEditor();

    return AnnotationController.set(editor, key, value, effectiveRange);

  }



  /**
   * Clear annotations in a given range or current selection.
   *
   *
   * @param names
   * @param range
   * @throws SEditorException
   */
  public void clearAnnotation(JavaScriptObject keys, @JsOptional SRange range)
      throws SEditorException {

    if (!editor.isEditing())
      return;

    Range effectiveRange = rangeSafeCheck(range != null ? range.asRange() : null);

    ReadableStringSet keySet = AnnotationRegistry.normalizeKeys(JsEditorUtils.toStringSet(keys));

    AnnotationController.clearAnnotation(editor, keySet, effectiveRange);



  }



  /**
   * Get annotations in the given range or in the selection otherwise.
   * <p>
   * Retrieve all if keys argument is empty.
   *
   * @param keys
   * @param range
   * @param onlyWithinRange
   * @return
   * @throws SEditorException
   */
  public JavaScriptObject getAnnotations(JavaScriptObject keys, @JsOptional SRange range)
      throws SEditorException {

    return AnnotationController.getAnnotationsWithFilters(editor, keys,
        rangeSafeCheck(range != null ? range.asRange() : null),
        null);

  }


  /**
   * Get all annotations with given key and value in the given range or in the
   * selection otherwise.
   * <p>
   * Retrieve all if keys argument is empty.
   *
   * @param keys
   * @param value
   * @param range
   * @param onlyWithinRange
   * @return
   * @throws SEditorException
   */
  public JavaScriptObject getAnnotationsWithValue(JavaScriptObject keys, final String value,
      @JsOptional SRange range)
      throws SEditorException {

    return AnnotationController.getAnnotationsWithFilters(editor, keys,
        rangeSafeCheck(range != null ? range.asRange() : null),
        (Object o) -> {

      if (o instanceof String) {
        return ((String) o).equals(value);
      } else {
        return o.toString().equals(value);
      }

    });
  }

  /**
   * Set a text annotation in the provided range creating or updating annotations in overlapped locations
   *
   * @param key
   * @param value
   * @param range
   *
   * @return
   * @throws SEditorException
   */
  public void setAnnotationOverlap(String key, String value, @JsOptional SRange range)
      throws SEditorException {


    final Range actualRange = rangeSafeCheck(range != null ? range.asRange() : null);
    final CMutableDocument doc = editor.getDocument();

    if (value == null)
      throw new SEditorException("Null value not allowed for overlapping annotations");

    editor.undoableSequence(new Runnable(){

      @Override
      public void run() {
        doc.beginMutationGroup();
        EditorAnnotationUtil.setAnnotationWithOverlap(doc, AnnotationRegistry.normalizeKey(key),
            value, actualRange.getStart(),
            actualRange.getEnd());
        doc.endMutationGroup();
      }

    });
  }

  /**
   * Clear a text annotation in the given range. Create or update annotations in
   * overlapped locations
   *
   * @param key
   * @param value
   * @param range
   * @throws SEditorException
   */
  public void clearAnnotationOverlap(String key, String value, @JsOptional SRange range)
      throws SEditorException {

    final Range actualRange = rangeSafeCheck(range != null ? range.asRange() : null);
    final CMutableDocument doc = editor.getDocument();

    final String nkey = AnnotationRegistry.normalizeKey(key);

    editor.undoableSequence(new Runnable(){

      @Override
      public void run() {

        EditorAnnotationUtil.getAnnotationSpread(editor.getDocument(), nkey, value, actualRange.getStart(), actualRange.getEnd())
        .forEach(new Consumer<RangedAnnotation<String>>(){

          @Override
          public void accept(RangedAnnotation<String> t) {

            // ignore annotations with null value, are just editor's internal stuff
            if (t.value() == null)
              return;

            String newValue = null;
            if (t.value().contains(",")) {
              newValue = t.value().replace(value, "");

              newValue = newValue.replace(",,", ",");

              if (newValue.charAt(0) == ',')
                newValue = newValue.substring(1, newValue.length());

              if (newValue.charAt(newValue.length()-1) == ',')
                newValue = newValue.substring(0, newValue.length()-1);

            }
            // This can remove or update an annotation
            doc.setAnnotation(t.start(), t.end(), t.key(), newValue);
          }
        });

      }

    });



  }


  public String getText(@JsOptional SRange range) {
    Range actualRange;
    try {
      actualRange = rangeSafeCheck(range != null ? range.asRange() : null);
    } catch (Exception e) {
      return null;
    }
    return DocHelper.getText(editor.getDocument(), actualRange.getStart(), actualRange.getEnd());
  }

  public SRange replaceText(SRange range, String text) {
    if (range == null)
      throw new IndexOutOfBoundsException("Text range is out of document size.");
    Range actualRange = range.asRange();
    editor.getDocument().deleteRange(actualRange.getStart(), actualRange.getEnd());
    editor.getDocument().insertText(actualRange.getStart(), text);
    return SRange
        .create(Range.create(actualRange.getStart(), actualRange.getStart() + text.length()));
  }

  public void insertText(int position, String text) {
    editor.getDocument().insertText(position, text);
  }

  public void deleteText(SRange range) {
    if (range == null)
      throw new IndexOutOfBoundsException("Text range is out of document size.");
    Range actualRange = range.asRange();
    editor.getDocument().deleteRange(actualRange.getStart(), actualRange.getEnd());
  }

  public SSelection getSelection() {
    try {
      Range r = rangeSafeCheck(null);
      return SSelection.get(r);
    } catch (Exception e) {
      return null;
    }
  }



  protected Editor getEditor() {

    if (editor == null) {
      editor =
        UserAgent.isMobileWebkit() ? new EditorImplWebkitMobile(false, editorPanel.getElement())
            : new EditorImpl(false, editorPanel.getElement());

      editor.init(null, SEditorStatics.getKeyBindingRegistry(), SEditorStatics.getSettings());

      this.service.addConnectionHandler(connectionHandler);
    }

    return editor;
  }

  @JsIgnore
  @Override
  public void onUpdate(EditorUpdateEvent event) {
    Editor editor = this.getEditor();

    if (selectionHandler != null) {
      Range range = editor.getSelectionHelper().getOrderedSelectionRange();
      if (range != null)
        selectionHandler.exec(SRange.create(range), this, SSelection.get(range));
    }
  }


  public String checkBrowserCompat() {
    if (UserAgent.isAndroid() || (UserAgent.isIPhone() && UserAgent.isChrome()))
      return COMPAT_MODE_READONLY;

    return COMPAT_MODE_EDIT;
  }


  public String __getContentView() {
    ContentNode node = editor.getContent().getFullContentView().getDocumentElement();
    return new Pretty<ContentNode>().print(FullContentView.INSTANCE, node);
  }

  public String __getHtmlView() {
    ContentNode node = editor.getContent().getFullContentView().getDocumentElement();
    return new Pretty<Node>().print(node.getContext().rendering().getFullHtmlView(),
        node.getImplNodelet());

  }

}
