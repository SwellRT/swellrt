package org.swellrt.client.editor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.swellrt.client.editor.TextEditorDefinitions.ParagraphAnnotation;
import org.swellrt.model.generic.TextType;
import org.swellrt.model.shared.ModelUtils;
import org.waveprotocol.wave.client.common.util.JsoStringMap;
import org.waveprotocol.wave.client.common.util.JsoStringSet;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.doodad.annotation.AnnotationHandler;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoAnnotation;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoAnnotationController;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoEditorRange;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoParagraphAnnotation;
import org.waveprotocol.wave.client.doodad.diff.DiffAnnotationHandler;
import org.waveprotocol.wave.client.doodad.diff.DiffDeleteRenderer;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler;
import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
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
import org.waveprotocol.wave.client.editor.content.paragraph.Line;
import org.waveprotocol.wave.client.editor.content.paragraph.LineRendering;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.LineStyle;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphBehaviour;
import org.waveprotocol.wave.client.editor.keys.KeyBindingRegistry;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
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
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.user.client.Event;

/**
 * A wrapper of the original Wave {@link Editor} to be integrated in the SwellRT
 * Web API. Users can edit TextType instances in this editor.
 *
 * @author pablojan
 *
 */
public class TextEditor implements EditorUpdateListener {


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


  /**
   * The gateway to get UI-versions of Blips. Registry is GWT related, so must
   * be injected in the Editor by the JS API. Model's classes have to ignore it.
   */
  private WaveDocuments<? extends InteractiveDocument> documentRegistry;

  private Editor editor;

  private TextEditorListener listener;

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

  public static TextEditor create(String containerElementId, StringMap<JsoWidgetController> widgetControllers, StringMap<JsoAnnotationController> annotationControllers) {
    Element e = Document.get().getElementById(containerElementId);
    Preconditions.checkNotNull(e, "Editor's parent element doesn't exist");
    
    TextEditor editor = new TextEditor(e, widgetControllers, annotationControllers);
    editor.registerDoodads();
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
  }



  /**
   * Inject document registry which manages UI versions of blips. Registry must
   * be only injected by the JS API.
   *
   * @param documentRegistry
   */
  public void setDocumentRegistry(WaveDocuments<? extends InteractiveDocument> documentRegistry) {
    this.documentRegistry = documentRegistry;
  }


  public void edit(TextType text) {
    Preconditions.checkNotNull(text, "Text object is null");
    Preconditions.checkNotNull(documentRegistry, "Document registry hasn't been initialized");

    if (!isClean()) cleanUp();

    doc = getContentDocument(text);
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

  }



  /* ---------------------------------------------------------- */



  private ContentDocument getContentDocument(TextType text) {
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
    if (editor != null) {
      editor.removeUpdateListener(this);
      editor.removeContentAndUnrender();
      editor.reset();
      doc = null;
    }
  }

  protected boolean isClean() {
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

  protected void registerDoodads() {


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
    // controller to
    // get notified on mutation or input events
    //
    LinkAnnotationHandler.register(registries, new LinkAttributeAugmenter() {
      @Override
      public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
          Map<String, String> current) {
        return current;
      }
    });

    WidgetDoodad.register(registries.getElementHandlerRegistry(), widgetRegistry);

    
    // Configure annotations. 
    // For shake of encapsulation, we don't expose native JS type JsoAnntationController to inner editor classes.
    
   annotationRegistry.each(new ProcV<JsoAnnotationController>() {

    @Override
    public void apply(String key, JsoAnnotationController controller) {
      
      
      if (key.contains(AnnotationConstants.LINK_PREFIX)) {
        
        AnnotationPaint.registerEventHandler(AnnotationConstants.LINK_PREFIX, new EventHandler() {

          @Override
          public void onEvent(ContentElement node, Event event) {
            if (controller != null)
              controller.onEvent(JsoEditorRange.Builder.create(node.getMutableDoc()).range(node).annotation(AnnotationConstants.LINK_PREFIX, null).build(), event);
          }
        });

        AnnotationPaint.setMutationHandler(AnnotationConstants.LINK_PREFIX, new MutationHandler() {

          @Override
          public void onMutation(ContentElement node) {
            if (controller != null)
              controller.onChange(JsoEditorRange.Builder.create(node.getMutableDoc()).range(node).annotation(AnnotationConstants.LINK_PREFIX, null).build());
          }
        });
        
      } else if (TextEditorDefinitions.isParagraphAnnotation(key)) {
      
          if (key.equals(ParagraphAnnotation.HEADER.toString())) {
            
            Paragraph.registerEventHandler(ParagraphBehaviour.HEADING, new Paragraph.EventHandler() {
              
              @Override
              public void onEvent(ContentElement node, Event event) {
                if (controller != null)
                  controller.onEvent(JsoEditorRange.Builder.create(node.getMutableDoc()).range(node).annotation(key, node.getAttribute(Paragraph.SUBTYPE_ATTR)).build(), event);
              }
            });
            
            Paragraph.registerMutationHandler(ParagraphBehaviour.HEADING, new Paragraph.MutationHandler() {

              @Override
              public void onMutation(ContentElement node) {
                if (controller != null)                  
                  controller.onChange(JsoEditorRange.Builder.create(node.getMutableDoc()).range(node).annotation(key, node.getAttribute(Paragraph.SUBTYPE_ATTR)).build());                
              }              
            });
            
          }
        
      } else if (TextEditorDefinitions.isStyleAnnotation(key)) {
        
        // Nothing to do with style annotations
        
      } else {
            
        // Register custom annotations        
        AnnotationHandler.register(registries, key, controller);  
        
      }
      
    }
     
   });
    
    
  }

  
  
