package org.swellrt.beta.model.wave.mutable;

import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.wave.SubstrateId;
import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * Wave-based nodes are supported by a wavelet blip substrate and they belong to
 * another wave-based node.
 * <p>
 * This class provides shared properties to them, except if they are primitive
 * values or they are the own object.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public abstract class SWaveNode implements SNode {

  /** the substrate id (wavelet/document id pair) */
  private final SubstrateId substrateId;

  /** the parent node */
  private SWaveNodeContainer parent = null;

  /** the node manager */
  SWaveNodeManager nodeManager;

  protected SWaveNode() {
    this.nodeManager = null;
    this.substrateId = null;
  }

  protected SWaveNode(SubstrateId substrateId, SWaveNodeManager nodeManager) {
    this.nodeManager = nodeManager;
    this.substrateId = substrateId;
  }

  public SubstrateId getSubstrateId() {
    return substrateId;
  }

  public SWaveNodeManager getNodeManager() {
    return nodeManager;
  }

  /**
   * A lazy method to set the parent of this node
   * @param parent the parent node
   */
  protected void attach(SWaveNodeContainer parent) {
    this.parent = parent;
  }

  /**
   * Notify this node is not longer part of an object.<p>
   */
  protected void deattach() {
    this.parent  = null;
  }

  public SWaveNodeContainer getParent() {
    return this.parent;
  }

  public boolean isAttached() {
    return this.parent != null;
  }

  public HashedVersion getLastVersion() {
    return getNodeManager().getWaveSubstrateVersion(this);
  }
}
