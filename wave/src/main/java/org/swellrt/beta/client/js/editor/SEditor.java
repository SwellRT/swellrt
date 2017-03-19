package org.swellrt.beta.client.js.editor;


import java.util.Map;

import org.swellrt.beta.client.ServiceBasis;
import org.swellrt.beta.client.ServiceBasis.ConnectionHandler;
import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.js.Console;
import org.swellrt.beta.client.js.JsUtils;
import org.swellrt.beta.client.js.editor.annotation.Annotation;
import org.swellrt.beta.client.js.editor.annotation.AnnotationAction;
import org.swellrt.beta.client.js.editor.annotation.AnnotationInstance;
import org.swellrt.beta.client.js.editor.annotation.AnnotationRegistry;
import org.swellrt.beta.client.js.editor.annotation.ParagraphAnnotation;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.common.util.UserAgentStaticProperties;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.doodad.selection.CaretAnnotationHandler;
import org.waveprotocol.wave.client.doodad.selection.SelectionExtractor;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorImpl;
import org.waveprotocol.wave.client.editor.EditorImplWebkitMobile;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.selection.html.NativeSelectionUtil;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.common.logging.AbstractLogger;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.common.logging.LogSink;
import org.waveprotocol.wave.model.conversation.Blips;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.FocusedPointRange;
import org.waveprotocol.wave.model.document.util.LineContainers;
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
    /*
    DiffAnnotationHandler.register(
        Editor.ROOT_REGISTRIES.getAnnotationHandlerRegistry(),
        Editor.ROOT_REGISTRIES.getPaintRegistry());
    
    DiffDeleteRenderer.register(
        Editor.ROOT_REGISTRIES.getElementHandlerRegistry());
        */
    
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
      
      EditorStaticDeps.logger.trace().log("Static user agent: "+UserAgentStaticProperties.get().getClass().getName());
      EditorStaticDeps.logger.trace().log("User Agent Properties: "+s);
    }
    
    
  }
  
  
  public static SEditor createWithId(String containerId, @JsOptional ServiceFrontend sf) throws SException {
    Element containerElement = DOM.getElementById(containerId);
    if (containerElement == null || !containerElement.getNodeName().equalsIgnoreCase("div"))
      throw new SException(SException.INTERNAL_ERROR, null, "Container element must be a div");
    
    SEditor se = new SEditor(containerElement);
    if (sf != null) se.registerService(sf);
    return se;
  }
  
  public static SEditor createWithElement(Element containerElement, @JsOptional ServiceFrontend sf) throws SException {
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
  ServiceBasis service;
   
  private SelectionExtractor selectionExtractor;
  
  private ProfileManager profileManager;
  
  private boolean wasEditingOnDiscconnect = true;
  
  private SelectionChangeHandler selectionHandler = null;
  
  private ConnectionHandler connectionHandler = new ConnectionHandler() {

    @Override
    public void exec(String state, SException e) {
      
      if (editor == null)
        return;
      
      if (!state.equals(ServiceFrontend.STATUS_CONNECTED)) {
        
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
    
    // start live carets
    if (selectionExtractor != null)
      selectionExtractor.start(e);
       
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
      
      if (selectionExtractor != null) {       
        selectionExtractor.stop(editor);
        // ensures selection extractor is create for each new doc.
        selectionExtractor = null; 
      }
      
      editor.removeContentAndUnrender();
      editor.reset();  
      editor.addUpdateListener(this);
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
  public void registerService(ServiceBasis service) {
    
    if (this.service != null)
      unregisterService();
    
    this.service = service;
    this.service.addConnectionHandler(connectionHandler);
    this.profileManager = service.getProfilesManager();
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
        editor.addUpdateListener(this);        
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
      selectionHandler.exec(range, this, SSelection.get());
    }
  }
 
}
