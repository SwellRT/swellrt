package org.swellrt.client.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.swellrt.api.BrowserSession;
import org.swellrt.client.editor.TextEditorDefinitions.ParagraphAnnotation;
import org.swellrt.model.generic.TextType;
import org.swellrt.model.shared.ModelUtils;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.impl.ProfileManagerImpl;
import org.waveprotocol.wave.client.common.util.JsoStringMap;
import org.waveprotocol.wave.client.common.util.JsoStringSet;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.doodad.annotation.AnnotationHandler;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoAnnotation;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoAnnotationController;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoRange;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoParagraphAnnotation;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.diff.DiffDeleteRenderer;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.doodad.selection.SelectionAnnotationHandler;
import org.waveprotocol.wave.client.doodad.selection.SelectionAnnotationHandler.CaretListener;
import org.waveprotocol.wave.client.doodad.selection.SelectionExtractor;
import org.waveprotocol.wave.client.doodad.widget.WidgetDoodad;
import org.waveprotocol.wave.client.doodad.widget.jso.JsoWidget;
import org.waveprotocol.wave.client.doodad.widget.jso.JsoWidgetController;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorSettings;
import org.waveprotocol.wave.client.editor.EditorStaticDeps;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.Editors;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint.EventHandler;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint.MutationHandler;
import org.waveprotocol.wave.client.editor.content.misc.StyleAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.LineStyle;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphBehaviour;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wave.RegistriesHolder;
import org.waveprotocol.wave.client.wave.WaveDocuments;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeProvider;
import org.waveprotocol.wave.client.widget.popup.simple.Popup;
import org.waveprotocol.wave.common.logging.AbstractLogger;
import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.common.logging.LogSink;
import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

/**
 * A wrapper of the original Wave {@link Editor} to be integrated in the SwellRT
 * Web API. Users can edit TextType instances in this editor.
 *
 * @author pablojan
 *
 */
public class TextEditor implements EditorUpdateListener {

  public interface Configurator {
    
    /**
     * The gateway to get UI-versions of Blips. Registry is GWT related, so must
     * be injected in the Editor by the JS API. Model's classes have to ignore it.
     */
    public WaveDocuments<? extends InteractiveDocument> getDocumentRegistry();
    
    /**
     * @return a profile manager instance
     */
    public ProfileManager getProfileManager();
    
    /**
     * @return a full session id including tab/window info. 
     */
    public String getSessionId();
    
    /**
     * 
     * @return current logged in user id
     */
    public ParticipantId getParticipantId();
  }

  public static class JSLogSink extends LogSink {

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

  
  public static class CustomLogger extends AbstractLogger {

    public CustomLogger(LogSink sink) {
      super(sink);
    }

    @Override
    public boolean isModuleEnabled() {
      return enableEditorLog();
    }

    @Override
    protected boolean shouldLog(Level level) {
      return enableEditorLog();
    }
  }
  
  private static native boolean enableEditorLog() /*-{
  	return $wnd.__debugEditor && ($wnd.__debugEditor == true); 	
  }-*/;

  private static final String TOPLEVEL_CONTAINER_TAGNAME = "body";

  static {
    Editors.initRootRegistries();
    LineContainers.setTopLevelContainerTagname(TOPLEVEL_CONTAINER_TAGNAME);
    if (enableEditorLog())
    	EditorStaticDeps.logger = new CustomLogger(new JSLogSink());
  }


  private static final EditorSettings EDITOR_SETTINGS = new EditorSettings()
      .setHasDebugDialog(true).setUndoEnabled(true).setUseFancyCursorBias(true)
      .setUseSemanticCopyPaste(false).setUseWhitelistInEditor(false)
      .setUseWebkitCompositionEvents(true);

  // Wave Editor specific

  private final Registries registries = RegistriesHolder.get();
  private final KeyBindingRegistry KEY_REGISTRY = new KeyBindingRegistry();


  private final LogicalPanel.Impl editorPanel;
  private LogicalPanel.Impl docPanel;
  private ContentDocument doc;

  private SelectionExtractor selectionExtractor;
      
  /** The actual implementation of the editor */
  private Editor editor;

  /** Listener for editor events */
  private TextEditorListener listener;
  
