package org.swellrt.beta.client.js.editor;


import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.ServiceFrontend.ConnectionHandler;
import org.swellrt.beta.client.js.JsUtils;
import org.swellrt.beta.client.js.editor.annotation.Annotation;
import org.swellrt.beta.client.js.editor.annotation.AnnotationAction;
import org.swellrt.beta.client.js.editor.annotation.AnnotationInstance;
import org.swellrt.beta.client.js.editor.annotation.AnnotationRegistry;
import org.swellrt.beta.client.js.editor.annotation.ParagraphAnnotation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.diff.DiffDeleteRenderer;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorImplWebkitMobile;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.common.logging.AbstractLogger;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.common.logging.LogSink;
import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.DOM;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "Editor")
public class SEditor implements EditorUpdateListener {

  @JsFunction
  public interface SelectionChangeHandler {
    
    void exec(Range range, SEditor editor, Node node);
    
  }
  
    
  //
  // public flag names
  //
    
  protected static int FLAG_LOG = 1;
  protected static int FLAG_DEBUG_DIALOG = 2;
  protected static int FLAG_UNDO = 3;
  protected static int FLAG_FANCY_CURSOR_BIAS = 4;
  protected static int FLAG_SEMANTIC_COPY_PASTE = 5;
  protected static int FLAG_WHITELIST_EDITOR = 6;
  protected static int FLAG_WEBKIT_COMPOSITION = 7;
  
  protected static final Map<Integer, Boolean> SETTINGS = new HashMap<Integer, Boolean>();
  
  static {
    SETTINGS.put(FLAG_LOG, true);
    SETTINGS.put(FLAG_DEBUG_DIALOG, true);
    SETTINGS.put(FLAG_UNDO, true);
    SETTINGS.put(FLAG_FANCY_CURSOR_BIAS, true);
    SETTINGS.put(FLAG_SEMANTIC_COPY_PASTE, false);
    SETTINGS.put(FLAG_WHITELIST_EDITOR, false);
    SETTINGS.put(FLAG_WEBKIT_COMPOSITION, true);
  }
  
  public static void setFlag(int flag, boolean value) {
    SETTINGS.put(flag, value);
  }
  
  public static boolean getFlag(int flag) {
    return SETTINGS.get(flag);
  }
  
  //
  // Static private properties
  //
  
  private static final String TOPLEVEL_CONTAINER_TAGNAME = "body";
  
  /*
   * A browser's console log
   */
  protected static class ConsoleLogSink extends LogSink {

    private native void console(String msg) /*-{
      console.log(msg);
    }-*/;

    @Override
    public void log(Level level, String message) {
      console("[" + level.name() + "] " + message);
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

    public CustomLogger(LogSink sink) {
      super(sink);
    }

    @Override
    public boolean isModuleEnabled() {
      return getFlag(FLAG_LOG);
    }

    @Override
    protected boolean shouldLog(Level level) {
      return getFlag(FLAG_LOG);
    }
  }

  // 
  // Put together here all static initialization.
  // Delegate all registry instances to those in Editor class,
  // but put here all initialization logic.
  //
  // Use always Editor.ROOT_REGISTRIES as reference for editor's registers
  