  public void setListener(TextEditorListener listener) {
    this.listener = listener;
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
  protected JsoStringMap<String> getAnnotationsOverRange(final Range range) {
	  
	  
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
  
  
  @Override
  public void onUpdate(final EditorUpdateEvent event) {
    if (event.selectionLocationChanged()) {
        Range range = editor.getSelectionHelper().getOrderedSelectionRange();
        JsoEditorRange.Builder editorRangeBuilder = JsoEditorRange.Builder.create(editor.getDocument());
        if (range != null) {
        	editorRangeBuilder.range(range)
					.annotations(getAnnotationsOverRange(range)).build();
        } else {
        	editorRangeBuilder.annotations(getAnnotationsByDefault());
        }
        
        
		if (listener != null)
			listener.onSelectionChange(editorRangeBuilder.build());
        
    }
  }
  
  protected void getParagraphAnnotations(int start, int end, JsoStringMap<String> annotations) {
      
	  TextEditorDefinitions.PARAGRAPH_ANNOTATIONS.each(new Proc() {

          @Override
          public void apply(String annotationName) {

            Collection<Entry<String, LineStyle>> styles =
                TextEditorDefinitions.ParagraphAnnotation.fromString(annotationName).values
                    .entrySet();

            String annotationValue = null;
            for (Entry<String, LineStyle> s : styles) {
              if (Paragraph.appliesEntirely(editor.getDocument(), start, end,
                  s.getValue())) {
                annotationValue = s.getKey();
                break;
              }
            }
            annotations.put(annotationName, annotationValue); 
          }
        });
	  
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
	public void setAnnotation(String key, String value) {
	
		Preconditions.checkArgument(isValidAnnotationKey(key), "Unknown annotation key");
	
		if (TextEditorDefinitions.isParagraphAnnotation(key)) {
			final Range range = editor.getSelectionHelper().getOrderedSelectionRange();
			if (range != null) {
				setParagraphAnnotation(key, value, range.getStart(), range.getEnd());
			}
		} else {
			EditorAnnotationUtil.setAnnotationOverSelection(editor, key, value);
		}
	
	}
	
	/**
	 * Set any type of annotation in a range of text.
	 * 
	 * @param range
	 * @param key
	 * @param value
	 */
	public void setAnnotationOverRange(JsoEditorRange range, String key, String value) {	
		Preconditions.checkArgument(isValidAnnotationKey(key), "Unknown annotation key");
		Preconditions.checkArgument(range != null && range.start() <= range.end(), "Invalid range object");
		
		if (TextEditorDefinitions.isParagraphAnnotation(key)) {
			setParagraphAnnotation(key, value, range.start(), range.end());
		} else {
			EditorAnnotationUtil.setAnnotationOverRange(editor.getDocument(), editor.getCaretAnnotations(), key, value, range.start(), range.end());
		}		
		
	}
  
  /**
   * Clear the annotation in the current selection or caret position.
   * 
   * @param annotationName
   */
  public void clearAnnotation(String annotationName) {
	  Preconditions.checkNotNull(annotationName, "Annotation key can't be null");
	  
	  if (TextEditorDefinitions.isParagraphAnnotation(annotationName)) {
		  final Range range = editor.getSelectionHelper().getOrderedSelectionRange();
    	  setParagraphAnnotation(annotationName, null, range.getStart(), range.getEnd());
      } else {
    	  EditorAnnotationUtil.clearAnnotationsOverSelection(editor, annotationName);
      }
  }  
  
  /**
   * Clear an annotation in the caret or document range. 
   * 
   * @param editorRange the range in the doc
   * @param key annotation key
   */
  public void clearAnnotation(JsoEditorRange editorRange, String key) {
	  Preconditions.checkNotNull(editorRange, "Range can't be null");
	  Preconditions.checkArgument(isValidAnnotationKey(key), "Invalid annotation key");
	  
	  if (TextEditorDefinitions.isParagraphAnnotation(key)) {		  
    	  setParagraphAnnotation(key, null, editorRange.start(), editorRange.end());
      } else {
    	  EditorAnnotationUtil.clearAnnotationsOverRange(editor.getDocument(), editor.getCaretAnnotations(), new String[] { key }, editorRange.start(), editorRange.end());
      }
	  
  }
  
  /**
   * Clear all annotations in the range
   * 
   * @param editorRange the range in the doc
   */
  public void clearAnnotation(JsoEditorRange editorRange) {
	  Preconditions.checkNotNull(editorRange, "Range can't be null");
	  
	  // Separate paragraph annotations
	  List<String> textAnnotations = new ArrayList<String>();
	  for (String s: editorRange.getAnnotationKeys()) {
		  if (TextEditorDefinitions.isParagraphAnnotation(s))
			  setParagraphAnnotation(s, null, editorRange.start(), editorRange.end());
		  else
			  textAnnotations.add(s);
	  }
	  	  
	  if (!textAnnotations.isEmpty())
		  EditorAnnotationUtil.clearAnnotationsOverRange(editor.getDocument(), editor.getCaretAnnotations(), (String[]) textAnnotations.toArray(), editorRange.start(), editorRange.end());
  }

  
  public JsoAnnotation getAnnotation(JsoEditorRange editorRange, String key) {
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
   * Gets the current selection. See {@link JsoEditorRange} for methods to
   * update the document's selection.
   * 
   *  Includes annotations.
   * 
   * @return
   */
  public JsoEditorRange getSelection() {
    Range r = editor.getSelectionHelper().getOrderedSelectionRange();
    if (r == null) {
      return null;
    }
    return JsoEditorRange.Builder.create(editor.getDocument()).range(r)
        .annotations(getAnnotationsOverRange(r)).build();

  }

}
