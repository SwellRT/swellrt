package org.swellrt.beta.client.js.editor.annotation;

import java.util.Map;

import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Paragraph annotation type. See internals in {@See Paragraph}.
 *  
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ParagraphActionAnnotation implements ParagraphAnnotation {
  
  private final Map<String, ContentElement.Action> actions;

  
  public ParagraphActionAnnotation(Map<String, ContentElement.Action> actions, ContentElement.Action resetAction) {
    this.actions = actions;
    this.actions.put("reset", resetAction);
  }
  
  @Override
  public void set(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range, String value) {
    if (range != null && doc != null && value != null) {
      
      final ContentElement.Action action = actions.get(value);
      if (action == null)
        return;
      
      Paragraph.traverse(mapper, range.getStart(), range.getEnd(), action);
    }
  }

  @Override
  public void reset(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range) {
    set(doc, mapper, localAnnotations, caret, range, "reset");
  }

  @Override
  public void update(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range, String value) {
    set(doc, mapper, localAnnotations, caret, range, value);   
  }

  @Override
  public Range mutate(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range, String text, String value) {
    // not implemented for this type of annotation
    return null;
  }

}
