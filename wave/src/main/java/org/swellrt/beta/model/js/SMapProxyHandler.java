package org.swellrt.beta.model.js;


import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.IllegalCastException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SUtils;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt")
public class SMapProxyHandler extends ProxyHandler {

  @JsIgnore
  public SMapProxyHandler() {
  }
  
  public Object get(SMap target, String property, ProxyHandler reciever) throws SException {
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
    target.put(property, SUtils.castToPrimitive(value));
    return true;
  }
  
}
