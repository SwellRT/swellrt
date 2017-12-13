package org.swellrt.beta.model.local;

import java.util.ArrayList;
import java.util.List;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SHandlerFunc;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.js.HasJsProxy;
import org.swellrt.beta.model.js.Proxy;
import org.swellrt.beta.model.js.SListProxyHandler;

import jsinterop.annotations.JsIgnore;

public class SListLocal implements SList<SNode>, HasJsProxy {

  private List<SNode> list = new ArrayList<SNode>();
  private Proxy proxy = null;

  @Override
  public SNode pick(int index) throws SException {
    return list.get(index);
  }


  @Override
  public SList<SNode> add(Object object) throws SException {
    SNode node = SUtils.castToSNode(object);
    list.add(node);
    return this;
  }

  @Override
  public SList<SNode> addAt(Object object, int index) throws SException {
    SNode node = SUtils.castToSNode(object);
    list.add(index, node);
    return this;
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


  public Object js() {
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

  @SuppressWarnings({ "rawtypes", "unchecked" })
  @JsIgnore
  @Override
  public void accept(SVisitor visitor) {
    visitor.visit(this);
  }

  //
  // -----------------------------------------------------
  //

  @Override
  public void set(String path, Object value) {
    SNode.set(this, path, value);
  }

  @Override
  public void push(String path, Object value) {
    SNode.push(this, path, value);
  }

  @Override
  public Object pop(String path) {
    return SNode.pop(this, path);
  }

  @Override
  public int length(String path) {
    return SNode.length(this, path);
  }

  @Override
  public boolean contains(String path, String property) {
    return SNode.contains(this, path, property);
  }

  @Override
  public void delete(String path) {
    SNode.delete(this, path);
  }

  @Override
  public Object get(String path) {
    return SNode.get(this, path);
  }

  @Override
  public SNode node(String path) throws SException {
    return SNode.node(this, path);
  }

  @Override
  public SMap asMap() {
    throw new IllegalStateException("Node is not a map");
  }

  @Override
  public SList<? extends SNode> asList() {
    return this;
  }

  @Override
  public String asString() {
    throw new IllegalStateException("Node is not a string");
  }

  @Override
  public double asDouble() {
    throw new IllegalStateException("Node is not a number");
  }

  @Override
  public int asInt() {
    throw new IllegalStateException("Node is not a number");
  }

  @Override
  public boolean asBoolean() {
    throw new IllegalStateException("Node is not a boolean");
  }

  @Override
  public SText asText() {
    throw new IllegalStateException("Node is not a text");
  }

  @Override
  public void addListener(SHandlerFunc h, String path) throws SException {
    throw new IllegalStateException("Local nodes don't support event listeners");
  }

  @Override
  public void listen(SHandlerFunc h) throws SException {
    throw new IllegalStateException("Local nodes don't support event listeners");
  }

  @Override
  public void removeListener(SHandlerFunc h, String path) throws SException {
    throw new IllegalStateException("Local nodes don't support event listeners");
  }

  @Override
  public void unlisten(SHandlerFunc h) throws SException {
    throw new IllegalStateException("Local nodes don't support event listeners");
  }

}
