package org.swellrt.beta.model.wave;

import org.swellrt.beta.client.PlatformBasedFactory;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SHandlerFunc;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SObservableNode;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SVisitor;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "ListenableNode")
public abstract class SWaveNodeContainer extends SWaveNode implements SObservableNode {

  @JsIgnore
  public static SWaveNodeContainer Void = new SWaveNodeContainer() {

    @Override
    protected void clearCache() {
      // Nothing to do
    }

    @SuppressWarnings("rawtypes")
    @JsIgnore
    @Override
    public void accept(SVisitor visitor) {
    }

    @Override
    public void set(String path, Object value) {
      // TODO Auto-generated method stub

    }

    @Override
    public Object get(String path) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void push(String path, Object value, @JsOptional Object index) {
      // TODO Auto-generated method stub

    }

    @Override
    public Object pop(String path) {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void delete(String path) {
      // TODO Auto-generated method stub

    }

    @Override
    public int length(String path) {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public boolean contains(String path, String property) {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public void addListener(SHandlerFunc h, String path) throws SException {
      // TODO Auto-generated method stub

    }

    @Override
    public void removeListener(SHandlerFunc h, String path) {
      // TODO Auto-generated method stub

    }

    @Override
    public SNode node(String path) {
      // TODO Auto-generated method stub
      return null;
    }

  };

  protected boolean eventsEnabled = false;

  private final CopyOnWriteSet<SHandlerFunc> eventHandlerSet = CopyOnWriteSet
      .<SHandlerFunc> createHashSet();

  protected SWaveNodeContainer() {
    super(null, null);
  }

  protected SWaveNodeContainer(SubstrateId substrateId, SWaveNodeManager nodeManager) {
    super(substrateId, nodeManager);
  }

  @Override
  protected void attach(SWaveNodeContainer parent) {
    super.attach(parent);
    this.eventHandlerSet.clear();
    this.eventsEnabled = true;
  }

  @Override
  protected void deattach() {
    this.eventsEnabled = false;
    this.eventHandlerSet.clear();
    super.deattach();
  }

  /**
   * Cleans the local cache recursively to mock
   * remote behavior in tests.
   */
  protected abstract void clearCache();


  protected SWaveNodeContainer lookUpListenableNode(String path) throws SException {
    SNode node = null;
    if (path != null)
      node = PlatformBasedFactory.getPathNodeExtractor().getNode(path, this);
    else
      node = this;

    SWaveNodeContainer targetNode = null;

    if (node instanceof SPrimitive) {
      SPrimitive primitiveNode = (SPrimitive) node;

      if (primitiveNode.getContainer() == null) {
        targetNode = primitiveNode.getParent();
      } else {
        targetNode = primitiveNode.getContainer().getParent();
      }

    } else if (node instanceof SWaveNodeContainer) {
      targetNode = (SWaveNodeContainer) node;
    }

    return targetNode;
  }

  @Override
  public void addListener(SHandlerFunc h, String path) throws SException {

    SWaveNodeContainer targetNode = lookUpListenableNode(path);
    if (targetNode != null)
      targetNode.eventHandlerSet.add(h);
    else
      throw new SException(SException.OPERATION_EXCEPTION, null, "Node not found at " + path);

  }


  @Override
  public void removeListener(SHandlerFunc h, String path) throws SException {

    SWaveNodeContainer targetNode = lookUpListenableNode(path);
    if (targetNode != null)
      eventHandlerSet.remove(h);
    else
      throw new SException(SException.OPERATION_EXCEPTION, null, "Node not found at " + path);

  }



  protected void triggerEvent(SEvent e) {

    if (!this.eventsEnabled)
      return;

    boolean propagate = true;

    for (SHandlerFunc h : eventHandlerSet) {
      propagate = h.exec(e);
    }

    if (propagate && this.getParent() != null && !this.getParent().equals(Void)) {
      this.getParent().triggerEvent(e);
    }

  }

  /**
   * Only For testing. Disable any event generation nor propagation
   * from this node upwards.
   * @param enabled
   */
  protected void enableEvents(boolean enabled) {

    this.eventsEnabled = enabled;
    if (this.getParent() != null && !this.getParent().equals(Void))
      this.getParent().enableEvents(enabled);
  }
}
