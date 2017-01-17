package org.swellrt.beta.model.local;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.model.IllegalCastException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.js.HasJsProxy;
import org.swellrt.beta.model.js.Proxy;
import org.swellrt.beta.model.js.SMapProxyHandler;


public class SMapLocal implements SMap, HasJsProxy {

  public SMapLocal() {
    
  }
  
  private Map<String, SNode> map = new HashMap<String, SNode>();
  private Proxy proxy = null;
  
  @Override
  public Object get(String key) {  
    SNode node = map.get(key);
    if (node instanceof SPrimitive)
      return ((SPrimitive) node).get();
    
    return node;
  }

  @Override
  public SMap put(String key, SNode value) {
    map.put(key, value);  
    return this;
  }
  
  @Override
  public SMap put(String key, Object object) throws IllegalCastException {
    SNode node = SUtils.castToSNode(object);
    return put(key, node);
  }

  @Override
  public void remove(String key) {
    map.remove(key);
  }
  
  @Override
  public boolean has(String key) {
    return map.containsKey(key);
  }

  @Override
  public String[] keys() {
    return map.keySet().toArray(new String[map.size()]);
  }
  
  @Override
  public Proxy getJsProxy() {
    return proxy;       
  }
  
  @Override
  public void setJsProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  @Override
  public SNode getNode(String key) {
    SNode n = map.get(key);
    return n;
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public Object asNative() {
    return new Proxy(this, new SMapProxyHandler());
  }
  
}
