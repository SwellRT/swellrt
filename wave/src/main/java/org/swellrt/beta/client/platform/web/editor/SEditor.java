package org.swellrt.beta.client.platform.web.editor;


import java.util.Map;
import java.util.function.Consumer;

import org.swellrt.beta.client.DefaultFrontend;
import org.swellrt.beta.client.ServiceConnection;
import org.swellrt.beta.client.ServiceConnection.ConnectionHandler;
import org.swellrt.beta.client.ServiceConstants;
import org.swellrt.beta.client.platform.web.browser.Console;
import org.swellrt.beta.client.platform.web.editor.annotation.AnnotationController;
import org.swellrt.beta.client.platform.web.editor.annotation.AnnotationRegistry;
import org.swellrt.beta.client.platform.web.editor.annotation.AnnotationValue;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.diff.DiffDeleteRenderer;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.doodad.selection.SelectionExtractor;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorImplWebkitMobile;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.FullContentView;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.common.logging.AbstractLogger;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.common.logging.LogSink;
import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Pretty;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.ReadableStringSet;

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

  public final static JavaScriptObject RANGE_ALL = JsEditorUtils.rangeToNative(Range.ALL);
  public final static JavaScriptObject RANGE_EMPTY = JsEditorUtils.rangeToNative(Range.NONE);

  public final static String COMPAT_MODE_NONE = "none";
  public final static String COMPAT_MODE_READONLY = "readonly";
  public final static String COMPAT_MODE_EDIT = "edit";

  @JsFunction
  public interface SelectionChangeHandler {
    void exec(Range range, SEditor editor, SSelection node);
  }

  //
  // Static private properties
  //

  private static final String TOPLEVEL_CONTAINER_TAGNAME = "body";

  /*
   * A browser's console log
   */
  protected static class ConsoleLogSink extends LogSink {

    @Override
    public void log(Level level, String message) {
      Console.log("[" + level.name() + "] " + message);
    }

    @Override
    public void lazyLog(Level level, Object... messages) {
      for (Object o : messages) {
        log(level, o.toString());
      }
    }
  }

  /**
   *
   */
  protected static class CustomLogger extends AbstractLogger {


    private boolean enabled = SEditorConfig.enableLog();

    public CustomLogger(LogSink sink) {
      super(sink);
    }

    @Override
    public boolean isModuleEnabled() {
      return enabled;
    }

    @Override
    protected boolean shouldLog(Level level) {
      return enabled;
    }
  }

  //
  // Put together here all static initialization.
  // Delegate all registry instances to those in Editor class,
  // but put here all initialization logic.
  //
  // Use always Editor.ROOT_REGISTRIES as reference for editor's registers

  protected static CaretAnnotationHandler caretAnnotationHandler;

  static {

    if (SEditorConfig.enableLog())
      EditorStaticDeps.logger = new CustomLogger(new ConsoleLogSink());

    Editors.initRootRegistries();


    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    EditorStaticDeps.setPopupChromeProvider(new PopupChromeProvider() {
      @Override
      public PopupChrome createPopupChrome() {
        return null;
      }
    });

    //
    // Register Doodads: all are statically handled
    //


    // Code taken from RegistriesHolder
    Blips.init();
    LineRendering.registerContainer(TOPLEVEL_CONTAINER_TAGNAME, Editor.ROOT_REGISTRIES.getElementHandlerRegistry());
    LineContainers.setTopLevelContainerTagname(TOPLEVEL_CONTAINER_TAGNAME);


    StyleAnnotationHandler.register(Editor.ROOT_REGISTRIES);

    // Listen for Diff annotations to paint new content or to insert a
    // delete-content tag
    // to be rendered by the DiffDeleteRendere

    DiffAnnotationHandler.register(
        Editor.ROOT_REGISTRIES.getAnnotationHandlerRegistry(),
        Editor.ROOT_REGISTRIES.getPaintRegistry());

    DiffDeleteRenderer.register(
        Editor.ROOT_REGISTRIES.getElementHandlerRegistry());

    caretAnnotationHandler = CaretAnnotationHandler.register(Editor.ROOT_REGISTRIES);

    //
    // Reuse existing link annotation handler, but also support external
    // controller to get notified on mutation or input events
    //
    LinkAnnotationHandler.register(Editor.ROOT_REGISTRIES, new LinkAttributeAugmenter() {
      @Override
      public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
          Map<String, String> current) {
        return current;
      }
    });

    // TODO register widgets. Widgets definitions are (so far) statically registered
    // so they shouldn't be associated with any particular instance of SEditor

    /*
    widgetRegistry.each(new ProcV<JsoWidgetController>() {

      @Override
      public void apply(String key, JsoWidgetController value) {
        value.setEditorJsFacade(editorJsFacade);
      }

    });

    WidgetDoodad.register(Editor.ROOT_REGISTRIES.getElementHandlerRegistry(), widgetRegistry);
    */

    // Debugging user agent

    if (SEditorConfig.enableLog()) {

      EditorStaticDeps.logger.trace().log("User Agent String: "+UserAgent.debugUserAgentString());

      String s = "";

      s += "Android: "+UserAgent.isAndroid()+", ";
      s += "IPhone: "+UserAgent.isIPhone()+", ";


      s += "Linux: "+UserAgent.isLinux()+", ";
      s += "Mac: "+UserAgent.isMac()+", ";
      s += "Win: "+UserAgent.isWin()+", ";

      s += "Mobile Webkit: "+UserAgent.isMobileWebkit()+", ";
      s += "Webkit: "+UserAgent.isWebkit()+", ";

      s += "Safari: "+UserAgent.isSafari()+", ";
      s += "Chrome: "+UserAgent.isChrome()+", ";
      s += "Firefox: "+UserAgent.isFirefox()+", ";

      s += "IE: "+UserAgent.isIE()+", ";
      s += "IE7: "+UserAgent.isIE7()+", ";
      s += "IE8: "+UserAgent.isIE8()+", ";

      EditorStaticDeps.logger.trace().log("User Agent Properties: "+s);
    }


  }


  public static SEditor createWithId(String containerId, @JsOptional DefaultFrontend sf) throws SException {
    Element containerElement = DOM.getElementById(containerId);
    if (containerElement == null || !containerElement.getNodeName().equalsIgnoreCase("div"))
      throw new SException(SException.INTERNAL_ERROR, null, "Container element must be a div");

    SEditor se = new SEditor(containerElement);
    if (sf != null) se.registerService(sf);
    return se;
  }

  public static SEditor createWithElement(Element containerElement, @JsOptional DefaultFrontend sf) throws SException {
    if (containerElement == null || !containerElement.getNodeName().equalsIgnoreCase("div"))
      throw new SException(SException.INTERNAL_ERROR, null, "Container element must be a div");

    SEditor se = new SEditor(containerElement);
    if (sf != null) se.registerService(sf);
    return se;
  }

  public static SEditor create() {
    SEditor se = new SEditor();
    return se;
  }


  private LogicalPanel.Impl editorPanel;

  /** Don't use this prop directly, use getter instead */
  private EditorImpl editor;
  /** Don't use this prop directly, use getter instead */
  private KeyBindingRegistry keyBindingRegistry;

  /** A service to listen to connection events */
  ServiceConnection service;

  @Deprecated
  private SelectionExtractor selectionExtractor;

  private CaretManager caretManager;

  private ProfileManager profileManager;

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

    try {

      if (caretManager != null) {
        caretManager.stop();
      }

      caretManager = new CaretManager(profileManager.getCurrentParticipantId(),
          profileManager.getCurrentSessionId(), text.getLiveCarets(), editor);
      caretManager.start();

    } catch (SException e) {
      new IllegalStateException(e);
    }

  }

  private void stopCaretManager() {

    if (caretManager != null) {
      try {
        caretManager.stop();
      } catch (SException e) {
        new IllegalStateException(e);
      }
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

      caretAnnotationHandler.clear();
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
      @JsOptional JavaScriptObject _range)
      throws SEditorException {

    key = AnnotationRegistry.normalizeKey(key);

    if (!editor.isEditing())
      return null;

    final Range effectiveRange = rangeSafeCheck(JsEditorUtils.nativeToRange(_range));
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
  public void clearAnnotation(JavaScriptObject keys, @JsOptional JavaScriptObject _range)
      throws SEditorException {

    if (!editor.isEditing())
      return;

    Range effectiveRange = rangeSafeCheck(JsEditorUtils.nativeToRange(_range));

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
  public JavaScriptObject getAnnotations(JavaScriptObject keys, @JsOptional JavaScriptObject _range)
      throws SEditorException {

    return AnnotationController.getAnnotationsWithFilters(editor, keys,
        rangeSafeCheck(JsEditorUtils.nativeToRange(_range)),
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
      @JsOptional JavaScriptObject _range)
      throws SEditorException {

    return AnnotationController.getAnnotationsWithFilters(editor, keys,
        rangeSafeCheck(JsEditorUtils.nativeToRange(_range)),
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
  public void setAnnotationOverlap(String key, String value, @JsOptional JavaScriptObject _range)
      throws SEditorException {


    final Range actualRange = rangeSafeCheck(JsEditorUtils.nativeToRange(_range));
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
  public void clearAnnotationOverlap(String key, String value, @JsOptional JavaScriptObject _range)
      throws SEditorException {

    final Range actualRange = rangeSafeCheck(JsEditorUtils.nativeToRange(_range));
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


  public String getText(@JsOptional JavaScriptObject _range) {
    Range range;
    try {
      range = rangeSafeCheck(JsEditorUtils.nativeToRange(_range));
    } catch (Exception e) {
      return null;
    }
    return DocHelper.getText(editor.getDocument(), range.getStart(), range.getEnd());
  }

  public Range replaceText(JavaScriptObject _range, String text) {
    Range range = JsEditorUtils.nativeToRange(_range);
    editor.getDocument().deleteRange(range.getStart(), range.getEnd());
    editor.getDocument().insertText(range.getStart(), text);
    return Range.create(range.getStart(), range.getStart() + text.length());
  }

  public void insertText(int position, String text) {
    editor.getDocument().insertText(position, text);
  }

  public void deleteText(JavaScriptObject _range) {
    Range range = JsEditorUtils.nativeToRange(_range);
    editor.getDocument().deleteRange(range.getStart(), range.getEnd());
  }

  public SSelection getSelection() {
    try {
      Range r = rangeSafeCheck(null);
      return SSelection.get(r);
    } catch (Exception e) {
      return null;
    }
  }
  //
  // Internal stuff
  //

  /**
   * Make editor instance to listen to service's connection
   * events.
   * <p>
   * We prefer to register the service on the editor instead
   * the contrary way, because service must be agnostic
   * from any platform dependent component.
   *
   * @param serviceFrontend
   */
  public void registerService(ServiceConnection service) {

    if (this.service != null)
      unregisterService();

    this.service = service;
    this.service.addConnectionHandler(connectionHandler);
    this.profileManager = service.getProfilesManager();
    EditorStaticDeps.setProfileManager(profileManager);
    if (this.profileManager != null)
      caretAnnotationHandler.setProfileManager(profileManager);
  }

  public void unregisterService() {
    if (service != null)
      service.removeConnectionHandler(connectionHandler);

    caretAnnotationHandler.setProfileManager(null);

    this.profileManager = null;
  }

  protected EditorSettings getSettings() {

    return new EditorSettings()
    .setHasDebugDialog(SEditorConfig.debugDialog())
    .setUndoEnabled(SEditorConfig.undo())
    .setUseFancyCursorBias(SEditorConfig.fancyCursorBias())
    .setUseSemanticCopyPaste(SEditorConfig.semanticCopyPaste())
    .setUseWhitelistInEditor(SEditorConfig.whitelistEditor())
    .setUseWebkitCompositionEvents(SEditorConfig.webkitComposition());

  }

  protected KeyBindingRegistry getKeyBindingRegistry() {

    if (keyBindingRegistry == null)
      keyBindingRegistry = new KeyBindingRegistry();

    return keyBindingRegistry;
  }

  protected Editor getEditor() {

    if (editor == null) {
      editor =
        UserAgent.isMobileWebkit() ? new EditorImplWebkitMobile(false, editorPanel.getElement())
            : new EditorImpl(false, editorPanel.getElement());

        editor.init(null, getKeyBindingRegistry(), getSettings());
    }

    if (selectionExtractor == null && profileManager != null) {
      selectionExtractor = new SelectionExtractor(SchedulerInstance.getLowPriorityTimer(), profileManager);
    }



    return editor;
  }

  Element caretMarker = null;

  @JsIgnore
  @Override
  public void onUpdate(EditorUpdateEvent event) {
    Editor editor = this.getEditor();

    if (selectionHandler != null) {
      Range range = editor.getSelectionHelper().getOrderedSelectionRange();
      if (range != null)
        selectionHandler.exec(range, this, SSelection.get(range));
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
