package org.swellrt.beta.client.js.editor.annotation;

import java.util.Map;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.model.document.util.Range;

/**
 * Paragraph annotation type. See internals in {@See Paragraph}.
 *  
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ParagraphValueAnnotation implements Annotation {
  
  private final String name;
  private final Map<String, Paragraph.LineStyle> styles;
  private final AttributeGenerator attributeGen;
  
  public ParagraphValueAnnotation(String name, Map<String, Paragraph.LineStyle> styles, AttributeGenerator attributeGen) {
    this.name = name;
    this.styles = styles;
    this.attributeGen = attributeGen;    
  }
  
  public String getName() {
    return name;
  }
  
  @Override
  public void set(EditorContext editor, Range range, String styleKey) {
    if (range != null && editor != null) {

      if (styleKey != null && !styleKey.isEmpty() && !styles.containsKey(styleKey))
        return;
      
      final boolean isOn = (styleKey != null && !styleKey.isEmpty() && !styleKey.equals("default"));
      if (!isOn)
        styleKey = "default";
      
      Paragraph.LineStyle style = styles.get(styleKey);
      if (attributeGen != null)
      style.setAttributes(attributeGen.generate(editor, range, styleKey));

      editor.undoableSequence(new Runnable() {
        @Override
        public void run() {
          Paragraph.apply(editor.getDocument(), range.getStart(), range.getEnd(), style, isOn);
          style.setAttributes(null);
        }
      });
      
    }
  }

  @Override
  public void reset(EditorContext editor, Range range) {
    set(editor, range, null);
  }
  
  public String apply(EditorContext editor, Range range) {
    
    for (String styleValue: styles.keySet()) {
      if (Paragraph.appliesEntirely(editor.getDocument(), range.getStart(), range.getEnd(), styles.get(styleValue))) {
        return styleValue.equals("default") ? null : styleValue;
      }
    }
    
    return null;
  }
}
