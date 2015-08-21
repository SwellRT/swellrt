package org.waveprotocol.wave.client.concurrencycontrol;

import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.operation.wave.BlipContentOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;

import java.util.HashMap;
import java.util.Map;

/**
 * A registry of operations performed to Wavelets, in order to follow up
 * authors.
 *
 * @author Pablo Ojanguren (pablojan@gmail.com)
 *
 */
public class DocOperationLog {


  Map<DocOp, String> docOpRegistry;

  public DocOperationLog() {
    docOpRegistry = new HashMap<DocOp, String>();
  }

  public void register(String waveletId, WaveletOperation op) {

    if (op instanceof WaveletBlipOperation) {
      // String docId = ((WaveletBlipOperation) op).getBlipId();
      // BlipContentOperation is the only expected blip op type.
      BlipContentOperation blipOp = (BlipContentOperation) ((WaveletBlipOperation) op).getBlipOp();
      docOpRegistry.put(blipOp.getContentOp(), blipOp.getContext().getCreator().getAddress());
    }

  }

  public String getCreatorAndRemove(DocOp docOp) {
    String creator = docOpRegistry.get(docOp);
    if (creator != null) {
      docOpRegistry.remove(docOp);
    }
    return creator;
  }

}
