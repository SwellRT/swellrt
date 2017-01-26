package org.swellrt.beta.client.js.editor.annotation;

import org.waveprotocol.wave.model.document.util.Range;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;

import jsinterop.annotations.JsIgnore;
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

  /** the annotation range is equals to selection range or inside it */
  public static final int MATCH_IN = 0;
  /** the annotation range is partially out of the selection range or selection spans more content beyond annotation */
  public static final int MATCH_OUT = 1;
  
  
  String name;
  String value;
  String text;
  Range range;
  Element line;
  Node node;
  int matchType;
  
  
  
  @JsIgnore
  public AnnotationInstance(String name, String value, String text, Range range, Element line, Node node, int matchType) {
    super();
    this.name = name;
    this.value = value;
    this.text = text;
    this.range = range;
    this.line = line;
    this.matchType = matchType;
    this.node = node;
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
    return text;
  }
  
  @JsProperty
  public Range getRange() {
    return range;
  }
  
  @JsProperty
  public Element getLine() {
    return line;
  }
  
  @JsProperty
  public Node getNode() {
    return node;
  }
  
  @JsProperty
  public int getMatchType() {
    return matchType;
  }
  
  
}
