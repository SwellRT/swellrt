package org.swellrt.beta.model.remote;

import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SHandler;
import org.swellrt.beta.model.SObservable;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "ListenableNode")
public abstract class SNodeRemoteContainer extends SNodeRemote implements SObservable {
   
  @JsIgnore
  public static SNodeRemoteContainer Void = new SNodeRemoteContainer() {
    
    @Override
    protected void clearCache() {
      // Nothing to do      
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
  public void listen(SHandler h) {
    eventHandlerSet.add(h);
  }
  

  @Override
  public void unlisten(SHandler h) {
    eventHandlerSet.remove(h);
  }
  
  
  protected void triggerEvent(SEvent e) {
    
    if (!this.eventsEnabled) 
      return;
    
    boolean propagate = true;
    
    for (SHandler h: eventHandlerSet) {
      propagate = propagate && h.exec(e);
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
