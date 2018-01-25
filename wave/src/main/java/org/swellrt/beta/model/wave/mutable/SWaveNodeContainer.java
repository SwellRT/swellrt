package org.swellrt.beta.model.wave.mutable;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.PathNavigator;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SMutationHandler;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SNodeLocator;
import org.swellrt.beta.model.SObservableNode;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.wave.SubstrateId;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

import jsinterop.annotations.JsIgnore;
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
    }

    @Override
    public Object get(String path) {
      return null;
    }

    @Override
    public void push(String path, Object value) {
    }

    @Override
    public Object pop(String path) {
      return null;
    }

    @Override
    public void delete(String path) {
    }

    @Override
    public int length(String path) {
      return 0;
    }

    @Override
    public boolean contains(String path, String property) {
      return false;
    }

    @Override
    public void addListener(SMutationHandler h, String path) throws SException {

    }

    @Override
    public void removeListener(SMutationHandler h, String path) {


    }

    @Override
    public SNode node(String path) {

      return null;
    }

    @Override
    public SMap asMap() {
      return null;
    }

    @Override
    public SList<SNode> asList() {
      return null;
    }

    @Override
    public String asString() {
      return null;
    }

    @Override
    public double asDouble() {
      return 0;
    }

    @Override
    public int asInt() {
      return 0;
    }

    @Override
    public boolean asBoolean() {
      return false;
    }

    @Override
    public void listen(SMutationHandler h) {
    }

    @Override
    public void unlisten(SMutationHandler h) {
    }

    @Override
    public SText asText() {
      // TODO Auto-generated method stub
      return null;
    }

  };

  protected boolean eventsEnabled = false;

  private final CopyOnWriteSet<SMutationHandler> eventHandlerSet = CopyOnWriteSet
      .<SMutationHandler> createHashSet();

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
    if (path != null) {
      node = SNodeLocator.locate(this, new PathNavigator(path)).node;
    } else
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
  public void addListener(SMutationHandler h, String path) throws SException {

    if (h == null)
      throw new SException(SException.OPERATION_EXCEPTION, null, "Handler is null");

    SWaveNodeContainer targetNode = lookUpListenableNode(path);
    if (targetNode != null)
      targetNode.eventHandlerSet.add(h);
    else
      throw new SException(SException.OPERATION_EXCEPTION, null, "Node not found at " + path);

  }

  @Override
  public void listen(SMutationHandler h) throws SException {
    if (h == null)
      throw new SException(SException.OPERATION_EXCEPTION, null, "Handler is null");

    this.eventHandlerSet.add(h);
  }

  @Override
  public void removeListener(SMutationHandler h, String path) throws SException {

    if (h == null)
      throw new SException(SException.OPERATION_EXCEPTION, null, "Handler is null");

    SWaveNodeContainer targetNode = lookUpListenableNode(path);
    if (targetNode != null)
      eventHandlerSet.remove(h);
    else
      throw new SException(SException.OPERATION_EXCEPTION, null, "Node not found at " + path);

  }

  @Override
  public void unlisten(SMutationHandler h) throws SException {

    if (h == null)
      throw new SException(SException.OPERATION_EXCEPTION, null, "Handler is null");

    eventHandlerSet.remove(h);
  }


  protected void triggerEvent(SEvent e) {

    if (!this.eventsEnabled)
      return;

    boolean propagate = true;

    for (SMutationHandler h : eventHandlerSet) {
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
