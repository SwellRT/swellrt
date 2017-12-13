package org.swellrt.beta.model;


import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.js.SMapJs;
import org.swellrt.beta.model.local.SMapLocal;
import org.swellrt.beta.model.wave.mutable.SWaveNodeContainer;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Map")
public interface SMap extends SNode, SObservableNode {

  public static SMap create(@JsOptional Object data) throws IllegalCastException {

	if (data != null && data instanceof JavaScriptObject)
		return SMapJs.create((JavaScriptObject) data);

    return new SMapLocal();
  }

  @JsIgnore
  public static SMap create() {
    return new SMapLocal();
  }

  /**
   * Returns a container or a primitive value container.
   * @param key
   * @return
   */
  public SNode pick(String key) throws SException;

  @JsIgnore
  public SMap put(String key, SNode value) throws SException;

  public SMap put(String key, Object object) throws SException;

  /**
   * Remove the value referenced by the key, if value is a
   * {@link SWaveNodeContainer} empty its substrate node.
   *
   * @param key
   * @throws SException
   */
  public void remove(String key) throws SException;

  /**
   * Remove the value referenced by the key, if value is a
   * {@link SWaveNodeContainer} keep the substrate node.
   * <p>
   * Use this method to delete nodes which are referenced by other nodes.
   *
   * @param key
   * @throws SException
   */
  public void removeSafe(String key) throws SException;

  public boolean has(String key) throws SException;

  public String[] keys() throws SException;

  public void clear() throws SException;

  boolean isEmpty();

  int size();
}