  private WaveId containerWaveId;

  private boolean shouldFireEvents = false;
  
  private TimerService clock;
  
  /**
   * Registry of JavaScript controllers for widgets
   */
  private final StringMap<JsoWidgetController> widgetRegistry;
 
  /**
   * Registry of JavaScript controllers for annotations
   */
  private final StringMap<JsoAnnotationController> annotationRegistry;

  {
    EditorStaticDeps.setPopupProvider(Popup.LIGHTWEIGHT_POPUP_PROVIDER);
    EditorStaticDeps.setPopupChromeProvider(new PopupChromeProvider() {
      public PopupChrome createPopupChrome() {
        return null;
      }
    });


  }

  public static TextEditor create(Element parent, StringMap<JsoWidgetController> widgetControllers, StringMap<JsoAnnotationController> annotationControllers) {
    Preconditions.checkNotNull(parent, "Element to hook editor canvas doesn't exist");    
    TextEditor editor = new TextEditor(parent, widgetControllers, annotationControllers);
    return editor;
  }
  

  protected TextEditor(final Element containerElement, StringMap<JsoWidgetController> widgetControllers, StringMap<JsoAnnotationController> annotationControllers) {
    this.editorPanel = new LogicalPanel.Impl() {
      {
        setElement(containerElement);
      }
    };
    this.widgetRegistry = widgetControllers;
    this.annotationRegistry = annotationControllers;
    this.clock = SchedulerInstance.getLowPriorityTimer();
  }

  /**
   * This is a nasty method to pass a reference of editor's js facade 
   * to widget and annotation controllers.
   * 
   * So there is a circular reference between TextEditor and TextEditorJS.
   * 
   * @param editorJsFacade editor's pure JavaScript facade
   */
  public void initialize(JavaScriptObject editorJsFacade) {
      registerDoodads(editorJsFacade);
      registerAnnotations(editorJsFacade);
  }

  
  protected void registerDoodads(final JavaScriptObject editorJsFacade) {


    // TOPLEVEL_CONTAINER_TAGNAME
    LineRendering.registerContainer(TOPLEVEL_CONTAINER_TAGNAME,
        registries.getElementHandlerRegistry());

    StyleAnnotationHandler.register(registries);

    // Listen for Diff annotations to paint new content or to insert a
    // delete-content tag
    // to be rendered by the DiffDeleteRendere
    DiffAnnotationHandler.register(registries.getAnnotationHandlerRegistry(),
        registries.getPaintRegistry());
    DiffDeleteRenderer.register(registries.getElementHandlerRegistry());
        
    //
    // Reuse existing link annotation handler, but also support external
    // controller to get notified on mutation or input events
    //
    LinkAnnotationHandler.register(registries, new LinkAttributeAugmenter() {
      @Override
      public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
          Map<String, String> current) {
        return current;
      }
    });
    
    // Set reference to js editor's facade to controllers.
    widgetRegistry.each(new ProcV<JsoWidgetController>() {

      @Override
      public void apply(String key, JsoWidgetController value) {
        value.setEditorJsFacade(editorJsFacade);
      }
      
    });

