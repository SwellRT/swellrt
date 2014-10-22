package org.waveprotocol.mod.model.p2pvalue.docbased;

import org.waveprotocol.mod.model.p2pvalue.ModelIndex;
import org.waveprotocol.mod.model.p2pvalue.id.IdGeneratorCommunity;
import org.waveprotocol.wave.model.adt.ObservableBasicSet;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicSet;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.ObservableWavelet;

import java.util.Collections;

public class DocBasedModelIndex implements ModelIndex {



  public static final String DOC_ID_PREFIX = "index";
  public static final String DOC_ID = DOC_ID_PREFIX;


  // Doc top tag <index ...>
  public static final String TOP_TAG = "index";


  // Active documents <docs>
  private static final String DOCUMENTS_TAG = "documents";

  private static final String DOC_TAG = "doc";
  private static final String DOC_ID_ATTR = "id";

  private final ObservableBasicSet<String> documents;
  private final ObservableBasicSet.Listener<String> docsListener;


  // Listeners
  private final CopyOnWriteSet<ModelIndex.Listener> listeners = CopyOnWriteSet.create();

  private IdGeneratorCommunity idGenerator;
  private ObservableWavelet wavelet;

  /**
   * Creates a DocBasedModelIndex instance backed by a Document. The top element
   * must already exist.
   * 
   * @param router Document
   * @param top the parent Element of the Project inside the document
   * @return a DocBasedProject instance
   */
  private static <E> DocBasedModelIndex create(DocumentEventRouter<? super E, E, ?> router,
      E indexElement, E docsElement) {

    Preconditions.checkArgument(router.getDocument().getTagName(indexElement).equals(TOP_TAG),
        "Invalid Model index top tag %s", router.getDocument().getTagName(indexElement));

    return new DocBasedModelIndex(DocumentBasedBasicSet.create(router, docsElement,
        Serializer.STRING, DOC_TAG, DOC_ID_ATTR));
  }


  /**
   * Create or adapt a DocBasedModelIndex backed by the provided Document.
   * 
   * @param doc Document supporting the Project
   * @return
   */
  public static <E> DocBasedModelIndex create(ObservableDocument doc) {

    DocEventRouter router = DefaultDocEventRouter.create(doc);

    // <index>
    Doc.E projectElement = DocHelper.getElementWithTagName(doc, TOP_TAG);
    if (projectElement == null) {
      doc.createChildElement(doc.getDocumentElement(), TOP_TAG,
          Collections.<String, String> emptyMap());
    }

    // <documents>
    Doc.E documentsElement = DocHelper.getElementWithTagName(doc, DOCUMENTS_TAG);
    if (documentsElement == null) {
      doc.createChildElement(projectElement, DOCUMENTS_TAG,
          Collections.<String, String> emptyMap());
    }

    return create(router, projectElement, documentsElement);

  }


  // Constructor


  DocBasedModelIndex(ObservableBasicSet<String> documents) {

    this.documents = documents;

    this.docsListener = new ObservableBasicSet.Listener<String>() {

      @Override
      public void onValueAdded(String newValue) {
        for (ModelIndex.Listener l: listeners) {
          l.onDocumentAdded(newValue);
        }
      }

      @Override
      public void onValueRemoved(String oldValue) {
        for (ModelIndex.Listener l : listeners) {
          l.onDocumentRemoved(oldValue);
        }
      }
    };

  }

  public void initialize(ObservableWavelet wavelet, IdGeneratorCommunity idGenerator) {
    this.wavelet = wavelet;
    this.idGenerator = idGenerator;
  }



  @Override
  public Pair<String, ObservableDocument> createDocument(String prefix) {

    String newDocId = idGenerator.newDocumentId(prefix);
    ObservableDocument newDoc = wavelet.getDocument(newDocId);

    documents.add(newDocId);

    return new Pair<String, ObservableDocument>(newDocId, newDoc);
  }




  @Override
  public void removeDocument(String id) {

    documents.remove(id);

  }


  @Override
  public ObservableDocument getDocument(String id) {

    if (!documents.contains(id)) return null;

    return wavelet.getDocument(id);

  }


  @Override
  public void traverseDocuments(Action action) {

    for (String docId : documents.getValues()) {
      action.execute(docId);
    }

  }


  @Override
  public ObservableDocument createDocumentWithId(String newDocId) {

    if (documents.contains(newDocId)) return null;

    ObservableDocument newDoc = wavelet.getDocument(newDocId);

    documents.add(newDocId);

    return newDoc;
  }


  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }


  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }
}
