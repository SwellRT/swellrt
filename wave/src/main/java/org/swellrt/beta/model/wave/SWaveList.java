package org.swellrt.beta.model.wave;

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

public class SWaveList extends SWaveNodeContainer implements SList<SWaveNode>, HasJsProxy, ObservableElementList.Listener<SWaveNode> {

  public static SWaveList create(SWaveNodeManager nodeManager, SubstrateId substrateId, ObservableElementList<SWaveNode, SWaveNode> list) {
    return new SWaveList(nodeManager, substrateId, list);
  }

  private final ObservableElementList<SWaveNode, SWaveNode> list;

  private Proxy proxy;


  protected SWaveList(SWaveNodeManager nodeManager, SubstrateId substrateId, ObservableElementList<SWaveNode, SWaveNode> list) {
    super(substrateId, nodeManager);
    this.list = list;
    this.list.addListener(this);
  }

  @Override
  public SNode pick(int index) throws SException {
    try {
      SWaveNode node = list.get(index);

      // This should be always true!
      if (node instanceof SWaveNode)
        node.attach(this); // lazily set parent

      return node;
    } catch (IndexOutOfBoundsException e) {
      throw new SException(SException.DATA_ERROR, e);
    } catch (Exception e) {
      throw new SException(SException.DATA_ERROR, e);
    }
  }

  @Override
  public SList<SWaveNode> add(SNode value) throws SException {
    check();
    SWaveNode remoteValue = getNodeManager().transformToWaveNode(value, this);
    this.list.add(remoteValue);
    return this;
  }


  @Override
  public SList<SWaveNode> add(Object value) throws SException {
    SNode node = SUtils.castToSNode(value);
    return add(node);
  }

  @Override
  public SList<SWaveNode> add(SNode value, int index) throws SException {
    check();
    if (index >= 0 && index <= this.list.size()) {
      SWaveNode remoteValue = getNodeManager().transformToWaveNode(value, this);
      this.list.add(index, remoteValue);
    } else {
      throw new SException(SException.OUT_OF_BOUNDS_INDEX);
    }
    return this;
  }

  @Override
  public SList<SWaveNode> add(Object value, @JsOptional Object index) throws SException {
    SNode node = SUtils.castToSNode(value);
    if (index != null) {
      return add(node, (int) index);
    } else {
      return add(node);
    }
  }

  @Override
  public SList<SWaveNode> remove(int index) throws SException {
    check();
    SWaveNode node = (SWaveNode) pick(index);
    getNodeManager().checkWritable(node);

    if (node instanceof SWaveNodeContainer) {
      SWaveNodeContainer nrc = (SWaveNodeContainer) node;
      nrc.deattach();
    }

    this.list.remove(node);
    getNodeManager().deleteNode(node);

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

    getNodeManager().check();
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
  public void onValueAdded(SWaveNode entry) {
    try {
      check(); // Ignore events if state is inconsistent
      SEvent e = new SEvent(SEvent.ADDED_VALUE, this, ""+list.indexOf(entry), entry);
      triggerEvent(e);
    } catch (SException e) {
      // Swallow it
    }
  }

  @Override
  public void onValueRemoved(SWaveNode entry) {
    try {
      check(); // Ignore events if state is inconsistent
      SEvent e = new SEvent(SEvent.REMOVED_VALUE, this, ""+list.indexOf(entry), entry);
      triggerEvent(e);
    } catch (SException e) {
      // Swallow it
    }
  }

  @Override
  public Iterable<SWaveNode> values() {
    return list.getValues();
  }

  @Override
  public String toString() {
    return "SMListRemote ["+getSubstrateId()+"]";
  }

  @Override
  public int indexOf(SWaveNode node) throws SException {
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

  @Override
  public SNode node(String path) throws SException {
    return SNode.node(this, path);
  }

}