    WidgetDoodad.register(registries.getElementHandlerRegistry(), widgetRegistry);

    
  }

 
  protected void registerAnnotations(final JavaScriptObject editorJsFacade) {

    // Configure annotations.
    // For shake of encapsulation, we don't expose native JS type
    // JsoAnntationController to inner editor classes.
   
    annotationRegistry.each(new ProcV<JsoAnnotationController>() {

      @Override
      public void apply(String key, JsoAnnotationController controller) {

        // sets the circular reference to the editor's js facade
        controller.setEditorJsFacade(editorJsFacade);
        
        if (key.contains(AnnotationConstants.LINK_PREFIX)) {
          
          // Link annotations are actually registered in registerDoodads() method
          // here only handlers are registered 

          AnnotationPaint.registerEventHandler(AnnotationConstants.LINK_PREFIX, new EventHandler() {

            @Override
            public void onEvent(ContentElement node, Event event) {
              if (controller != null && shouldFireEvents)
                controller.onEvent(JsoRange.Builder.create(node.getMutableDoc()).range(node)
                    .annotation(AnnotationConstants.LINK_PREFIX, null).build(), event);
            }
          });

          AnnotationPaint.setMutationHandler(AnnotationConstants.LINK_PREFIX,
              new MutationHandler() {

                @Override
                public void onMutation(ContentElement node) {
                  if (controller != null  && shouldFireEvents)
                    controller.onChange(JsoRange.Builder.create(node.getMutableDoc())
                        .range(node).annotation(AnnotationConstants.LINK_PREFIX, null).build());
                }
                
                @Override
                public void onAdded(ContentElement node) {
                  if (controller != null  && shouldFireEvents)
                    controller.onAdd(JsoRange.Builder.create(node.getMutableDoc())
                        .range(node).annotation(AnnotationConstants.LINK_PREFIX, null).build());        
                }

                @Override
                public void onRemoved(ContentElement node) {
                  if (controller != null  && shouldFireEvents)
                    controller.onRemove(JsoRange.Builder.create(node.getMutableDoc())
                        .range(node).annotation(AnnotationConstants.LINK_PREFIX, null).build());        
                }
                
              });

        } else if (TextEditorDefinitions.isParagraphAnnotation(key)) {

          if (key.equals(ParagraphAnnotation.HEADER.toString())) {

            Paragraph.registerEventHandler(ParagraphBehaviour.HEADING,
                new Paragraph.EventHandler() {

                  @Override
                  public void onEvent(ContentElement node, Event event) {
                    if (controller != null  && shouldFireEvents)
                      controller.onEvent(
                          JsoRange.Builder.create(node.getMutableDoc()).range(node)
                              .annotation(key, node.getAttribute(Paragraph.SUBTYPE_ATTR)).build(),
                          event);
                  }
                });

            Paragraph.registerMutationHandler(ParagraphBehaviour.HEADING,
                new Paragraph.MutationHandler() {

                  @Override
                  public void onMutation(ContentElement node) {
                    if (controller != null && shouldFireEvents)
                      controller
                          .onChange(JsoRange.Builder.create(node.getMutableDoc()).range(node)
                              .annotation(key, node.getAttribute(Paragraph.SUBTYPE_ATTR)).build());
                  }

                  @Override
                  public void onAdded(ContentElement node) {
                    if (controller != null && shouldFireEvents)
                      controller
                          .onAdd(JsoRange.Builder.create(node.getMutableDoc()).range(node)
                              .annotation(key, node.getAttribute(Paragraph.SUBTYPE_ATTR)).build());                   
                  }

                  @Override
                  public void onRemoved(ContentElement node) {
                    if (controller != null && shouldFireEvents)
                      controller
                          .onRemove(JsoRange.Builder.create(node.getMutableDoc()).range(node)
                              .annotation(key, node.getAttribute(Paragraph.SUBTYPE_ATTR)).build());     
                    
                  }
                  
                  
                  
                  
                });

          }

        } else if (TextEditorDefinitions.isStyleAnnotation(key)) {

          // Nothing to do with style annotations

        } else {

          // Register custom annotations
          AnnotationHandler.register(registries, key, controller, new AnnotationHandler.Activator() {            
            @Override
            public boolean shouldFireEvent() {
              return shouldFireEvents;
            }
          });

        }

      }

    });

  }
  
  
  public void setListener(TextEditorListener listener) {
    this.listener = listener;
  }
  
  public WaveId getWaveId() {
    return containerWaveId;
  }
  

