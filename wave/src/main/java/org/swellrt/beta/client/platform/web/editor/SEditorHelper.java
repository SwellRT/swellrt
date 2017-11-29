package org.swellrt.beta.client.platform.web.editor;

import java.util.Iterator;

import org.waveprotocol.wave.client.common.util.JsoStringSet;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringSet;

public class SEditorHelper {
  
  public static boolean inRange(Range container, Range inner) {
    return container.getStart() <= inner.getStart() && inner.getEnd() <= container.getEnd();
  }
  
  public static Range getFullValidRange(EditorContext editor) {
    int start = editor.getDocument().getLocation(editor.getSelectionHelper().getFirstValidSelectionPoint());
    int end = editor.getDocument().getLocation(editor.getSelectionHelper().getLastValidSelectionPoint());
    return new Range(start, end);   
  }

  /**
   * This is weak logic to get the full valid range of a document.
   * It shouldn't be used. 
   * @param doc
   * @return
   */
  public static Range getFullValidRange(CMutableDocument doc) {   
    int start = 3;
    int end = doc.size() - 3;
    return new Range(start, end);   
  }
  
  public static Range replaceText(CMutableDocument doc, Range range, String text) {
    Preconditions.checkArgument(range != null, "Null range");
    
    if (range.isCollapsed()) {
      
      doc.insertText(range.getStart(), text);
      
    } else if (inRange(getFullValidRange(doc), range)) {     
      
      doc.beginMutationGroup();
      doc.deleteRange(range.getStart(), range.getEnd());
      doc.insertText(range.getStart(), text);
      doc.endMutationGroup();
      
      
    } else {
      Preconditions.checkArgument(false, "Range out of bounds");
    }
    
    return new Range(range.getStart(), range.getStart()+ text.length());
  }
  
  
  public static Range getAnnotationRange(ContentElement node, String name) {
    
    int start = node.getMutableDoc().getLocation(node);
    int end = node.getMutableDoc().getLocation(node.getNextSibling());

    
    StringSet keys = JsoStringSet.create();
    keys.add(name);
    Iterator<RangedAnnotation<String>> it = node.getMutableDoc().rangedAnnotations(start, end, keys).iterator();
    while (it.hasNext()) {
      RangedAnnotation<String> ra = it.next();
      if (ra.value() != null && ra.key().equals(name)) {
        return new Range(ra.start(), ra.end());
      }
    }

    return new Range(start, start);
  }
}
