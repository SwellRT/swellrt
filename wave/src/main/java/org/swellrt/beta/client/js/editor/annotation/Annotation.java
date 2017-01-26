package org.swellrt.beta.client.js.editor.annotation;

import java.util.Map;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Unified interface to handle annotations in SwellRT Editor.
 *   
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface Annotation {

  public interface AttributeGenerator {
    
    public Map<String, String> generate(EditorContext editor, Range range, String styleKey);
    
  }
  
  /**
   * Apply this annotation at the range of the text managed in the editor
   * @param editor the editor
   * @param range text range
   * @param value text style name, paragraph style or action name of the this annotation type 
   */
  public void set(EditorContext editor, Range range, String value);
  
  
  public void reset(EditorContext editor, Range range);
  
  
}
