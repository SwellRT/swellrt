package org.waveprotocol.mod.model.generic;

import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;
import org.waveprotocol.wave.model.wave.WaveletListener;

import java.util.Collections;
import java.util.Map;
import java.util.Set;


public class Model implements SourcesEvents<Model.Listener> {


  public interface Listener {

    void onAddParticipant(ParticipantId participant);

    void onRemoveParticipant(ParticipantId participant);

  }

  public static final String WAVELET_ID_PREFIX = "generic";
  public static final String WAVELET_ID = WAVELET_ID_PREFIX + IdUtil.TOKEN_SEPARATOR + "root";

  // The root document
  private static final String ROOT_DOC_ID = "model+root";

  // String Index is stored in the root Document
  private static final String STRING_INDEX_TAG = "s-index";
  private static final String STRING_ITEM_TAG = "s";
  private static final String STRING_VALUE_ATTR = "v";

  // The top level (root) map is stored in the root Document
  private static final String ROOT_MAP_TAG = "root-map";

  // Generic root tag for all data documents.
  private static final String ROOT_DATA_TAG = "data";

  private final TypeSerializer typeSerializer;



  private static final Factory<Doc.E, ObservableBasicValue<String>, String> StringIndexFactory =
      new Factory<Doc.E, ObservableBasicValue<String>, String>() {

        @Override
        public ObservableBasicValue<String> adapt(DocumentEventRouter<? super E, E, ?> router,
            E element) {
          return DocumentBasedBasicValue.create(router, element, Serializer.STRING,
              STRING_VALUE_ATTR);
        }

        @Override
        public Initializer createInitializer(final String initialState) {

          return new Initializer() {

            @Override
            public void initialize(Map<String, String> target) {
              target.put(STRING_VALUE_ATTR, initialState);
            }

          };
        }
      };


  private final ObservableDocument rootModelDocument;
  private final Doc.E rootMapElement;
  private final ObservableElementList<ObservableBasicValue<String>, String> stringIndex;
  private final ObservableWavelet wavelet;
  private final TypeIdGenerator idGenerator;
  private MapType rootMap = null;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  public static Model create(WaveContext wave, String domain, ParticipantId loggedInUser,
      boolean isNewWave, IdGenerator idGenerator) {

    WaveletId waveletId = WaveletId.of(domain, WAVELET_ID);
    ObservableWavelet wavelet = wave.getWave().getWavelet(waveletId);

    if (wavelet == null) {
      wavelet = wave.getWave().createWavelet(waveletId);
      wavelet.addParticipant(loggedInUser);
    }

    // Set up the Root document
    ObservableDocument modelDocument = wavelet.getDocument(ROOT_DOC_ID);
    DocEventRouter router = DefaultDocEventRouter.create(modelDocument);

    // Create a String Index Tag in the Root Document
    Doc.E strIndexElement = DocHelper.getElementWithTagName(modelDocument, STRING_INDEX_TAG);
    if (strIndexElement == null) {
      strIndexElement =
          modelDocument.createChildElement(modelDocument.getDocumentElement(), STRING_INDEX_TAG,
              Collections.<String, String> emptyMap());
    }

    // Create a Tag for the Root Map in the Root Document
    Doc.E rootMapElement = DocHelper.getElementWithTagName(modelDocument, ROOT_MAP_TAG);
    if (rootMapElement == null) {
      rootMapElement =
          modelDocument.createChildElement(modelDocument.getDocumentElement(), ROOT_MAP_TAG,
              Collections.<String, String> emptyMap());
    }


    return new Model(wavelet, TypeIdGenerator.get(idGenerator), DocumentBasedElementList.create(
        router, strIndexElement,
        STRING_ITEM_TAG, StringIndexFactory), modelDocument, rootMapElement);



  }


  protected Model(ObservableWavelet wavelet, TypeIdGenerator idGenerator,
      ObservableElementList<ObservableBasicValue<String>, String> stringIndex,
      ObservableDocument modelDocument, Doc.E rootMapElement) {

    this.wavelet = wavelet;
    this.wavelet.addListener(waveletListener);

    this.idGenerator = idGenerator;
    this.stringIndex = stringIndex;
    this.rootModelDocument = modelDocument;
    this.rootMapElement = rootMapElement;
    this.typeSerializer = new TypeSerializer(this);
  }

