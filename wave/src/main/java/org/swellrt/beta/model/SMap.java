package org.swellrt.beta.model;


import org.swellrt.beta.model.local.SMapLocal;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "Map")
public interface SMap extends SNode {
 
  public static SMap create(@JsOptional Object data) {
    return new SMapLocal();
  } 
    
  public Object get(String key);
  
  public SNode getNode(String key);
  
  @JsIgnore
  public SMap put(String key, SNode value);
  
  public SMap put(String key, Object object) throws IllegalCastException;
  
  public void remove(String key);
  
  public boolean has(String key);
  
  public String[] keys();
  
  public void clear();

  boolean isEmpty();
  
  int size();
}
