package org.swellrt.beta.model.wave.mutable;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.local.SMapLocal;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Single facade to access predefined transient data structures for SwellRT
 * internals.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SWaveTransient {

  private final SMap transientRootMap;

  public SWaveTransient(SMap transientRootMap) {
    this.transientRootMap = transientRootMap;
  }

  public SMap getCaretsForDocument(String documentId) {

    Preconditions.checkArgument(documentId != null && !documentId.isEmpty(),
        "Document id can't be empty");

    try {

      if (!transientRootMap.has(IdConstants.MAP_CARETS)) {
        transientRootMap.put(IdConstants.MAP_CARETS, new SMapLocal());
      }

      // A map referencing text documents
      SMap caretDocIndexMap = transientRootMap.pick(IdConstants.MAP_CARETS).asMap();

      if (!caretDocIndexMap.has(documentId)) {
        caretDocIndexMap.put(documentId, new SMapLocal());
      }

      // The particular map of carets for this text
      SMap caretMap = caretDocIndexMap.pick(documentId).asMap();

      return caretMap;

    } catch (SException e) {
      throw new IllegalStateException(e);
    }

  }

  public SMap getPresenceStatusMap() {

    try {

      if (!transientRootMap.has(IdConstants.MAP_PRESENCE)) {
        transientRootMap.put(IdConstants.MAP_PRESENCE, new SMapLocal());
      }

      return transientRootMap.pick(IdConstants.MAP_PRESENCE).asMap();

    } catch (SException e) {
      throw new IllegalStateException(e);
    }

  }

}
