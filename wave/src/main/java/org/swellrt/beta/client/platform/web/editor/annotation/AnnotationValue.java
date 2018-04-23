package org.swellrt.beta.client.platform.web.editor.annotation;

import org.waveprotocol.wave.model.document.util.IRange;

import com.google.gwt.dom.client.Node;

import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Annotation")
public class AnnotationValue {

  /** The search range is equals to annotation range or inside it */
  public static final int MATCH_IN = 0;

  /**
   * The search range is partially out of the annotation range or selection
   * spans more content beyond annotation
   */
  public static final int MATCH_OUT = 1;

  public IRange range;
  public String key;
  public Object value;
  public Object newValue;
  public Node node;
  public Node line;
  public String text;
  public int searchMatch;

  public Object data;

}
