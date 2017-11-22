package org.swellrt.beta.client.js;

import org.swellrt.beta.client.js.editor.STextLocalWeb;
import org.swellrt.beta.client.js.editor.STextRemoteWeb;
import org.swellrt.beta.common.Platform;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.local.STextLocal;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.wave.Blip;

public class PlatformWeb extends Platform {

  @Override
  public boolean isWeb() {
    return true;
  }

  @Override
  public boolean isJavaScript() {
    return true;
  }

  @Override
  public boolean isJava() {
    return false;
  }

  @Override
  public SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId,
      Blip blip, InteractiveDocument doc) {
    return new STextRemoteWeb(nodeManager, substrateId, blip, doc);
  }

  @Override
  public STextLocal createLocalText(String text) throws SException {
    return STextLocalWeb.create(text);
  }

}
