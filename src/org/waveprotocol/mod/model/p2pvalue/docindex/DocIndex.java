package org.waveprotocol.mod.model.p2pvalue.docindex;

import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.SourcesEvents;

public interface DocIndex extends SourcesEvents<DocIndex.Listener> {


  public interface Action {

    void execute(String docId);

  }

  void traverseDocuments(Action action);

  ObservableDocument createDocumentWithId(String id);

  Pair<String, ObservableDocument> createDocument(String prefix);

  void removeDocument(String docId);

  ObservableDocument getDocument(String docId);


  public interface Listener {

    void onDocumentAdded(String docId);

    void onDocumentRemoved(String docId);

  }


}
