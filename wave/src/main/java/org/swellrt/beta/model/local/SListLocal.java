package org.swellrt.beta.model.local;

import java.util.ArrayList;
import java.util.List;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.js.HasJsProxy;
import org.swellrt.beta.model.js.Proxy;
import org.swellrt.beta.model.js.SListProxyHandler;

import jsinterop.annotations.JsOptional;

public class SListLocal implements SList<SNode>, HasJsProxy {

  private List<SNode> list = new ArrayList<SNode>();
  private Proxy proxy = null;
  
  @Override
  public Object get(int index) throws SException {
    SNode node = list.get(index);
    if (node instanceof SPrimitive)
      return ((SPrimitive) node).get();
    
    return node;
  }

  @Override
  public SNode getNode(int index) throws SException {
    return list.get(index);
  }

  @Override
  public SList<SNode> add(SNode value) throws SException {
    list.add(value);
    return this;
  }

  @Override
  public SList<SNode> add(SNode value, int index) throws SException {
    list.add(index, value);
    return this;
  }
  
  @Override
  public SList<SNode> add(Object object) throws SException {
    SNode node = SUtils.castToSNode(object);
    return add(node);
  }
  
  @Override
  public SList<SNode> add(Object object, @JsOptional Object index) throws SException {
    SNode node = SUtils.castToSNode(object);
    if (index != null) {
      return add(node, (int) index);
    } else {
      return add(node);
    }
  }

  @Override
  public SList<SNode> remove(int index) throws SException {
    list.remove(index);
    return this;
  }

  @Override
  public int indexOf(SNode node) throws SException {
    return list.indexOf(node);
  }

  @Override
  public void clear() throws SException {
    list.clear();
  }

  @Override
  public boolean isEmpty() {
    return list.isEmpty();
  }

  @Override
  public int size() {
    return list.size();
  }

  @Override
  public Iterable<SNode> values() {
    return list;
  }

  @Override
  public Object asNative() {
    if (proxy == null)
      proxy = new Proxy(this, new SListProxyHandler());
    return proxy;
  }
  
  //
  // Js Proxy
  //
  
  @Override
  public Proxy getJsProxy() {
    return proxy;       
  }
  
  @Override
  public void setJsProxy(Proxy proxy) {
    this.proxy = proxy;
  }

}