  /**
   * Start an editing session.
   * 
   * @param text the TextType instance to edit
   * @param a handler to listen on caret events
   * @param configurator editor dependencies not related with text
   * 
   */
  public void edit(TextType text, CaretListener caretListener, Configurator configurator) {
    Preconditions.checkNotNull(text, "Text object can't be null");
    Preconditions.checkNotNull(configurator, "Editor configurator can't be null");
    
    shouldFireEvents = false;

    if (!isClean()) cleanUp();

    // Place here selection extractor to ensure session id and user id are refreshed
    SelectionAnnotationHandler.register(registries, configurator.getSessionId(), configurator.getProfileManager(), caretListener);
    
    selectionExtractor = new SelectionExtractor(clock, configurator.getParticipantId().getAddress(), configurator.getSessionId());
    
    doc = getContentDocument(text, configurator.getDocumentRegistry());
    Preconditions.checkArgument(doc != null, "Can't edit an unattached TextType");

    doc.setRegistries(registries);

    this.docPanel = new LogicalPanel.Impl() {
      {
        setElement(Document.get().createDivElement());
      }
    };


    doc.setInteractive(docPanel);

    // Append the doc panel to the provided container panel
    editorPanel.getElement().appendChild(
        doc.getFullContentView().getDocumentElement().getImplNodelet());

    if (editor == null) {
      editor = Editors.attachTo(doc);
      editor.init(null, KEY_REGISTRY, EditorSettings.DEFAULT);
    } else {
      // Reuse existing editor.
      if (editor.hasDocument()) {
        editor.removeContentAndUnrender();
        editor.reset();
      }
      editor.setContent(doc);
      editor.init(null, KEY_REGISTRY, EDITOR_SETTINGS);
    }

    editor.addUpdateListener(this);
    // editor.addKeySignalListener(parentPanel);

    editor.setEditing(true);
    editor.focus(true);
    
    selectionExtractor.start(editor);
    
    containerWaveId = text.getModel().getWaveId();
    
    shouldFireEvents = true;
  }



  private ContentDocument getContentDocument(TextType text, WaveDocuments<? extends InteractiveDocument> documentRegistry) {
    Preconditions.checkArgument(text != null,
        "Unable to get ContentDocument from null TextType");
    Preconditions.checkArgument(documentRegistry != null,
        "Unable to get ContentDocument from null DocumentRegistry");

    return documentRegistry.getBlipDocument(ModelUtils.serialize(text.getModel().getWaveletId()),
        text.getDocumentId()).getDocument();
  }


  public void setEditing(boolean isEditing) {
    if (editor != null && editor.hasDocument()) {
      if (editor.isEditing() != isEditing) editor.setEditing(isEditing);
    }
  }

  public void cleanUp() {
    if (editor != null && doc != null) {
      if (selectionExtractor != null) {
        selectionExtractor.stop(editor);
        selectionExtractor = null;
      }
      editor.removeUpdateListener(this);
      editor.removeContentAndUnrender();
      editor.reset();
      doc = null;
      containerWaveId = null;
    }
  }

  public boolean isClean() {
    return doc == null;
  }

  public void toggleDebug() {
    editor.debugToggleDebugDialog();
  }

  /**
   * Insert or append a Widget at the current cursor position or at the end iff the type
   * is registered.
   * 
   * @param type
   * @param state
   * 
   * @return The widget as DOM element
   */
  public JsoWidget addWidget(String type, String state) {
	Point<ContentNode> currentPoint = null;
    if (editor.getSelectionHelper().getOrderedSelectionPoints() != null)
    	currentPoint = editor.getSelectionHelper().getOrderedSelectionPoints().getFirst();

    ContentElement w = WidgetDoodad.addWidget(editor.getDocument(), currentPoint, type, state);
    return JsoWidget.create(w.getImplNodelet(), w);
  }

  /**
   * Get the widget associated with the DOM element
   * 
   * @param domElement the widget element or a descendant
   */
  public JsoWidget getWidget(Element domElement) {
   return WidgetDoodad.getWidget(editor.getDocument(), domElement);  
  }
  


