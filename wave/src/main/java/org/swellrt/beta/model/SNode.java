package org.swellrt.beta.model;

import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "Node")
public interface SNode {
  
  public static SNode readOnly(Object object) throws IllegalCastException {
    SNode node = SUtils.castToSNode(object);
    return new SSuplementNode(node, true, false);
  }
  
  public static SNode unique(@JsOptional Object object) throws IllegalCastException {
    SNode node = null;
    
    if (object != null) {
      node = SUtils.castToSNode(object);
      if (!(node instanceof SPrimitive)) {
        node = null;
      }
    } 

    if (node == null)
      node = new SPrimitive(0);
      
    return new SSuplementNode(node, true, true);
  }
  
}
