package org.swellrt.beta.model.remote;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.js.HasJsProxy;
import org.swellrt.beta.model.js.Proxy;
import org.swellrt.beta.model.js.SListProxyHandler;
import org.waveprotocol.wave.model.adt.ObservableElementList;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;

public class SListRemote extends SNodeRemoteContainer implements SList<SNodeRemote>, HasJsProxy, ObservableElementList.Listener<SNodeRemote> {

  public static SListRemote create(SObjectRemote object, SubstrateId substrateId, ObservableElementList<SNodeRemote, SNodeRemote> list) {
    return new SListRemote(object, substrateId, list);
  }

  private final ObservableElementList<SNodeRemote, SNodeRemote> list;

  private Proxy proxy;


  protected SListRemote(SObjectRemote object, SubstrateId substrateId, ObservableElementList<SNodeRemote, SNodeRemote> list) {
    super(substrateId, object);
    this.list = list;
    this.list.addListener(this);
  }

  @Override
  public SNode node(int index) throws SException {
    try {
      SNodeRemote node = list.get(index);

      // This should be always true!
      if (node instanceof SNodeRemote)
        node.attach(this); // lazily set parent

      return node;
    } catch (IndexOutOfBoundsException e) {
      throw new SException(SException.DATA_ERROR, e);
    } catch (Exception e) {
      throw new SException(SException.DATA_ERROR, e);
    }
  }

  @Override
  public SList<SNodeRemote> add(SNode value) throws SException {
    check();
    SNodeRemote remoteValue =  getObject().transformToRemote(value, this, false);
    this.list.add(remoteValue);
    return this;
  }


  @Override
  public SList<SNodeRemote> add(Object value) throws SException {
    SNode node = SUtils.castToSNode(value);
    return add(node);
  }

  @Override
  public SList<SNodeRemote> add(SNode value, int index) throws SException {
    check();
    if (index >= 0 && index <= this.list.size()) {
      SNodeRemote remoteValue =  getObject().transformToRemote(value, this, false);
      this.list.add(index, remoteValue);
    } else {
      throw new SException(SException.OUT_OF_BOUNDS_INDEX);
    }
    return this;
  }

  @Override
  public SList<SNodeRemote> add(Object value, @JsOptional Object index) throws SException {
    SNode node = SUtils.castToSNode(value);
    if (index != null) {
      return add(node, (int) index);
    } else {
      return add(node);
    }
  }

  @Override
  public SList<SNodeRemote> remove(int index) throws SException {
    check();
    SNodeRemote node = (SNodeRemote) node(index);
    getObject().checkWritable(node);

    if (node instanceof SNodeRemoteContainer) {
      SNodeRemoteContainer nrc = (SNodeRemoteContainer) node;
      nrc.deattach();
    }

    this.list.remove(node);
    getObject().deleteNode(node);

    return this;
  }

  @Override
  public void clear() throws SException {
    this.list.clear();
  }

  @Override
  public boolean isEmpty() {
    return this.list.size() == 0;
  }

  @Override
  public int size() {
    return this.list.size();
  }

  public Object js() {
    if (proxy == null)
      proxy = new Proxy(this, new SListProxyHandler());
    return proxy;
  }


  //
  // Node remote container
  //

  @Override
  protected void clearCache() {
    // TODO Auto-generated method stub
  }

  /**
   * Perform a sanity check. Raise an exception if this node
   * can't perform the operation or the container object is
   * in a bad state.
   * <p>
   * Don't use it for read operations to avoid client frameworks
   * (like angular2) receiving exceptions in templates.
   */
  protected void check() throws SException {
    if (this.getParent() == null)
      throw new SException(SException.NOT_ATTACHED_NODE);

    getObject().check();
  }

  //
  // Js Proxies
  //

  @Override
  public void setJsProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  @Override
  public Proxy getJsProxy() {
    return this.proxy;
  }

  //
  // Event handling
  //

  @Override
  public void onValueAdded(SNodeRemote entry) {
    try {
      check(); // Ignore events if state is inconsistent
      SEvent e = new SEvent(SEvent.ADDED_VALUE, this, ""+list.indexOf(entry), entry);
      triggerEvent(e);
    } catch (SException e) {
      // Swallow it
    }
  }

  @Override
  public void onValueRemoved(SNodeRemote entry) {
    try {
      check(); // Ignore events if state is inconsistent
      SEvent e = new SEvent(SEvent.REMOVED_VALUE, this, ""+list.indexOf(entry), entry);
      triggerEvent(e);
    } catch (SException e) {
      // Swallow it
    }
  }

  @Override
  public Iterable<SNodeRemote> values() {
    return list.getValues();
  }

  @Override
  public String toString() {
    return "SMListRemote ["+getSubstrateId()+"]";
  }

  @Override
  public int indexOf(SNodeRemote node) throws SException {
      return this.list.indexOf(node);
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
  public void push(String path, Object value, @JsOptional Object index) {
    SNode.push(this, path, value, index);
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

}
