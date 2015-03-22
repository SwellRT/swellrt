package org.waveprotocol.mod.model.generic;

import com.google.common.collect.ImmutableMap;

import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Main interface of the dynamic Wave's generic content model. See following
 * model version info:
 * 
 * <p>
 * [version not provided] <br/>
 * The original very buggy implementation. No longer supported.
 * 
 * <p>
 * version 0.1 <br/>
 * 
 * Each <code>Type</code> instance stores values in a new <code>Document</code>.
 * Strings are stored in a separated string index.<br/>
 * 
 * Simplified <code>Type</code> interface, only one <code>attach()</code>
 * method. <br/>
 * 
 * Improved class names to distinguish List and Map inner tools:
 * ListElementFactory...<br/>
 * 
 * The main document is now "map+root", supporting metadata, index of string and
 * the root map.<br/>
 * 
 */
public class Model implements SourcesEvents<Model.Listener> {

  /**
   * The model version of the current source code. In the future, code could
   * support multiple model versions havinf backwards compatibility.
   */
  public static final String MODEL_VERSION = "0.1";

  public interface Listener {

    void onAddParticipant(ParticipantId participant);

    void onRemoveParticipant(ParticipantId participant);

  }

  public static final String WAVELET_ID_PREFIX = "generic";
  public static final String WAVELET_ID = WAVELET_ID_PREFIX + IdUtil.TOKEN_SEPARATOR + "root";

  // The root document
  private static final String ROOT_DOC_ID = "map+root";

  private static final String METADATA_TAG = "model";
  private static final String METADATA_ATTR_VERSION = "v";

  // String Index is stored in the root Document
  private static final String STRING_INDEX_TAG = "strings";
  private static final String STRING_ITEM_TAG = "s";
  private static final String STRING_VALUE_ATTR = "v";



  private final MapSerializer typeSerializer;



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
  private final ObservableElementList<ObservableBasicValue<String>, String> stringIndex;
  private final ObservableWavelet wavelet;
  private final TypeIdGenerator idGenerator;
  private MapType rootMap = null;

  private final Map<String, Document> documents = new HashMap<String, Document>();

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

    // Get or create model version and additional metadata
    Doc.E metadataElement = DocHelper.getElementWithTagName(modelDocument, METADATA_TAG);
    if (metadataElement == null) {
      metadataElement = modelDocument.createChildElement(modelDocument.getDocumentElement(), METADATA_TAG,
              ImmutableMap.of(METADATA_ATTR_VERSION, MODEL_VERSION));
    }

    // Create a String Index Tag in the Root Document
    Doc.E strIndexElement = DocHelper.getElementWithTagName(modelDocument, STRING_INDEX_TAG);
    if (strIndexElement == null) {
      strIndexElement =
          modelDocument.createChildElement(modelDocument.getDocumentElement(), STRING_INDEX_TAG,
              Collections.<String, String> emptyMap());
    }



    return new Model(wavelet, TypeIdGenerator.get(idGenerator), DocumentBasedElementList.create(
        router, strIndexElement, STRING_ITEM_TAG, StringIndexFactory), modelDocument);



  }


  protected Model(ObservableWavelet wavelet, TypeIdGenerator idGenerator,
      ObservableElementList<ObservableBasicValue<String>, String> stringIndex,
      ObservableDocument modelDocument) {

    this.wavelet = wavelet;
    this.wavelet.addListener(waveletListener);

    this.idGenerator = idGenerator;
    this.stringIndex = stringIndex;
    this.rootModelDocument = modelDocument;
    this.typeSerializer = new MapSerializer(this);

    documents.put(ROOT_DOC_ID, rootModelDocument);
  }

  protected ObservableElementList<ObservableBasicValue<String>, String> getStringIndex() {
    return stringIndex;
  }



  protected String generateDocId(String prefix) {
    return idGenerator.newDocumentId(prefix);
  }


  protected ObservableDocument createDocument(String docId) {

    Preconditions.checkArgument(!wavelet.getDocumentIds().contains(docId),
        "Trying to create an existing substrate document");
    ObservableDocument doc = wavelet.getDocument(docId);

    documents.put(docId, doc);

    return doc;
  }

  protected ObservableDocument getDocument(String docId) {

    Preconditions.checkArgument(wavelet.getDocumentIds().contains(docId),
        "Trying to create an existing substrate document");
    ObservableDocument doc = wavelet.getDocument(docId);

    documents.put(docId, doc);

    return doc;
  }


  protected MapSerializer getTypeSerializer() {
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
      rootMap = (MapType) MapType.createAndAttach(this, ROOT_DOC_ID);
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

  /**
   * For debug purposes only
   */
  public Set<String> getModelDocuments() {
      return documents.keySet();
  }

  /**
   * For debug purposes only
   */
  public String getModelDocument(String documentId) {
    return documents.get(documentId).toDebugString();
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