  protected boolean isValidAnnotationKey(String key) {
		return (TextEditorDefinitions.isParagraphAnnotation(key) || 
				TextEditorDefinitions.isStyleAnnotation(key) ||
				annotationRegistry.containsKey(key) ||
				key.equals("link"));
  }
  

  
  /**
   * Calculate the value of all annotations in the provided range.
   * If annotation is not set in the range, the result it will be a null value.
   *
   * @return a native JS object having a property for each annotation.
   */
  protected JsoStringMap<String> getAllAnnotationsInRange(final Range range) {
	  
	  
	  // Map to contain the current state of each annotation
      final JsoStringMap<String> annotationMap = JsoStringMap.<String>create();  
	  
      if (range != null) {

        // get state of caret annotations
        TextEditorDefinitions.CARET_ANNOTATIONS.each(new Proc() {

          @Override
          public void apply(String annotationName) {

            String annotationValue =
                EditorAnnotationUtil.getAnnotationOverRangeIfFull(editor.getDocument(),
                    editor.getCaretAnnotations(), annotationName, range.getStart(), range.getEnd());

            annotationMap.put(annotationName, annotationValue);            

          }

        });
        
        // get all custom annotations
        annotationRegistry.keySet().each(new Proc() {

			@Override
			public void apply(String annotationName) {
				
				String annotationValue =
		                EditorAnnotationUtil.getAnnotationOverRangeIfFull(editor.getDocument(),
		                    editor.getCaretAnnotations(), annotationName, range.getStart(), range.getEnd());
				
				annotationMap.put(annotationName, annotationValue); 
			}
        });        
        
        // get paragraph annotations
        TextEditorDefinitions.PARAGRAPH_ANNOTATIONS.each(new Proc() {

          @Override
          public void apply(String annotationName) {

            Collection<Entry<String, LineStyle>> styles =
                TextEditorDefinitions.ParagraphAnnotation.fromString(annotationName).values
                    .entrySet();

            String annotationValue = null;
            for (Entry<String, LineStyle> s : styles) {
              if (Paragraph.appliesEntirely(editor.getDocument(), range.getStart(), range.getEnd(),
                  s.getValue())) {
                annotationValue = s.getKey();
                break;
              }
            }
            annotationMap.put(annotationName, annotationValue); 
          }
        });
      }

      return annotationMap;
  }
  
  /**
   * Generate a map of all accepted annotations with the default value as null
   * 
   * @return
   */
  protected JsoStringMap<String> getAnnotationsByDefault() {
	  
	  // Map to contain the current state of each annotation
      final JsoStringMap<String> annotationMap = JsoStringMap.<String>create();
      
      // caret annotations
      TextEditorDefinitions.CARET_ANNOTATIONS.each(new Proc() {
        @Override
        public void apply(String annotationName) {
          annotationMap.put(annotationName, null);            

        }
      });
      
      // get all custom annotations
      annotationRegistry.keySet().each(new Proc() {
			@Override
			public void apply(String annotationName) {
				annotationMap.put(annotationName, null); 
			}
      });        
      
      // get paragraph annotations
      TextEditorDefinitions.PARAGRAPH_ANNOTATIONS.each(new Proc() {

        @Override
        public void apply(String annotationName) {        	
        	annotationMap.put(annotationName, TextEditorDefinitions.ParagraphAnnotation.fromString(annotationName).defaultValue); 
        }
        
      });

      return annotationMap;     
  }
  

  
  /**
   * Return the list of all ranges in the document having the annotation.
   * 
   * @param key the annotation name to list.
   */
  public JsArray<JsoAnnotation> getAnnotationSet(String key) {
	  
		Preconditions.checkArgument(isValidAnnotationKey(key), "Invalid annotation key");

		@SuppressWarnings("unchecked")
		JsArray<JsoAnnotation> ranges = (JsArray<JsoAnnotation>) JsArray.createArray();

		if (TextEditorDefinitions.isParagraphAnnotation(key)) {
			
			ParagraphAnnotation pa = ParagraphAnnotation.fromString(key);
			
			Paragraph.traverseDoc(editor.getDocument(), new ContentElement.RangedAction() {
				
				@Override
				public void execute(ContentElement e, Range r) {
					
					for (Entry<String, LineStyle> ls : pa.getLineStyles().entrySet()) {
						if (ls.getValue().isApplied(e)) {					
							// TODO(pablojan) this might not work in all situations. I need look into line/paragraph rendering
							Element implNodelet = (Element) e.getImplNodeletRightwards();														
							ranges.push(JsoParagraphAnnotation.create(editor.getDocument(), r.getStart(), r.getEnd(), key, ls.getKey(), implNodelet));
						}
					}	
					
				}
			});
			

		} else {

			JsoStringSet annotationNames = JsoStringSet.create();
			annotationNames.add(key);

			for (AnnotationInterval<String> interval : editor.getDocument().annotationIntervals(0,
					editor.getDocument().size(), annotationNames)) {
				// TODO(pablojan) checking for intervals with actual value for
				// the annotation, if not, void intervals are returned
				if (interval.annotations().get(key) != null)
					ranges.push(JsoAnnotation.create(editor, new Range(interval.start(), interval.end()), key));
			}

		}

		return ranges;
  }
  
    
  public JsoAnnotation getAnnotationInRange(JsoRange editorRange, String key) {
    Preconditions.checkNotNull(editorRange, "Range can't be null");
    Preconditions.checkArgument(isValidAnnotationKey(key), "Invalid annotation key");
    
    if (TextEditorDefinitions.isParagraphAnnotation(key)) {
      
      for(Entry<String, LineStyle> ls: ParagraphAnnotation.fromString(key).getLineStyles().entrySet()) {
      
        if (Paragraph.appliesEntirely(editor.getDocument(), editorRange.start(), editorRange.end(), ls.getValue())) {
        Point<ContentNode> point = editor.getDocument().locate(editorRange.start());
          ContentNode lineNode = LineContainers.getRelatedLineElement(editor.getDocument(), point);                  
          return JsoParagraphAnnotation.create(editor.getDocument(), editorRange.start(), editorRange.end(), key, ls.getKey(), lineNode.asElement().getImplNodelet());
        }
        
      }
      
    } else {
      
      Range range = EditorAnnotationUtil.getEncompassingAnnotationRange(editor.getDocument(), key, editorRange.start());  
      if (range == null)
        return null;
      
      return JsoAnnotation.create(editor, range, key);
    }
    
  return null;
    
    
  }  
  
  
  