  static {
    
    EditorStaticDeps.logger = new CustomLogger(new ConsoleLogSink());
    
    Editors.initRootRegistries();

           
    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    EditorStaticDeps.setPopupChromeProvider(new PopupChromeProvider() {
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
    
  }
  
  
  public static SEditor createWithId(String containerId) throws SException {
    Element containerElement = DOM.getElementById(containerId);
    if (containerElement == null || !containerElement.getNodeName().equalsIgnoreCase("div"))
      throw new SException(SException.INTERNAL_ERROR, null, "Container element must be a div");
    
    SEditor se = new SEditor(containerElement);  
    return se;
  }
  
  public static SEditor createWithElement(Element containerElement) throws SException {
    if (containerElement == null || !containerElement.getNodeName().equalsIgnoreCase("div"))
      throw new SException(SException.INTERNAL_ERROR, null, "Container element must be a div");
    
    SEditor se = new SEditor(containerElement);  
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
  ServiceFrontend serviceFrontend;
  
  private boolean wasEditingOnDiscconnect= true;
  
  private SelectionChangeHandler selectionHandler = null;
  
  private ConnectionHandler connectionHandler = new ConnectionHandler() {

    @Override
    public void exec(String state, SException e) {
      if (!state.equals(ServiceFrontend.STATE_CONNECTED)) {
        
        if (editor.isEditing()) {
          wasEditingOnDiscconnect = true;      
          edit(false);
        }

      } else {
        
        if (wasEditingOnDiscconnect && !editor.isEditing())
          edit(true);
        
      }
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
    
    AnnotationRegistry.muteHandlers(false);
  }
  
  /**
   * Enable or disable edit mode.
   * @param editOn
   */
  public void edit(boolean editOn) {
    if (editor != null && editor.hasDocument()) {
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
      editor.removeContentAndUnrender();
      editor.reset();  
      editor.addUpdateListener(this);
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
   * Implements a safe logic to a range argument. 
   * 
   * @param range
   * @return the original range or the current selection
   * @throws SEditorException if not valid range can be privided
   */
  protected Range checkRangeArgument(Range range) throws SEditorException {
    
    if (Range.ALL.equals(range))
      range = SEditorHelper.getFullValidRange(editor);

    if (range == null)
      range = editor.getSelectionHelper().getOrderedSelectionRange();
    
    if (range == null)
      throw new SEditorException("A valid range must be provided or a selection must be active");
    
    return range;
  }
   
  /**
   * Set annotation in an specific doc range or in the current selection otherwise.
   * 
   * @param name annotation's name
   * @param value a valid value for the annotation
   * @param range a range or null
   */
  public AnnotationInstance setAnnotation(String name, String value, @JsOptional Range range) throws SEditorException {
    
    if (!editor.isEditing())
      return null;
    
    Annotation antn = AnnotationRegistry.get(name);
    if (antn == null)
      throw new SEditorException(SEditorException.UNKNOWN_ANNOTATION, "Unknown annotation");
    
    final Range effectiveRange = checkRangeArgument(range);
    final Editor editor = getEditor();
    
    
    if (antn instanceof ParagraphAnnotation) {
      
      editor.undoableSequence(new Runnable(){
        @Override
        public void run() {
          antn.set(editor.getDocument(), editor.getContent().getLocationMapper(), editor.getContent().getLocalAnnotations(), editor.getCaretAnnotations() , effectiveRange, value);           
        }        
      });
      return null;
      
    } else {
 
      antn.set(editor.getDocument(), editor.getContent().getLocationMapper(), editor.getContent().getLocalAnnotations(), editor.getCaretAnnotations() , effectiveRange, value);           
      return AnnotationInstance.create(editor.getContent(), name, value, effectiveRange, AnnotationInstance.MATCH_IN);

    }
    
  }
  
  
  /**
   * Reset annotations in an specific doc range or in the current selection otherwise.
   * 
   * @param names
   * @param range
   * @throws SEditorException
   */
  public void clearAnnotation(JavaScriptObject names, @JsOptional Range range) throws SEditorException {
    
    if (!editor.isEditing())
      return;
    
    Range effectiveRange = checkRangeArgument(range);
    
    AnnotationAction resetAction = new AnnotationAction(editor, effectiveRange);
    
    if (names != null) {
      if (JsUtils.isArray(names)) {
        resetAction.add((JsArrayString) names);
      } else if (JsUtils.isString(names)){
        String name = names.toString();
        resetAction.add(name);
      } else {
        throw new SEditorException("Expected array or string as first argument");
      }
    }
    
    editor.undoableSequence(new Runnable(){
      @Override
      public void run() {
        resetAction.reset();       
      }
    });
      
  }
  
  /**
   * Get annotations in an specific doc range or in the current selection otherwise.
   * 
   * @param names
   * @param range
   * @return
   * @throws SEditorException
   */
  public JavaScriptObject getAnnotation(JavaScriptObject names, @JsOptional Range range, @JsOptional Boolean all) throws SEditorException {
        
    range = checkRangeArgument(range);
    
    AnnotationAction getAction = new AnnotationAction(editor, range);
    
    getAction.deepTraverse(all != null ? all : false);
    // By default only consider effective annotations 
    getAction.onlyEffectiveAnnotations( !(all != null && all.equals(Boolean.TRUE)) ); 
    
    if (names != null) {
      if (JsUtils.isArray(names)) {
        getAction.add((JsArrayString) names);
      } else if (JsUtils.isString(names) && !names.toString().isEmpty()){
        getAction.add(names.toString());
      } else {
        throw new SEditorException("Expected array or string as first argument");
      }
    }
    
     
    
    return getAction.get();   
  }
  
  
  public String getText(@JsOptional Range range) {
    try {
      range = checkRangeArgument(range);
    } catch (Exception e) {
      return null;
    }
    return DocHelper.getText(editor.getDocument(), range.getStart(), range.getEnd());
  }
  
  public Range getSelection() {
    try { 
      return checkRangeArgument(null);
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
  @JsIgnore
  public void registerService(ServiceFrontend serviceFrontend) {
    this.serviceFrontend = serviceFrontend;
    this.serviceFrontend.addConnectionHandler(connectionHandler);
  }
  
  @JsIgnore
  public void unregisterService() {
    if (serviceFrontend != null)
      serviceFrontend.removeConnectionHandler(connectionHandler);
  }
  
  protected EditorSettings getSettings() {
    
    return new EditorSettings()
    .setHasDebugDialog(SETTINGS.get(FLAG_DEBUG_DIALOG))
    .setUndoEnabled(SETTINGS.get(FLAG_UNDO))
    .setUseFancyCursorBias(SETTINGS.get(FLAG_FANCY_CURSOR_BIAS))
    .setUseSemanticCopyPaste(SETTINGS.get(FLAG_SEMANTIC_COPY_PASTE))
    .setUseWhitelistInEditor(SETTINGS.get(FLAG_WHITELIST_EDITOR))
    .setUseWebkitCompositionEvents(SETTINGS.get(FLAG_WEBKIT_COMPOSITION));
    
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
        editor.addUpdateListener(this);
    }
    
    
    return editor;
  }

  @JsIgnore
  @Override
  public void onUpdate(EditorUpdateEvent event) {
    Editor editor = this.getEditor();
    if (selectionHandler != null && event.selectionLocationChanged()) {
      Range range = editor.getSelectionHelper().getOrderedSelectionRange();

      if (range != null) {

        Point<ContentNode> pStart = editor.getDocument().locate(range.getStart()+1);
        Node nStart = pStart.getCanonicalNode().getImplNodelet();

        selectionHandler.exec(range, this, nStart);
      }

    }
  }
 
}
