package org.swellrt.beta.model.js;


import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SUtils;
import org.waveprotocol.wave.client.common.util.JsoView;

import com.google.gwt.core.client.JavaScriptObject;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt")
public class SMapProxyHandler extends ProxyHandler {
  
  private static final String PROP_TARGET = "__target__";
  private static final String PROP_CONTROLLER = "__controller";
  private static final String PROP_CTRL = "__ctrl";

  @JsIgnore
  public SMapProxyHandler() {
  }
  
  public Object get(SMap target, String property, ProxyHandler reciever) throws SException {
    
    if (property.equals(PROP_TARGET) || 
        property.equals(PROP_CONTROLLER) ||
        property.equals(PROP_CTRL)) {
      return target;
    }
    
    Object node = target.get(property);

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
