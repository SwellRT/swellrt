package org.swellrt.beta.client.js.editor.annotation;

import org.waveprotocol.wave.client.doodad.annotation.GeneralAnnotationHandler;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.model.document.util.Range;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

/**
 * Convenience wrapper of (non-paragraph) annotations.
 * <p>
 * More info at {@link GeneralAnnotationHandler} and {@link AnnotationPaint} 
 *  
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class TextAnnotation implements Annotation, AnnotationPaint.EventHandler, AnnotationPaint.MutationHandler {

  private final String name;
  private AnnotationEventHandler handler; 
  
  private final String contentNodeAttributeName;
  
  public TextAnnotation(String name) {
    super();
    this.name = name;
    this.contentNodeAttributeName = AnnotationPaint.VALUE_ATTR_PREFIX + GeneralAnnotationHandler.getSafeKey(name);
  }

  @Override
  public void set(EditorContext editor, Range range, String value) {
    EditorAnnotationUtil.setAnnotationOverRange(editor.getDocument(), editor.getCaretAnnotations(), name, value, range.getStart(), range.getEnd());
  }

  @Override
  public void reset(EditorContext editor, Range range) {
    EditorAnnotationUtil.clearAnnotationsOverRange(editor.getDocument(), editor.getCaretAnnotations(), new String[]{ name }, range.getStart(), range.getEnd());
  }

  public void setHandler(AnnotationEventHandler h) {    
    this.handler = h;
    if (handler != null) {
      AnnotationPaint.registerEventHandler(name, this);
      AnnotationPaint.setMutationHandler(name, this);
    } else {
      // clear event handler?
      AnnotationPaint.clearMutationHandler(name);
    }
  }
  
  protected Range getNodeRange(ContentElement node) {
    
    int start = node.getContext().locationMapper().getLocation(node);
    int end = node.getContext().locationMapper().getLocation(node.getNextSibling());
    return new Range(start, end);

  }
  
  protected Element getNodeElement(ContentElement node) {
    /*
    Point<ContentNode> point = doc.locate(start);
    if (point != null)
      node = point.getCanonicalNode().getImplNodeletRightwards();
      */    
    return node.asElement().getImplNodelet();
  }
     
  @Override
  public void onAdded(ContentElement node) {
    if (handler != null) {      
      String value = node.getAttribute(this.contentNodeAttributeName);
      Range range = getNodeRange(node);
      handler.onAdded(getNodeElement(node), value, range);
    }    
  }

  @Override
  public void onMutation(ContentElement node) {
    if (handler != null) {
      String value = node.getAttribute(this.contentNodeAttributeName);
      Range range = getNodeRange(node);
      handler.onMutation(getNodeElement(node), value, range);
    }   
  }

  @Override
  public void onRemoved(ContentElement node) {
    if (handler != null) {
      String value = node.getAttribute(this.contentNodeAttributeName);
      Range range = getNodeRange(node);
      handler.onRemoved(getNodeElement(node), value, range);
    }   
  }

  @Override
  public void onEvent(ContentElement node, Event event) {
    if (handler != null) {
      String value = node.getAttribute(this.contentNodeAttributeName);
      Range range = getNodeRange(node);
      handler.onEvent(event, getNodeElement(node), value, range);
    }   
  }
  

  
}
