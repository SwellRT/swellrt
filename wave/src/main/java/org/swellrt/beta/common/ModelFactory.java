package org.swellrt.beta.common;

import org.swellrt.beta.model.local.STextLocal;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.wave.Blip;

public abstract class ModelFactory {

  /** Avoid this global dependency. */
  public static ModelFactory instance = null;

  public abstract SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId,
      Blip blip,
      InteractiveDocument doc);

  public abstract STextLocal createLocalText(String text) throws SException;

}
