package org.waveprotocol.mod.model.p2pvalue.docbased;

import org.waveprotocol.mod.model.p2pvalue.Community;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.ObservableSingleton;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedSingleton;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.ObservableWavelet;

import java.util.Map;

/**
 * A Document-based Community
 *
 * @author pablojan@gmail.com
 *
 */
public class DocBasedCommunity implements Community {

  public static final String DOC_ID_PREFIX = "community";
  public static final String DOC_ID = DOC_ID_PREFIX;


  // Doc top tag <community ...>
  public static final String TOP_TAG = "community";

  // Name <community name="...">
  private static final String NAME_ATTR = "name";
  private final ObservableBasicValue<String> name;
  private final ObservableBasicValue.Listener<String> nameListener;

  // Listeners
  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();


  /**
   * A Factory of DocBasedCommunity instances.
   */
  protected static final Factory<Doc.E, DocBasedCommunity, Void> FACTORY =
      new Factory<Doc.E, DocBasedCommunity, Void>() {

        @Override
        public DocBasedCommunity adapt(DocumentEventRouter<? super Doc.E, Doc.E, ?> router,
            Doc.E element) {
          return create(router, element);
        }

        @Override
        public Initializer createInitializer(Void initialState) {
          return new Initializer() {
            @Override
            public void initialize(Map<String, String> target) {
              // Nothing to do
            }
          };
        }


      };


  /**
   * Create a DocBasedCommunity instance backed by a Document.
   *
   * @param router Document
   * @param top Parent Element of the Community inside the document.
   * @return a DocBasedCommunity instance
   */
  private static <E> DocBasedCommunity create(DocumentEventRouter<? super E, E, ?> router, E top) {

    Preconditions.checkArgument(router.getDocument().getTagName(top).equals(TOP_TAG),
        "Invalid Community top tag %s", router.getDocument().getTagName(top));

    return new DocBasedCommunity(DocumentBasedBasicValue.create(router, top, Serializer.STRING,
        NAME_ATTR));
  }

  /**
   * Create or adapt a DocBasedCommunity backed by the provided Wavelet. This
   * method is provided because Community is the root type of the Document. It
   * provides a singleton container <community>...</community>
   *
   *
   * @param wavelet
   * @return
   */
  public static <E> DocBasedCommunity create(ObservableWavelet wavelet) {

    // Retrieve or create
    ObservableDocument doc = wavelet.getDocument(DOC_ID);
    DocEventRouter router = DefaultDocEventRouter.create(doc);

    // All document content is wrapped in a singleton container
    // TODO(pablojan) create the <community> top tag

    ObservableSingleton<DocBasedCommunity, Void> container =
        DocumentBasedSingleton.create(router, doc.getDocumentElement(), TOP_TAG,
            DocBasedCommunity.FACTORY);


    return container.get();
  }


  // Constructor

  DocBasedCommunity(ObservableBasicValue<String> name) {

    this.name = name;
    this.nameListener = new ObservableBasicValue.Listener<String>() {

      @Override
      public void onValueChanged(String oldValue, String newValue) {
        for (Community.Listener l : listeners)
          l.onNameChanged(newValue);
      }
    };

  }

  @Override
  public void addListener(Listener listener) {
    this.listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    this.listeners.remove(listener);
  }

  @Override
  public void setName(String name) {
    this.name.set(name);
  }

  @Override
  public String getName() {
    return this.name.get();
  }


}
