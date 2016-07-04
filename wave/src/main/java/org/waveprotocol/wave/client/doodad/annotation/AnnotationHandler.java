package org.waveprotocol.wave.client.doodad.annotation;

import org.waveprotocol.wave.client.doodad.link.LinkAnnotationHandler.LinkAttributeAugmenter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint.EventHandler;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint.MutationHandler;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.AnnotationFamily;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.DefaultAnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.util.StringSet;

import com.google.gwt.user.client.Event;

import java.util.HashMap;
import java.util.Map;


/**
 * Annotations handler configurable with {@link AnnotationController} instances.
 * 
 * @author Pablo Ojanguren (pablojan@gmail.com)
 * 
 */
public class AnnotationHandler implements AnnotationMutationHandler {
	
  public static final String ANNOTATION_PREFIX = "annotation";

  private final AnnotationPainter painter;

  /**
   * A logic to set paint attributes for the annotation (see {@link AnnotationPaint}).
   * Eventually, these attributes are rendered to HTML by {@link AnnotationSpreadRenderer}. 
   */
	private static class RenderFunc implements PaintFunction {

		private final LinkAttributeAugmenter augmenter;
		private final StringMap<AnnotationController> controllers;

		public RenderFunc(LinkAttributeAugmenter augmenter, StringMap<AnnotationController> controllers) {
			this.augmenter = augmenter;
			this.controllers = controllers;
		}

		public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {

			Map<String, String> ret = new HashMap<String, String>();

			for (String key : from.keySet()) {

				AnnotationController controller = controllers.get(key);
				
				if (controller != null) {
					//
					// <l:s value-comment="234235"> ... </l:s>
					//
					ret.put(AnnotationPaint.VALUE_ATTR+"-"+(key.replace("/", "-")), from.get(key).toString());

					//
					// <l:s mouseListener="comment"> ... </l:s>
					//
					ret.put(AnnotationPaint.MOUSE_LISTENER_ATTR, key);

					// <l:s mutationListener="comment"> ... </l:s>
					ret.put(AnnotationPaint.MUTATION_LISTENER_ATTR, key);
					
					//
					// <l:s class="comment"> .. </l:s>
					//
					ret.put(AnnotationPaint.CLASS_ATTR, controller.getCSSClass());
				
					// CSS inline styles
					for (int i = 0; i < controller.getStyleNames().length(); i++) {
						String styleName = controller.getStyleNames().get(i);
						ret.put(styleName, controller.getStyleValue(styleName));
					}
					
				}
			}

			return augmenter.augment(from, isEditing, ret);
		}
	}


  public static void register(Registries registries, final StringMap<AnnotationController> annotationControllers) {
  
    PainterRegistry painterRegistry = registries.getPaintRegistry();    
    AnnotationHandler handler = new AnnotationHandler(painterRegistry.getPainter());

    final StringSet KEYS = CollectionUtils.copyStringSet(annotationControllers.keySet());     
    //
    // NOTE: AnnotationPaint.registerEventHandler should be cleared every time the editor is initialized
    //
	KEYS.each(new Proc() {
		@Override
		public void apply(final String key) {
			
			registries.getAnnotationHandlerRegistry().registerHandler(key, handler);    
		    registries.getAnnotationHandlerRegistry().registerBehaviour(key,
		        new DefaultAnnotationBehaviour(AnnotationFamily.CONTENT));

			
			AnnotationPaint.registerEventHandler(key, new EventHandler() {

				@Override
				public void onEvent(ContentElement node, Event event) {					
					AnnotationController controller = annotationControllers.get(key);					
					if (controller != null) {
						controller.onEvent(AnnotationContent.get(node), event);
					}
				}
			});
			

			AnnotationPaint.setMutationHandler(key, new MutationHandler() {
				
				@Override
				public void onMutation(ContentElement node) {
					AnnotationController controller = annotationControllers.get(key);
					if (controller != null) {
						controller.onChange(AnnotationContent.get(node));
					}
					
				}
				
			});
			
		}
	});
    
    // Register painter to update attributes of the local view
   
    painterRegistry.registerPaintFunction(KEYS, new RenderFunc(new LinkAttributeAugmenter() {
        @Override
        public Map<String, String> augment(Map<String, Object> annotations, boolean isEditing,
            Map<String, String> current) {        	
          return current;
        }
      }, annotationControllers));
  }


  public AnnotationHandler(AnnotationPainter painter) {
    this.painter = painter;
  }


  @Override
  public <N, E extends N, T extends N> void handleAnnotationChange(DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue) {
    painter.scheduleRepaint(bundle, start, end);
  }

}
