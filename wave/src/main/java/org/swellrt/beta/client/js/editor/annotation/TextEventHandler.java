package org.swellrt.beta.client.js.editor.annotation;

import org.waveprotocol.wave.client.doodad.annotation.UserAnnotationHandler;
import org.waveprotocol.wave.client.doodad.annotation.UserAnnotationHandler.ContentMutationHandler;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.document.util.Range;

import com.google.gwt.user.client.Event;

public class TextEventHandler
    implements AnnotationPaint.EventHandler, AnnotationPaint.MutationHandler, ContentMutationHandler {

  private final String key;
  private AnnotationEvent.Handler handler;
  /** attribute name in content node where annotation's value is stored */
  private final String valueAttribute;

  public TextEventHandler(String key) {
    super();
    this.key = key;
    this.valueAttribute = AnnotationPaint.VALUE_ATTR_PREFIX + UserAnnotationHandler.getSafeKey(key);
  }

  protected void setHandler(AnnotationEvent.Handler handler) {
    this.handler = handler;
  }


  @Override
  public void onAdded(ContentElement node) {
    if (handler != null && !AnnotationRegistry.muteHandlers) {
      String value = node.getAttribute(this.valueAttribute);
      handler.exec(AnnotationEvent.build(AnnotationEvent.EVENT_DOM_ADDED,
          AnnotationValueBuilder.buildWithNode(key, value, node), null));
    }
  }

  @Override
  public void onMutation(ContentElement node) {
    if (handler != null && !AnnotationRegistry.muteHandlers) {
      String value = node.getAttribute(this.valueAttribute);
      handler.exec(AnnotationEvent.build(AnnotationEvent.EVENT_DOM_MUTATED,
          AnnotationValueBuilder.buildWithNode(key, value, node), null));
    }
  }

  @Override
  public void onRemoved(ContentElement node) {
    if (handler != null && !AnnotationRegistry.muteHandlers) {
      String value = node.getAttribute(this.valueAttribute);
      handler.exec(AnnotationEvent.build(AnnotationEvent.EVENT_DOM_REMOVED,
          AnnotationValueBuilder.buildWithNode(key, value, node), null));
    }
  }

  @Override
  public void onEvent(ContentElement node, Event event) {
    if (handler != null && !AnnotationRegistry.muteHandlers) {
      String value = node.getAttribute(this.valueAttribute);
      handler.exec(AnnotationEvent.build(AnnotationEvent.EVENT_DOM_EVENT,
          AnnotationValueBuilder.buildWithNode(key, value, node), event));
    }
  }

  @Override
  public <N, E extends N, T extends N> void handleMutation(DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue) {
    if (handler != null && !AnnotationRegistry.muteHandlers) {
      try {
        handler.exec(AnnotationEvent.build(
            newValue == null ? AnnotationEvent.EVENT_REMOVED : AnnotationEvent.EVENT_CREATED,
            AnnotationValueBuilder.buildWithRange((CMutableDocument) bundle.document(), key,
                newValue, new Range(start, end), AnnotationValue.MATCH_IN),
            null));
      } catch (Exception e) {

      }

    }

  }

}