  /**
   * Toggle a paragraph annotation in the provided range. To unset the annotation use null value.
   * 
   * @param annotationName
   * @param annotationValue
   * @param start
   * @param end
   */
  protected void setParagraphAnnotation(String annotationName, String annotationValue, int start, int end) {
		
		Preconditions.checkArgument(TextEditorDefinitions.isParagraphAnnotation(annotationName),
				"Invalid paragraph annotation");

		ParagraphAnnotation annotation = TextEditorDefinitions.ParagraphAnnotation.fromString(annotationName);
		final LineStyle style = annotation.getLineStyleForValue(annotationValue);
		

    if (annotationName.equals(ParagraphAnnotation.HEADER.toString())) {
      Date now = new Date();
      style.setId(String.valueOf(now.getTime()) + String.valueOf(start) + String.valueOf(end)); 
    }
		
		final boolean isOn = annotationValue != null && !annotationValue.isEmpty();

		editor.undoableSequence(new Runnable() {
			@Override
			public void run() {
				Paragraph.apply(editor.getDocument(), start, end, style, isOn);
			}
		});
	  
  }

  /**
   * Set any type of annotation in the current selected text or caret.
   * 
   * @param key
   * @param value
   */
	public JsoAnnotation setAnnotation(String key, String value) {
	
		Preconditions.checkArgument(isValidAnnotationKey(key), "Unknown annotation key");
		final Range range = editor.getSelectionHelper().getOrderedSelectionRange();
		if (range == null) return null;
		
		if (TextEditorDefinitions.isParagraphAnnotation(key)) {
			setParagraphAnnotation(key, value, range.getStart(), range.getEnd());
		} else {
			EditorAnnotationUtil.setAnnotationOverSelection(editor, key, value);
		}
		
		
		return JsoAnnotation.create(editor, range, key);
	}
	
	/**
	 * Set any type of annotation in a range of text.
	 * 
	 * @param range
	 * @param key
	 * @param value
	 */
	public JsoAnnotation setAnnotationInRange(JsoRange range, String key, String value) {	
		Preconditions.checkArgument(isValidAnnotationKey(key), "Unknown annotation key");
		Preconditions.checkArgument(range != null && range.start() <= range.end(), "Invalid range object");
		
		if (TextEditorDefinitions.isParagraphAnnotation(key)) {
			setParagraphAnnotation(key, value, range.start(), range.end());
		} else {
			EditorAnnotationUtil.setAnnotationOverRange(editor.getDocument(), editor.getCaretAnnotations(), key, value, range.start(), range.end());
		}		
		
		return JsoAnnotation.create(editor, new Range(range.start(), range.end()), key);
	}
  
