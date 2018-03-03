package org.swellrt.beta.model.wave.mutable;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.local.SMapLocal;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Single facade to access predefined transient data structures for SwellRT
 * internals.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SWaveTransient {

  /** Id of root map for storing doc carets */
  public static final String CARETS_NODE = "carets";

  private final SMap transientRootMap;

  public SWaveTransient(SMap transientRootMap) {
    this.transientRootMap = transientRootMap;
  }

  protected SMap getRootMap() {
    return transientRootMap;
  }

  public SMap getCaretsForDocument(String documentId) {

    Preconditions.checkArgument(documentId != null && !documentId.isEmpty(),
        "Document id can't be empty");

    try {

      if (!transientRootMap.has(CARETS_NODE)) {
        transientRootMap.put(CARETS_NODE, new SMapLocal());
      }

      // A map referencing text documents
      SMap caretDocIndexMap = transientRootMap.pick(CARETS_NODE).asMap();

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

}
