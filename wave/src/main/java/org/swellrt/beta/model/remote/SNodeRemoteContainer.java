package org.swellrt.beta.model.remote;

import org.swellrt.beta.client.PlatformBasedFactory;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SHandler;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SObservable;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SVisitor;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "ListenableNode")
public abstract class SNodeRemoteContainer extends SNodeRemote implements SObservable {

  @JsIgnore
  public static SNodeRemoteContainer Void = new SNodeRemoteContainer() {

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

  };

  protected boolean eventsEnabled = false;

  private final CopyOnWriteSet<SHandler> eventHandlerSet = CopyOnWriteSet.<SHandler>createHashSet();

  protected SNodeRemoteContainer() {
    super(null, null);
  }

  protected SNodeRemoteContainer(SubstrateId substrateId, SObjectRemote object) {
    super(substrateId, object);
  }

  @Override
  protected void attach(SNodeRemoteContainer parent) {
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


  @Override
  public void addListener(SHandler h, String event, String path) throws SException {
    SNode node = null;
    if (path != null)
      node = PlatformBasedFactory.getPathNodeExtractor().getNode(path, this);
    else
      node = this;

    // !!!!!!!!!!!!!!!!!!!!!!

    if (node instanceof SPrimitive) {
      SPrimitive primitiveNode = ((SPrimitive) node).getContainer();
    }

    eventHandlerSet.add(h);
  }


  @Override
  public void removeListener(SHandler h, String event, String path) {
    eventHandlerSet.remove(h);
  }



  protected void triggerEvent(SEvent e) {

    if (!this.eventsEnabled)
      return;

    boolean propagate = true;

    for (SHandler h: eventHandlerSet) {
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
