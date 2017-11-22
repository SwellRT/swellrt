package org.swellrt.beta.model.wave.mutable;

import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.wave.SubstrateId;
import org.waveprotocol.wave.model.wave.Blip;

/**
 * Abstract base class for remote text types which are platform dependent.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public abstract class SWaveText extends SWaveNode implements SText {

  /**
   * A Blip is the original Wave representation of a Text document.
   * We keep using the Blip type as convenience as long as it matches
   * quite well the interface SwellRT requires.
   * <p>
   * Blip is also platform independent unlike ContentDocument that is a
   * specific wrapper of a Blip for Web rendering.
   */
  private final Blip blip;

  protected SWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId, Blip blip) {
    super(substrateId, nodeManager);
    this.blip = blip;
  }

}
