package org.waveprotocol.mod.model.p2pvalue.docindex;

import org.waveprotocol.wave.model.document.ObservableDocument;


public interface DocIndexedFactory<T> {

  T adapt(ObservableDocument doc, String docId);

}