  /**
   * Clear the annotation in the current selection or caret position.
   * 
   * @param keyPrefix
   */
  public void clearAnnotation(String keyPrefix) {      
    Range r = editor.getSelectionHelper().getOrderedSelectionRange();
    if (r == null) {
      return;
    }   
    clearAnnotationInRange(JsoRange.Builder.create(editor.getDocument()).range(r).build(), keyPrefix);
  }  
  
  
  /**
   * Clear all annotations in the range starting with the prefix
   * 
   * @param range the range in the doc
   */
  public void clearAnnotationInRange(JsoRange range, String keyPrefix) {
    Preconditions.checkNotNull(keyPrefix, "Annotation key or prefix can't be null");    
	  Preconditions.checkNotNull(range, "Range can't be null");
	 	
	  StringMap<String> annotations = range.getAnnotations();
	  if (annotations.isEmpty()) {
	    annotations = getAllAnnotationsInRange(new Range(range.start(), range.end()));
	  }
	  	  
	  List<String> textAnnotations = new ArrayList<String>();
	  
	  annotations.each(new ProcV<String>() {

      @Override
      public void apply(String key, String value) {
        if (key.startsWith(keyPrefix)) {
          if (TextEditorDefinitions.isParagraphAnnotation(key))
            setParagraphAnnotation(key, null, range.start(), range.end());
          else
            textAnnotations.add(key);
        }
      }
	    
	  });

	  	  
	  if (!textAnnotations.isEmpty()) {
	    String[] anotArray = (String[]) textAnnotations.toArray(new String[]{});
		  EditorAnnotationUtil.clearAnnotationsOverRange(editor.getDocument(), editor.getCaretAnnotations(), anotArray, range.start(), range.end());
	  }
  }

  /**
   * Insert o replace text over range in the document
   *  
   * @param range
   * @param text
   */
  public JsoRange setText(JsoRange range, String text) {
    Preconditions.checkNotNull(range, "Range can't be null");
    CMutableDocument doc = editor.getDocument();
    if (range.start() == range.end()) {
      doc.insertText(range.start(), text);
    } else if (range.start() > 0 && range.start() < range.end() && range.end() < editor.getDocument().size()) {
      doc.beginMutationGroup();
      doc.deleteRange(range.start(), range.end());
      doc.insertText(range.start(), text);
      doc.endMutationGroup();
    } else {
      Preconditions.checkArgument(false, "Range is not correct");
    }
    
    
    return JsoRange.Builder.create(doc).range(range.start(), range.start()+text.length(), text.length()).build();
    
  }
  
  /**
   * Delete text in a range
   * 
   * @param range
   * @return
   */
  public void deleteText(JsoRange range) {
    Preconditions.checkNotNull(range, "Range can't be null");
    CMutableDocument doc = editor.getDocument();
    doc.deleteRange(range.start(), range.end());
  }
  
  
  public String getText(JsoRange range) {
    Preconditions.checkNotNull(range, "Range can't be null");
    CMutableDocument doc = editor.getDocument();
    Preconditions.checkArgument(range.start() >= 0 && range.start() <= range.end() && range.end() < doc.size(), "Range is not correct");
    return DocHelper.getText(doc, range.start(), range.end());
  }
 
  /**
   * Gets the current selection. See {@link JsoRange} for methods to
   * update the document's selection.
   * 
   *  Includes annotations.
   * 
   * @return
   */
  public JsoRange getSelection() {
    Range r = editor.getSelectionHelper().getOrderedSelectionRange();
    if (r == null) {
      return null;
    }   
    return JsoRange.Builder.create(editor.getDocument()).range(r)
        .annotations(getAllAnnotationsInRange(r)).build();
  }
  
  

  @Override
  public void onUpdate(final EditorUpdateEvent event) {
    if (event.selectionLocationChanged()) {
        Range range = editor.getSelectionHelper().getOrderedSelectionRange();
        JsoRange.Builder editorRangeBuilder = JsoRange.Builder.create(editor.getDocument());
        if (range != null) {
          editorRangeBuilder.range(range)
          .annotations(getAllAnnotationsInRange(range)).build();
        } else {
          editorRangeBuilder.annotations(getAnnotationsByDefault());
        }
        
        
    if (listener != null)
      listener.onSelectionChange(editorRangeBuilder.build());
        
    }
  }
  
}