  protected ObservableElementList<ObservableBasicValue<String>, String> getStringIndex() {
    return stringIndex;
  }


  //
  // Manage instances attachment to model
  //

  protected void attach(Type instance, String documentId) {

    Preconditions.checkArgument(wavelet.getDocumentIds().contains(documentId),
        "Model.attach() the documentId doesn't math an actual Document");

    ObservableDocument document = wavelet.getDocument(documentId);

    instance.attachToParent(documentId, document, document.getDocumentElement());
  }

  protected void attach(Type instance) {

    if (instance instanceof StringType) {

      // Strings are attached to the String Index structure in model+root
      // document
      StringType str = (StringType) instance;

      int indexStringPos = stringIndex.size();
      ObservableBasicValue<String> observableValue =
          stringIndex.add(indexStringPos, str.getValue());

      instance.attachToString(indexStringPos, observableValue);

    } else {

      // Other Types are attached to separated documents

      String docId = idGenerator.newDocumentId(instance.getPrefix());
      Preconditions.checkArgument(!wavelet.getDocumentIds().contains(docId),
          "Trying to create an existing substrate document");
      ObservableDocument doc = wavelet.getDocument(docId);

      // Create a root tag to ensure the document is persisted.
      // If the doc is created empty and it's not populated with data it won't exist when the
      // wavelet is open again.
      Doc.E rootDataElement = DocHelper.getElementWithTagName(doc, ROOT_DATA_TAG);
      if (rootDataElement == null)
        rootDataElement =
            doc.createChildElement(doc.getDocumentElement(), ROOT_DATA_TAG,
                Collections.<String, String> emptyMap());


      instance.attachToParent(docId, doc, rootDataElement);

    }
  }

  protected TypeSerializer getTypeSerializer() {
    return typeSerializer;
  }


  //
  // Listeners
  //

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }


  @Override
  public void removeListener(Listener listener) {
    listeners.add(listener);
  }

  //
  // Public operations
  //

  public Set<ParticipantId> getParticipants() {
    return wavelet.getParticipantIds();
  }

  public void addParticipant(String address) {
    wavelet.addParticipant(ParticipantId.ofUnsafe(address));
  }

  public void removeParticipant(String address) {
    wavelet.removeParticipant(ParticipantId.ofUnsafe(address));
  }

  public MapType getRoot() {

    // Delayed initialization of the Root Map
    if (rootMap == null) {
      rootMap = new MapType(this);
      rootMap.attachToParent(ROOT_DOC_ID, rootModelDocument, rootMapElement);
    }

    return rootMap;
  }


  public MapType createMap() {
    return new MapType(this);
  }

  public StringType createString(String value) {
    return new StringType(this, value);
  }

  public ListType createList() {
    return new ListType(this);
  }

  //
  // Wavelet Listener
  //

  private final WaveletListener waveletListener = new WaveletListener() {

    @Override
    public void onParticipantRemoved(ObservableWavelet wavelet, ParticipantId participant) {
      for (Listener l : listeners)
        l.onRemoveParticipant(participant);
    }

    @Override
    public void onParticipantAdded(ObservableWavelet wavelet, ParticipantId participant) {
      for (Listener l : listeners)
        l.onAddParticipant(participant);
    }

    @Override
    public void onLastModifiedTimeChanged(ObservableWavelet wavelet, long oldTime, long newTime) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipAdded(ObservableWavelet wavelet, Blip blip) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipRemoved(ObservableWavelet wavelet, Blip blip) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipSubmitted(ObservableWavelet wavelet, Blip blip) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipTimestampModified(ObservableWavelet wavelet, Blip blip, long oldTime,
        long newTime) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipVersionModified(ObservableWavelet wavelet, Blip blip, Long oldVersion,
        Long newVersion) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipContributorAdded(ObservableWavelet wavelet, Blip blip,
        ParticipantId contributor) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onBlipContributorRemoved(ObservableWavelet wavelet, Blip blip,
        ParticipantId contributor) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onVersionChanged(ObservableWavelet wavelet, long oldVersion, long newVersion) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onHashedVersionChanged(ObservableWavelet wavelet, HashedVersion oldHashedVersion,
        HashedVersion newHashedVersion) {
      // TODO Auto-generated method stub

    }

    @Override
    public void onRemoteBlipContentModified(ObservableWavelet wavelet, Blip blip) {
      // TODO Auto-generated method stub

    }


  };

}
