package org.waveprotocol.wave.client.doodad.annotation;

import java.util.HashMap;
import java.util.Map;

import org.waveprotocol.wave.client.doodad.annotation.jso.JsoEditorRange;
import org.waveprotocol.wave.client.common.util.JsoStringSet;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoAnnotationController;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint.EventHandler;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint.MutationHandler;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.AnnotationFamily;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.DefaultAnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;

import com.google.gwt.user.client.Event;


/**
 * Annotations handler configurable with {@link JsoAnnotationController} instances.
 * 
 * @author Pablo Ojanguren (pablojan@gmail.com)
 * 
 */
public class AnnotationHandler implements AnnotationMutationHandler {

  private static AnnotationHandler handlerInstance = null;
  
  private final AnnotationPainter painter;

  
  public static String getSafeKey(String key) {
    return key.replace("/", "-");
  }
  
  /**
   * Logic to set paint attributes for the annotation (see {@link AnnotationPaint}).
   * Eventually, these attributes are rendered to HTML by {@link AnnotationSpreadRenderer}. 
   */
	private static class RenderFunc implements PaintFunction {

		private final String key;
		private final String styleClass;
		private final JsoView stylesInline;

		public RenderFunc(String key, String styleClass, JsoView stylesInline) {
			  this.key = key;
			  this.styleClass = styleClass;
			  this.stylesInline = stylesInline;
		}

    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {

      Map<String, String> ret = new HashMap<String, String>();

      for (String key : from.keySet()) {
        if (this.key.equals(key)) {

          String safeKey = getSafeKey(key);
          String safeValue = from.get(key).toString();

          // <l:s value-comment="234235">
          ret.put(AnnotationPaint.VALUE_ATTR_PREFIX + safeKey, safeValue);

          // <l:s eventListener-comment="comment"> ... </l:s>
          ret.put(AnnotationPaint.EVENT_LISTENER_ATTR_PREFIX + safeKey, key);

          // <l:s mutationListener-comment="comment"> ... </l:s>
          ret.put(AnnotationPaint.MUTATION_LISTENER_ATTR_PREFIX + safeKey, key);

          // <l:s class-comment="?"> .. </l:s>
          if (styleClass != null)
            ret.put(AnnotationPaint.CLASS_ATTR, styleClass);

          if (stylesInline != null) {

            stylesInline.each(new ProcV<String>() {

              @Override
              public void apply(String styleName, String styleValue) {
                ret.put(styleName, styleValue);
              }

            });
          }
        }

        
      }
      return ret;
    }
    
  }

	/**
	 * 
	 * 
	 * @param registries
	 * @param key
	 * @param mutationHanlder
	 * @param eventHandler
	 */
	public static void register(Registries registries, String key, MutationHandler mutationHanlder, EventHandler eventHandler, String styleClass, JsoView stylesInline) {

		PainterRegistry painterRegistry = registries.getPaintRegistry();
		
		if (handlerInstance == null)
		  handlerInstance = new AnnotationHandler(painterRegistry.getPainter());
		
		AnnotationRegistry annotationRegistry = registries.getAnnotationHandlerRegistry();
		
		annotationRegistry.registerHandler(key, handlerInstance);
    annotationRegistry.registerBehaviour(key, new DefaultAnnotationBehaviour(AnnotationFamily.CONTENT));
		
    // Register painter to update attributes of the local view
    JsoStringSet keySet = JsoStringSet.create();
    keySet.add(key);
    painterRegistry.registerPaintFunction(keySet, new RenderFunc(key, styleClass, stylesInline));
    
    if (eventHandler != null) 
      AnnotationPaint.registerEventHandler(key, eventHandler);
      
    if (mutationHanlder != null)
      AnnotationPaint.setMutationHandler(key, mutationHanlder);


		// TODO (pablojan) Not sure if boundary functions are required
		//painterRegistry.registerBoundaryFunction(annotationNames, );
	}

	
	public static void register(Registries registries, String key, JsoAnnotationController controller) {
	  
    AnnotationHandler.register(registries, key, 
        new AnnotationPaint.MutationHandler() {
          
      
          @Override
          public void onMutation(ContentElement node) {
            String valueAttr = AnnotationPaint.VALUE_ATTR_PREFIX + AnnotationHandler.getSafeKey(key);
            if (controller != null)
              controller.onChange(JsoEditorRange.Builder.create(node.getMutableDoc()).range(node).annotation(key, node.getAttribute(valueAttr)).build());
          }
        }, 
        
        new AnnotationPaint.EventHandler() {
          
          @Override
          public void onEvent(ContentElement node, Event event) {
            String valueAttr = AnnotationPaint.VALUE_ATTR_PREFIX + AnnotationHandler.getSafeKey(key);
            if (controller != null)
              controller.onEvent(JsoEditorRange.Builder.create(node.getMutableDoc()).range(node).annotation(key, node.getAttribute(valueAttr)).build(), event);
          }
        }, 
        controller != null ? controller.getStyleClass() : null, 
        controller != null ? controller.getStylesInline() : JsoView.create());	  
	  
	}
	
	
  public AnnotationHandler(AnnotationPainter painter) {
    this.painter = painter;
  }


  @Override
  public <N, E extends N, T extends N> void handleAnnotationChange(DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue) {
    painter.scheduleRepaint(bundle, start, end);
  }

  public static boolean isAnnotationNode(ContentNode node) { 
	  ContentElement element = node.asElement();
	  if (element == null) return false;
	  return element.getTagName().equals(AnnotationPaint.SPREAD_FULL_TAGNAME);
  }
  
}
