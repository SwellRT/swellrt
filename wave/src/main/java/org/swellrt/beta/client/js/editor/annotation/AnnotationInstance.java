package org.swellrt.beta.client.js.editor.annotation;

import java.util.Iterator;

import org.swellrt.beta.client.js.editor.SEditorHelper;
import org.waveprotocol.wave.client.common.util.JsoStringSet;
import org.waveprotocol.wave.client.editor.EditorContext;
import org.waveprotocol.wave.client.editor.content.CMutableDocument;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.model.document.RangedAnnotation;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.LineContainers;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.StringSet;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * 
 * 
 * @author pablojan@gmail.com
 *
 */
@JsType(namespace = "swellrt.Editor", name = "Annotation")
public class AnnotationInstance {

  /** the selection range is equals to annotation range or inside it */
  public static final int MATCH_IN = 0;
  /** the selection range is partially out of the annotation range or selection spans more content beyond annotation */
  public static final int MATCH_OUT = 1;
  
  public static int getRangeMatch(Range selectionRange, Range annotationRange) {  
    boolean in = selectionRange.equals(annotationRange) || ( selectionRange.getStart() >= annotationRange.getStart() && selectionRange.getEnd() <= annotationRange.getEnd());     
    return in ? AnnotationInstance.MATCH_IN : AnnotationInstance.MATCH_OUT;
  }
  
  String name;
  String value;
  Range range;
  ContentElement node;
  int matchType;
  CMutableDocument doc;
  
  protected static Node lookupNodelet(ContentNode n) {
    if (n != null) {
        Node rightwardsNodelet = n.getImplNodeletRightwards();
        if (rightwardsNodelet != null) {
          return rightwardsNodelet;
        }
    }
    
    return null;
  }
  
  
  protected static Range getAnnotationRange(ContentElement node, String name) {
    
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
 
  

  /**
   * Use this method from annotation search. See {@link AnnotationRegistry}
   * 
   */
  protected static AnnotationInstance create(CMutableDocument doc, String name, String value, Range range, int matchType) {
    return new AnnotationInstance(doc, name, value, range, null, matchType);
  }

  
  /**
   * Use this method from event handlers. See {@link TextAnnotation}
   * 
   */
  protected static AnnotationInstance create(CMutableDocument doc, String name, String value, ContentElement node) {
    return new AnnotationInstance(doc, name, value, getAnnotationRange(node, name), node, MATCH_IN);
  }
    
  protected AnnotationInstance(CMutableDocument doc, String name, String value, Range range, ContentElement node, int matchType) {
    super();
    this.name = name;
    this.value = value;
    this.range = range;
    this.matchType = matchType;
    this.node = node;
    this.doc = doc;
  }
  

  @JsProperty
  public String getName() {
    return name;
  }
  
  @JsProperty
  public String getValue() {
    return value;
  }
  
  @JsProperty
  public String getText() {
    if (range != null) 
      return DocHelper.getText(doc, range.getStart(), range.getEnd());
    else
      return "";
  }
  
  @JsProperty
  public Range getRange() {   
    return range;
  }
  
  
  @JsProperty
  public Element getLine() {
    if (range != null) {
      Point<ContentNode> point = doc.locate(range.getStart()+1);      
      ContentElement lineNode = LineContainers.getRelatedLineElement(doc, point);
      return lookupNodelet(lineNode).getParentElement();
    } else if (node != null) {
      ContentElement lineNode = LineContainers.getRelatedLineElement(doc, doc.locate(doc.getLocation(node)));
      return lookupNodelet(lineNode).getParentElement();
    }
    
    return null;
  }
  
  @JsProperty
  public Node getNode() {
    if (this.node != null) {
      return node.getImplNodelet();
    } else if (range != null) {
       Point<ContentNode> point = doc.locate(range.getStart()+1);    
      return point.getContainer().getParentElement().getImplNodelet();
    }
    
    return null;
  }
  
  @JsProperty
  public int getMatchType() {
    return matchType;
  }
  
  public void update(String value) {
    if (range != null) {
      doc.setAnnotation(range.getStart(), range.getEnd(), name, value);
      this.value = value;
    }
  }

  public AnnotationInstance mutate(String text) {
    if (range == null) return null;
    
    String value = this.value;
    // remove old annotation
    clear(); 
    // edit text
    Range mutatedRange = SEditorHelper.replaceText(doc, range, text); 
    // create
    doc.setAnnotation(mutatedRange.getStart(), mutatedRange.getEnd(), name, value);
    return new AnnotationInstance(doc, name, value, mutatedRange, null, MATCH_IN);
  }

  public void clear() {
    if (range != null) {
      doc.setAnnotation(range.getStart(), range.getEnd(), name, null);
      this.value = null;
    }
  }
  

  
}
