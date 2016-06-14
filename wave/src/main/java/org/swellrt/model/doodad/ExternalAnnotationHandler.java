package org.swellrt.model.doodad;

import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.AnnotationFamily;
import org.waveprotocol.wave.model.document.AnnotationBehaviour.DefaultAnnotationBehaviour;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import java.util.Collections;
import java.util.Map;


/**
 * A handler for annotations managed by client apps. SwellRT must block the use
 * of Wave original annotations from clients.
 * 
 * @author Pablo Ojanguren (pablojan@gmail.com)
 * 
 */
public class ExternalAnnotationHandler implements AnnotationMutationHandler {

  private static final String EXTERNAL_ANNOTATION_PREFIX = "ext";
  private static final String EXTERNAL_ANNOTATION_CLASS_KEY = "ext/class";

  private static final ReadableStringSet KEYS = CollectionUtils
      .newStringSet(EXTERNAL_ANNOTATION_CLASS_KEY);

  private final AnnotationPainter painter;


  /**
   * Transform annotation value to attributes for the local node of the
   * intermediate model. In particular, it passes a class name for HTML rendered
   * annotation.
   */
  private static final PaintFunction localNodePainter = new PaintFunction() {
    @Override
    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {


      Object annotationValue = from.get(EXTERNAL_ANNOTATION_CLASS_KEY);
      if (annotationValue != null) {
        String className = (String) annotationValue;
        return Collections.singletonMap(AnnotationPaint.CLASS_ATTR, className);
      } else {
        return Collections.emptyMap();
      }
    }
  };


  public static void register(Registries registries) {
    PainterRegistry painterRegistry = registries.getPaintRegistry();
    ExternalAnnotationHandler handler = new ExternalAnnotationHandler(painterRegistry.getPainter());

    registries.getAnnotationHandlerRegistry().registerHandler(EXTERNAL_ANNOTATION_PREFIX, handler);
    registries.getAnnotationHandlerRegistry().registerBehaviour(EXTERNAL_ANNOTATION_PREFIX,
        new DefaultAnnotationBehaviour(AnnotationFamily.CONTENT));

    painterRegistry.registerPaintFunction(KEYS, localNodePainter);
  }


  public ExternalAnnotationHandler(AnnotationPainter painter) {
    this.painter = painter;
  }


  @Override
  public <N, E extends N, T extends N> void handleAnnotationChange(DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue) {
    painter.scheduleRepaint(bundle, start, end);
  }

}
