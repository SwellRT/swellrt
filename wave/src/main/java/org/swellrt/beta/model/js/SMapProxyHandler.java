package org.swellrt.beta.model.js;


import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.remote.SMapRemote;
import org.swellrt.beta.model.remote.SNodeRemoteContainer;
import org.swellrt.beta.model.remote.SObjectRemote;
import org.waveprotocol.wave.client.common.util.JsoView;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * A Javascript proxy for SMap.
 *
 * TODO implement unit test
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(namespace = "swell")
public class SMapProxyHandler extends ProxyHandler {

  private static final String PROP_TARGET = "__target__";
  private static final String PROP_CONTROLLER = "__controller";
  private static final String PROP_CTRL = "__ctrl";
  private static final String PROP_PRIVATE = "__priv";
  private static final String PROP_USER = "__user";

  @JsIgnore
  public SMapProxyHandler() {
  }

  public Object get(SMap target, String property, ProxyHandler reciever) throws SException {

    boolean isRoot = false;
    SObjectRemote object = null;

    if (target instanceof SMapRemote) {
      SMapRemote targetRemote = (SMapRemote) target;
      if (targetRemote.getParent().equals(SNodeRemoteContainer.Void)) {
        isRoot = true;
        object = targetRemote.getObject();
      }
    }

    if (isRoot && property.equals(PROP_TARGET) ||
        property.equals(PROP_CONTROLLER) ||
        property.equals(PROP_CTRL)) {
      return target;
    }

    Object node = null;

    if (isRoot && (property.equals(PROP_PRIVATE) ||
        property.equals(PROP_USER))) {
      node = object.getUserObject();
    } else {
      node = target.get(property);
    }



    if (node instanceof HasJsProxy) {
      Proxy proxy = ((HasJsProxy) node).getJsProxy();

      if (proxy == null) {
        if (node instanceof SMap) {
          proxy = new Proxy((SNode) node, new SMapProxyHandler());
          ((HasJsProxy) node).setJsProxy(proxy);
        }
      }

      return proxy;

    } else {
      return node;
    }
  }

  public boolean set(SMap target, String property, Object value, ProxyHandler reciever) throws SException {
    target.put(property, SUtils.castToSNode(value));
    return true;
  }

  public boolean has(SMap target, String key) throws SException {
    return target.has(key);
  }

  public Object ownKeys(SMap target) throws SException {
    return target.keys();
  }

  public Object getOwnPropertyDescriptor(SMap target, String key) throws SException {

    if (target.has(key)) {
      JsoView propDesc = JsoView.as(JavaScriptObject.createObject());
      propDesc.setObject("value", get(target, key, this));
      propDesc.setBoolean("writable", true);
      propDesc.setBoolean("enumerable", true);
      propDesc.setBoolean("configurable", true);

      return propDesc;
    } else {
      return Reflect.getOwnPropertyDescriptor(target, key);
    }

  }

  public boolean defineProperty(SMap target, String key, JavaScriptObject propDesc) throws SException {
    JsoView propDescView = JsoView.as(propDesc);
    Object object = propDescView.getObjectUnsafe("value");
    if (object != null) {
      target.put(key, object);
      return true;
    } else {
      return false;
    }
  }


  public boolean deleteProperty(SMap target, String key) throws SException {
    if (target.has(key)) {
      target.remove(key);
      return true;
    }
    return false;
  }
}
