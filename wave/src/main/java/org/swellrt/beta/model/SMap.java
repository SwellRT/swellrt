package org.swellrt.beta.model;


import org.swellrt.beta.model.js.SMapJs;
import org.swellrt.beta.model.local.SMapLocal;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "Map")
public interface SMap extends SNode {
 
  public static SMap create(@JsOptional Object data) throws IllegalCastException {
	  
	if (data != null && data instanceof JavaScriptObject)
		return SMapJs.create((JavaScriptObject) data);
	  
    return new SMapLocal();
  } 
    
  /**
   * Returns a container or a primitive value.
   * @param key
   * @return
   */
  public Object get(String key);
  
  /**
   * Returns a container or a primitive value container.
   * @param key
   * @return
   */
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
  
  Object asNative();
}
