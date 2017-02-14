package org.swellrt.beta.client.js.editor.annotation;

import java.util.Set;

import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.model.document.util.Range;

public interface ParagraphAnnotation extends Annotation {

  public static void clearRange(ContentDocument doc, Range range,  Set<ParagraphAnnotation> annotations) {
   
    for (Annotation a: annotations) {
      a.reset(doc.getMutableDoc(), doc.getLocationMapper(), doc.getLocalAnnotations(), doc.getContext().editing().editorContext().getCaretAnnotations(), range);      
    }
    
  }
  
}
