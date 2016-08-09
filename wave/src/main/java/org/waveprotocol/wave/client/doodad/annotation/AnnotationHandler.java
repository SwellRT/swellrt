package org.waveprotocol.wave.client.doodad.annotation;

import java.util.HashMap;
import java.util.Map;

import org.waveprotocol.wave.client.doodad.annotation.jso.JsoEditorRange;
import org.waveprotocol.wave.client.common.util.JsoStringSet;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoAnnotationController;
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
import org.waveprotocol.wave.model.document.util.AnnotationRegistry;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;
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
	
  private final AnnotationPainter painter;

  /**
   * Logic to set paint attributes for the annotation (see {@link AnnotationPaint}).
   * Eventually, these attributes are rendered to HTML by {@link AnnotationSpreadRenderer}. 
   */
	private static class RenderFunc implements PaintFunction {

		private final StringMap<JsoAnnotationController> controllers;

		public RenderFunc(StringMap<JsoAnnotationController> controllers) {
			this.controllers = controllers;
		}

		public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {

			Map<String, String> ret = new HashMap<String, String>();

			for (String key : from.keySet()) {

				JsoAnnotationController controller = controllers.get(key);
				
				String safeAnnotationName = key.replace("/", "-");
				String safeAnnotationValue = from.get(key).toString();
				
				if (controller != null) {
					
					// <l:s value-comment="234235">
					ret.put(AnnotationPaint.VALUE_ATTR_PREFIX+safeAnnotationName, safeAnnotationValue);

					// <l:s eventListener-comment="comment"> ... </l:s>
					ret.put(AnnotationPaint.EVENT_LISTENER_ATTR_PREFIX+safeAnnotationName, key);

					// <l:s mutationListener-comment="comment"> ... </l:s>
					ret.put(AnnotationPaint.MUTATION_LISTENER_ATTR_PREFIX+safeAnnotationName, key);
					
					// <l:s class-comment="?"> .. </l:s>
					ret.put(AnnotationPaint.CLASS_ATTR, controller.getStyleClass());
				
					// CSS inline styles
					for (int i = 0; i < controller.getStyleNames().length(); i++) {
						String styleName = controller.getStyleNames().get(i);
						ret.put(styleName, controller.getStyleValue(styleName));
					}				
				}
			}

			return ret;
		}
	}

	/**
	 * Register controllers for generic annotations.
	 * 
	 * @param registries
	 * @param annotationControllers annotation names musy include the "generic/" prefix
	 */
	public static void register(Registries registries,
			final StringMap<JsoAnnotationController> annotationControllers) {

		PainterRegistry painterRegistry = registries.getPaintRegistry();
		AnnotationHandler handler = new AnnotationHandler(painterRegistry.getPainter());		
		final AnnotationRegistry annotationRegistry = registries.getAnnotationHandlerRegistry();
		final StringSet annotationNames = CollectionUtils.copyStringSet(annotationControllers.keySet());
		//
		// NOTE: AnnotationPaint.registerEventHandler should be cleared every
		// time the editor is initialized
		//
		annotationNames.each(new Proc() {
			@Override
			public void apply(final String annotationName) {

				annotationRegistry.registerHandler(annotationName, handler);
				annotationRegistry.registerBehaviour(annotationName, new DefaultAnnotationBehaviour(AnnotationFamily.CONTENT));
				
				AnnotationPaint.registerEventHandler(annotationName, new EventHandler() {

					@Override
					public void onEvent(ContentElement node, Event event) {
						JsoAnnotationController controller = annotationControllers.get(annotationName);
						if (controller != null) {
							controller.onEvent(JsoEditorRange.Builder.create(node.getMutableDoc()).range(node).annotation(annotationName, null).build(),event);
						}
					}
				});

				AnnotationPaint.setMutationHandler(annotationName, new MutationHandler() {

					@Override
					public void onMutation(ContentElement node) {
						JsoAnnotationController controller = annotationControllers.get(annotationName);
						if (controller != null) {									
							controller.onChange(JsoEditorRange.Builder.create(node.getMutableDoc()).range(node).annotation(annotationName, null).build());
						}

					}

				});

			}
		});

		// Register painter to update attributes of the local view

		painterRegistry.registerPaintFunction(annotationNames, new RenderFunc(annotationControllers));
		
		// TODO (pablojan) Not sure if boundary functions are required
		//painterRegistry.registerBoundaryFunction(annotationNames, );
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
