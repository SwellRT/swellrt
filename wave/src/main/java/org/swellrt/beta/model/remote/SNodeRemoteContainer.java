package org.swellrt.beta.model.remote;

import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SHandler;
import org.swellrt.beta.model.SObservable;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;

public abstract class SNodeRemoteContainer implements SNodeRemote, SObservable {
   
  public static SNodeRemoteContainer Void = new SNodeRemoteContainer() {
    
    @Override
    protected void clearCache() {
      // Nothing to do      
    }
  };
  
  protected SNodeRemoteContainer parent = null;
  protected boolean eventsEnabled = false;
  
  private final CopyOnWriteSet<SHandler> eventHandlerSet = CopyOnWriteSet.<SHandler>createHashSet();
  
  /** 
   * A lazy method to set the parent of this node
   * @param parent the parent node 
   */
  protected void attach(SNodeRemoteContainer parent) {
    this.parent = parent;
    this.eventHandlerSet.clear();
    this.eventsEnabled = true;
  }
  
  /** 
   * Notify this node is not longer part of an object.<p>
   * This is a recursive in-depth process. 
   */
  protected void deattach() {
    this.eventsEnabled = false;
    this.eventHandlerSet.clear();
    this.parent  = null;
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

    if (propagate && this.parent != null && !this.parent.equals(Void)) {
      this.parent.triggerEvent(e);
    }
      
  }
  
  /**
   * Only For testing. Disable any event generation nor propagation
   * from this node upwards.
   * @param enabled
   */
  protected void enableEvents(boolean enabled) {
    
    this.eventsEnabled = enabled;
    if (this.parent != null && !this.parent.equals(Void))
      this.parent.enableEvents(enabled);
  }
}
