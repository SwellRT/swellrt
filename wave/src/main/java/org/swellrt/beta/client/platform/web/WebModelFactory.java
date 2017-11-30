package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.platform.web.editor.STextLocalWeb;
import org.swellrt.beta.client.platform.web.editor.STextRemoteWeb;
import org.swellrt.beta.common.ModelFactory;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.local.STextLocal;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.wave.Blip;

public class WebModelFactory extends ModelFactory {

  @Override
  public SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId, Blip blip,
      InteractiveDocument doc) {
    return new STextRemoteWeb(nodeManager, substrateId, blip, doc);

  }

  @Override
  public STextLocal createLocalText(String text) throws SException {
    return STextLocalWeb.create(text);
  }

}
