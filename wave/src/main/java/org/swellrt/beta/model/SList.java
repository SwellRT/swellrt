package org.swellrt.beta.model;


import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "List")
public interface SList extends SNode {
 
  public static SList create(@JsOptional Object data) throws IllegalCastException {
	/*  
	if (data != null && data instanceof JavaScriptObject)
		return SListJs.create((JavaScriptObject) data);
	  
    return new SListLocal();
    */
    return null;
  } 
    
  /**
   * Returns a container or a primitive value.
   * @param key
   * @return
   */
  public Object get(int index) throws SException;
  
  /**
   * Returns a container or a primitive value container.
   * @param key
   * @return
   */
  public SNode getNode(int index) throws SException;
  
  @JsIgnore
  public SList add(SNode value) throws SException;
  
  public SList add(Object object) throws SException;
  
  public SList remove(int index) throws SException;
  
  public int indexOf(SNode node) throws SException;
  
  public void clear() throws SException;

  public boolean isEmpty();
  
  public int size();
  
  @JsIgnore
  public Iterable<SNode> values();
  
  Object asNative();
}
