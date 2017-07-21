package org.swellrt.beta.model.wave.unmutable;

import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.WaveCommons;

public class UnmutableObject extends UnmutableMap {


  public static UnmutableObject materialize(UnmutableWaveNodeManager nodeManager) {

    UnmutableMap root = nodeManager.materializeMap(
        SubstrateId.ofMap(nodeManager.getMasterWaveletId(), WaveCommons.ROOT_SUBSTRATE_ID));

    return new UnmutableObject(root, nodeManager);
  }

  private final UnmutableWaveNodeManager nodeManager;

  protected UnmutableObject(UnmutableMap root, UnmutableWaveNodeManager nodeManager) {
    super(root.map);
    this.nodeManager = nodeManager;
  }

}
