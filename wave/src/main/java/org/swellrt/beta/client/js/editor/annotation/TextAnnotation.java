package org.swellrt.beta.client.js.editor.annotation;

import org.swellrt.beta.client.js.editor.SEditorHelper;
import org.waveprotocol.wave.client.doodad.annotation.GeneralAnnotationHandler;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.content.misc.CaretAnnotations;
import org.waveprotocol.wave.client.editor.util.EditorAnnotationUtil;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.indexed.LocationMapper;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.ReadableStringSet;

import com.google.gwt.user.client.Event;

/**
 * Convenience wrapper of (non-paragraph) annotations.
 * <p>
 * More info at {@link GeneralAnnotationHandler} and {@link AnnotationPaint} 
 *  
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class TextAnnotation implements Annotation, Annotation.Listenable, AnnotationPaint.EventHandler, AnnotationPaint.MutationHandler {

  
  public static void clearRange(MutableAnnotationSet<String> doc,
      CaretAnnotations caret, ReadableStringSet keys, int start, int end) {
    
    EditorAnnotationUtil.clearAnnotationsOverRange(doc, caret, keys, start, end);
  }
  
  private final String name;
  private AnnotationInstance.Handler handler; 
  
  private final String contentNodeAttributeName;
  
  public TextAnnotation(String name) {
    super();
    this.name = name;
    this.contentNodeAttributeName = AnnotationPaint.VALUE_ATTR_PREFIX + GeneralAnnotationHandler.getSafeKey(name);
  }

  public String getName() {
    return name;
  }
  
  @Override
  public void set(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range, String value) {     
    if (Annotation.isLocal(name)) {
      localAnnotations.setAnnotation(range.getStart(), range.getEnd(), name, value);
    } else {
      EditorAnnotationUtil.setAnnotationOverRange(doc, caret, name, value, range.getStart(), range.getEnd());
    }   
  }

  @Override
  public void reset(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range) {
    if (Annotation.isLocal(name)) { 
      localAnnotations.setAnnotation(range.getStart(), range.getEnd(), name, null);
    } else {
      EditorAnnotationUtil.clearAnnotationsOverRange(doc, caret, new String[]{ name }, range.getStart(), range.getEnd());
    }
  }
  

  @Override
  public void update(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range, String value) {
    set(doc, mapper, localAnnotations, caret, range, value);

    
  }

  @Override
  public Range mutate(CMutableDocument doc, LocationMapper<ContentNode> mapper, MutableAnnotationSet<Object> localAnnotations, CaretAnnotations caret, Range range, String text, String value) {
    reset(doc, mapper, localAnnotations, caret, range);    
    Range mutatedRange = SEditorHelper.replaceText(doc, range, text); 
    set(doc, mapper, localAnnotations, caret, mutatedRange, value);
    return mutatedRange;
  }

  
  @Override
  public void setHandler(AnnotationInstance.Handler h) {    
    this.handler = h;
    if (handler != null) {
      AnnotationPaint.registerEventHandler(name, this);
      AnnotationPaint.setMutationHandler(name, this);
    } else {
      // clear event handler?
      AnnotationPaint.clearMutationHandler(name);
    }
  }
  
  

     
  @Override
  public void onAdded(ContentElement node) {
    if (handler != null) {      
      String value = node.getAttribute(this.contentNodeAttributeName);          
      handler.exec(AnnotationInstance.EVENT_ADDED, AnnotationInstance.create(name, value, node), null);
    }    
  }

  @Override
  public void onMutation(ContentElement node) {
    if (handler != null) {
      String value = node.getAttribute(this.contentNodeAttributeName);
      handler.exec(AnnotationInstance.EVENT_MUTATED, AnnotationInstance.create(name, value, node), null);
    }   
  }

  @Override
  public void onRemoved(ContentElement node) {
    if (handler != null) {
      String value = node.getAttribute(this.contentNodeAttributeName);
      handler.exec(AnnotationInstance.EVENT_REMOVED, AnnotationInstance.create(name, value, node), null);
    }   
  }

  @Override
  public void onEvent(ContentElement node, Event event) {
    if (handler != null) {
      String value = node.getAttribute(this.contentNodeAttributeName);
      handler.exec(AnnotationInstance.EVENT_MOUSE, AnnotationInstance.create(name, value, node), event);
    }   
  }

  
}
