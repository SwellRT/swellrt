package org.swellrt.beta.model.remote;

import org.swellrt.beta.model.SNode;

/**
 * Remote nodes are supported by a substrate and they belong
 * to an remote object.
 * <p>
 * This class provides shared properties to them, except if they
 * are primitive values or they are the own object.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public abstract class SNodeRemote implements SNode {

  
  /** the substrate id (wavelet/document id pair) */
  private final SubstrateId substrateId;
  
  /** the parent node */
  private SNodeRemoteContainer parent = null;
  
  /** the root object */
  private final SObjectRemote object;
  
  protected SNodeRemote() {
    this.object = null;
    this.substrateId = null;
  }
  
  protected SNodeRemote(SubstrateId substrateId, SObjectRemote object) {
    this.object = object;
    this.substrateId = substrateId;
  }
  
  protected SubstrateId getSubstrateId() {
    return substrateId;
  }
  
  protected SObjectRemote getObject() {
    return object;
  }
  
  /** 
   * A lazy method to set the parent of this node
   * @param parent the parent node 
   */
  protected void attach(SNodeRemoteContainer parent) {
    this.parent = parent;
  }
  
  /** 
   * Notify this node is not longer part of an object.<p>
   */
  protected void deattach() {
    this.parent  = null;
  }
  
  protected SNodeRemoteContainer getParent() {
    return this.parent;
  }
}
