package org.swellrt.beta.client.js.editor.annotation;

import java.util.Map;

import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Unified interface to handle annotations in SwellRT Editor.
 *   
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface Annotation {
  
  
  public static boolean isLocal(String key) {
    return Annotations.isLocal(key);
  }
  
  public interface Listenable {
    
    public void setHandler(AnnotationInstance.Handler h);
    
  }
  

  public interface AttributeGenerator {
    
    public Map<String, String> generate(Range range, String styleKey);
    
  }
  
  /**
   * Apply this annotation at the range of the text managed in the editor.
   */
  public void set(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range, String value);
  
  /**
   * Delete this annotation in the provided ranged.
   */
  public void reset(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range);
  
  /**
   * Updates the value of this annotation within the provided range.
   */
  public void update(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range, String value);
  
  
  /**
   * Mutate the text of contained in this annotation within the provided range.
   */
  public Range mutate(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range, String text, String value);
  
  
}
