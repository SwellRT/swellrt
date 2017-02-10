package org.swellrt.beta.client.js.editor.annotation;

import java.util.Map;

import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.misc.AnnotationPaint;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.Alignment;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.EventHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.Paragraph.MutationHandler;
import org.waveprotocol.wave.client.editor.content.paragraph.ParagraphBehaviour;
import org.waveprotocol.wave.model.document.util.Range;

import com.google.gwt.user.client.Event;

/**
 * Paragraph annotation type. See internals in {@See Paragraph}.
 *  
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ParagraphValueAnnotation implements Annotation, Annotation.Listenable, EventHandler, MutationHandler {
  
  private final String name;
  private final Map<String, Paragraph.LineStyle> styles;
  private final AttributeGenerator attributeGen;
  private final ParagraphBehaviour behaviour;
  
  private AnnotationInstance.Handler handler; 
  
  public ParagraphValueAnnotation(ParagraphBehaviour behaviour, String name, Map<String, Paragraph.LineStyle> styles, AttributeGenerator attributeGen) {
    this.name = name;
    this.styles = styles;
    this.attributeGen = attributeGen;    
    this.behaviour = behaviour;
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
  
  public String apply(ContentElement e) {
    
    final String[] result = new String[1];
    
    this.styles.forEach( (value , style) -> {            
      if (!value.equals("default") && style.isApplied(e))
        result[0] = value;
    });
    
    return result[0];
    
  }

  @Override
  public void setHandler(AnnotationInstance.Handler h) {
    this.handler = h;
    Paragraph.registerEventHandler(behaviour, this);
    Paragraph.registerMutationHandler(behaviour, this);
  }

  
  private static String getParagraphAnnotationValue(ContentElement node, ParagraphBehaviour behaviour) {
    if (ParagraphBehaviour.DEFAULT.equals(behaviour)) {
      return Alignment.fromValue(node.asElement().getAttribute(Paragraph.ALIGNMENT_ATTR)).cssValue();
    } else if (ParagraphBehaviour.HEADING.equals(behaviour)) {
      return node.asElement().getAttribute(Paragraph.SUBTYPE_ATTR);
    } else if (ParagraphBehaviour.LIST.equals(behaviour)) {
      return node.asElement().getAttribute(Paragraph.LIST_STYLE_ATTR);
    }
    
    return null;
  }
  
  @Override
  public void onAdded(ContentElement node) {
    if (handler != null) {      
      String value = getParagraphAnnotationValue(node, behaviour);        
      handler.exec(AnnotationInstance.EVENT_ADDED, AnnotationInstance.create(node.getMutableDoc(), name, value, node), null);
    }        
  }

  @Override
  public void onMutation(ContentElement node) {
    if (handler != null) {      
      String value = getParagraphAnnotationValue(node, behaviour);        
      handler.exec(AnnotationInstance.EVENT_MUTATED, AnnotationInstance.create(node.getMutableDoc(), name, value, node), null);
    }   
  }

  @Override
  public void onRemoved(ContentElement node) {
    if (handler != null) {      
      String value = getParagraphAnnotationValue(node, behaviour);        
      handler.exec(AnnotationInstance.EVENT_REMOVED, AnnotationInstance.create(node.getMutableDoc(), name, value, node), null);
    }   
  }

  @Override
  public void onEvent(ContentElement node, Event event) {
    if (handler != null) {      
      String value = getParagraphAnnotationValue(node, behaviour);        
      handler.exec(AnnotationInstance.EVENT_MOUSE, AnnotationInstance.create(node.getMutableDoc(), name, value, node), event);
    }   
  }
  
}
