package org.swellrt.beta.model.wave.mutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.swellrt.beta.client.wave.SWaveDocuments;
import org.swellrt.beta.common.ContextStatus;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.ModelFactory;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.presence.SSessionManager;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.WaveCommons;
import org.swellrt.beta.model.wave.adt.DocumentBasedBasicRMap;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.id.IdConstants;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.testing.OpBasedWaveletFactory;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipantIdUtil;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

/**
 * Stuff mapping observable wavelet to a Swell observable data model
 *
 *
 * The Swell data model is mapped with Wave data model as follows:
 * <p>
 * Wave => SObject instance <br>
 * Wavelet => Container <br>
 * Blip/Document => Substrate <br>
 * <p>
 * <p>
 * Each SNode is stored in a substrate blip/document. Blips could be stored in
 * different container Wavelets for performance reasons if it is necessary. <br>
 * <p>
 * <li>A map (see example below) is represented as XML structure within a blip.
 * <li>Primitive values are embedded in their container structure
 * <li>nested containers {@link SMap} or {@link SList} are represented as
 * pointers to its substrate blip.
 * <p>
 * <br>
 *
 * <pre>
 * {@code
 *
 *    prop1 : "value1",
 *    prop2 : 12345,
 *    prop3 :
 *              prop31: "value31"
 *              prop32: "value32"
 *
 *     prop4 : [ "a", "b", "c" ]
 *
 *
 *
 *  <object>
 *    <prop1 t='s'>value1</prop1>
 *    <prop2 t='i'>12345</prop2>
 *    <prop3 t='o'>
 *          <prop31 t='s'>value31</prop31>
 *          <prop32 t='s'>value32</prop32>
 *    </prop3>
 *    <prop4 t='a'>
 *          <item t='s'>a</item>
 *          <item t='s'>b</item>
 *          <item t='s'>c</item>
 *    </prop4>
 *  </object>
 *
 * }
 * </pre>
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SWaveNodeManager {

  /** Factory to create nodes in special conditions. */
  public interface NodeFactory {

    public SWaveText createWaveText(SWaveNodeManager nodeManager, SubstrateId substrateId,
        Blip blip);

  }


  public static abstract class Deserializer {

    SWaveNode deserialize(String s) {

      Preconditions.checkNotNull(s, "Unable to deserialize a null value");

      SubstrateId substrateId = SubstrateId.deserialize(s);
      if (substrateId != null) {

        if (substrateId.isList())
          return materializeList(substrateId);

        if (substrateId.isMap())
          return materializeMap(substrateId);

        if (substrateId.isText())
          return materializeText(substrateId);

        return null;

      } else {
        return SPrimitive.deserialize(s);
      }

    }

    protected abstract SWaveNode materializeList(SubstrateId substrateId);

    protected abstract SWaveNode materializeMap(SubstrateId substrateId);

    protected abstract SWaveNode materializeText(SubstrateId substrateId);



  }

  /**
   * A serializer/deserializer of SNode objects to/from a Wave's list
   */
  public static class SubstrateListSerializer
      implements org.waveprotocol.wave.model.adt.docbased.Factory<Doc.E, SWaveNode, SWaveNode> {

    Deserializer d;

    public SubstrateListSerializer(Deserializer d) {
      this.d = d;
    }

    @Override
    public SWaveNode adapt(DocumentEventRouter<? super E, E, ?> router, E element) {
      Map<String, String> attributes = router.getDocument().getAttributes(element);
      return d.deserialize(attributes.get(WaveCommons.LIST_ENTRY_VALUE_ATTR));
    }

    @Override
    public Initializer createInitializer(SWaveNode node) {
      return new org.waveprotocol.wave.model.adt.docbased.Initializer() {

        @Override
        public void initialize(Map<String, String> target) {
          target.put(WaveCommons.LIST_ENTRY_KEY_ATTR, String.valueOf(System.currentTimeMillis())); // temp
          target.put(WaveCommons.LIST_ENTRY_VALUE_ATTR, serialize(node));
        }

      };
    }

  }

  /**
   * A serializer/deserializer of SNode objects to/from a Wave's map
   */
  public static class SubstrateMapSerializer
      implements org.waveprotocol.wave.model.util.Serializer<SWaveNode> {

    Deserializer d;

    public SubstrateMapSerializer(Deserializer d) {
      this.d = d;
    }

    @Override
    public String toString(SWaveNode x) {
      return serialize(x);
    }

    @Override
    public SWaveNode fromString(String s) {
      return d.deserialize(s);
    }

    @Override
    public SWaveNode fromString(String s, SWaveNode defaultValue) {
      return fromString(s);
    }

  }

  private SWaveNodeManager.Deserializer d = new SWaveNodeManager.Deserializer() {

    @Override
    protected SWaveNode materializeList(SubstrateId substrateId) {
      return loadList(substrateId);
    }

    @Override
    protected SWaveNode materializeMap(SubstrateId substrateId) {
      return loadMap(substrateId);
    }

    @Override
    protected SWaveNode materializeText(SubstrateId substrateId) {
      return loadText(substrateId, null);
    }

  };

  private SWaveNodeManager.SubstrateMapSerializer mapSerializer = new SWaveNodeManager.SubstrateMapSerializer(d);

  private final Map<SubstrateId, SWaveNode> nodeStore = new HashMap<SubstrateId, SWaveNode>();
  private final SSessionManager session;
  private final IdGenerator idGenerator;
  private final String domain;
  private final ObservableWaveView wave;
  private final ContextStatus waveStatus;

  private final ObservableWavelet dataWavelet;
  private final ObservableWavelet userWavelet;
  private final ObservableWavelet transientWavelet;

  private final SWaveDocuments<? extends InteractiveDocument> documentRegistry;

  private SWaveTransient transientData;
  private SWaveMetadata metadata;

  private SWaveObject waveObject;

  public static SWaveNodeManager create(SSessionManager session, IdGenerator idGenerator,
      String domain, ObservableWaveView wave, ContextStatus waveStatus,
      SWaveDocuments<? extends InteractiveDocument> documentRegistry) {
    return new SWaveNodeManager(session, idGenerator, domain, wave, waveStatus, documentRegistry);
  }

  private static ObservableWavelet retrieveDataWavelet(String domain, ObservableWaveView wave) {

    ObservableWavelet dataWavelet = wave
        .getWavelet(WaveletId.of(domain, IdConstants.DATA_MASTER_WAVELET));

    if (dataWavelet == null) {
      dataWavelet = wave.createWavelet(WaveletId.of(domain, IdConstants.DATA_MASTER_WAVELET));
    }

    return dataWavelet;
  }

  private static ObservableWavelet retrieveUserWavelet(String domain, ObservableWaveView wave,
      ParticipantId participantId) {

    String userWaveletid = IdConstants.USER_DATA_WAVELET_PREFIX + IdConstants.TOKEN_SEPARATOR
        + participantId.getAddress();

    if (!participantId.isAnonymous()) {

      ObservableWavelet userWavelet = wave
          .getWavelet(WaveletId.of(domain, userWaveletid));

      if (userWavelet == null) {
        userWavelet = wave.createWavelet(WaveletId.of(domain, userWaveletid));
      }

      return userWavelet;

    } else {


      ObservableWavelet userWavelet = OpBasedWaveletFactory.builder(new SchemaProvider() {

        @Override
        public DocumentSchema getSchemaForId(WaveletId waveletId, String documentId) {
          return DocumentSchema.NO_SCHEMA_CONSTRAINTS;
        }
      }).with(participantId).build().create(wave.getWaveId(), WaveletId.of(domain, userWaveletid),
          participantId);

      return userWavelet;
    }
  }

  private static ObservableWavelet retrieveTransientWavelet(String domain,
      ObservableWaveView wave) {

    ObservableWavelet transientWavelet = wave
        .getWavelet(WaveletId.of(domain, IdConstants.TRANSIENT_MASTER_WAVELET));

    if (transientWavelet == null) {
      transientWavelet = wave
          .createWavelet(WaveletId.of(domain, IdConstants.TRANSIENT_MASTER_WAVELET));

      ObservableWavelet dataWavelet = retrieveDataWavelet(domain, wave);
      ObservableWavelet tw = transientWavelet;
      dataWavelet.getParticipantIds().forEach(p -> {
        if (!tw.getParticipantIds().contains(p))
          tw.addParticipant(p);
      });

    }

    return transientWavelet;
  }


  private SWaveNodeManager(SSessionManager session, IdGenerator idGenerator, String domain,
      ObservableWaveView wave, ContextStatus waveStatus,
      SWaveDocuments<? extends InteractiveDocument> documentRegistry) {
    this.session = session;
    this.idGenerator = idGenerator;
    this.domain = domain;
    this.wave = wave;
    this.dataWavelet = retrieveDataWavelet(domain, wave);
    this.userWavelet = retrieveUserWavelet(domain, wave, session.get().getParticipantId());
    this.transientWavelet = retrieveTransientWavelet(domain, wave);
    this.waveStatus = waveStatus;
    this.documentRegistry = documentRegistry;
  }

  public void setListener(SWaveletListener listener) {
    this.wave.getWavelet(WaveletId.of(domain, WaveCommons.MASTER_DATA_WAVELET_NAME))
        .addListener(listener);
  }

  public ParticipantId getParticipant() {
    return session.get().getParticipantId();
  }

  public SSessionManager getSession() {
    return session;
  }

  public String getId() {
    return ModernIdSerialiser.INSTANCE.serialiseWaveId(wave.getWaveId());
  }

  public WaveId getWaveId() {
    return wave.getWaveId();
  }

  public SWaveMap getDataRoot() {
    SWaveMap map = loadMap(
        SubstrateId.ofMap(dataWavelet.getId(), IdConstants.MAP_ROOT_DOC));
    map.attach(SWaveNodeContainer.Void);
    return map;
  }

  public SWaveMap getTransientRoot() {
    SWaveMap map = loadMap(SubstrateId.ofMap(transientWavelet.getId(), IdConstants.MAP_ROOT_DOC));
    map.attach(SWaveNodeContainer.Void);
    return map;
  }

  public SWaveMap getMetadataRoot() {
    SWaveMap map = loadMap(SubstrateId.ofMap(dataWavelet.getId(), IdConstants.MAP_METADATA_ROOT_DOC));
    map.attach(SWaveNodeContainer.Void);
    return map;
  }

  public SWaveMetadata getMetadata() {
    if (metadata == null) {
      metadata = new SWaveMetadata(getMetadataRoot());
    }
    return metadata;
  }

  public SWaveTransient getTransient() {
    if (transientData == null) {
      transientData = new SWaveTransient(getTransientRoot());
    }
    return transientData;
  }

  public SWaveMap getUserRoot() {
    SWaveMap map = loadMap(SubstrateId.ofMap(userWavelet.getId(), IdConstants.MAP_ROOT_DOC));
    map.attach(SWaveNodeContainer.Void);
    return map;
  }

  private DocEventRouter getWaveletDocument(SubstrateId substrateId) {

    if (substrateId.getContainerId().isUserWavelet()
        && session.get().getParticipantId().isAnonymous()) {

      // Anonymous user data wavelet are not supported by the actual wave
      ObservableDocument document = userWavelet.getDocument(substrateId.getDocumentId());
      DefaultDocEventRouter router = DefaultDocEventRouter.create(document);
      return router;

    } else {

      // Normal documents are supported by wave's wavelets
      ObservableWavelet substrateContainer = wave.getWavelet(substrateId.getContainerId());
      ObservableDocument document = substrateContainer.getDocument(substrateId.getDocumentId());
      DefaultDocEventRouter router = DefaultDocEventRouter.create(document);
      return router;

    }

  }

  private ObservableWavelet getWavelet(SubstrateId substrateId) {

    if (substrateId.getContainerId().isUserWavelet()
        && session.get().getParticipantId().isAnonymous()) {

      // Anonymous user data wavelet are not supported by the actual wave
      return userWavelet;

    } else {

      // Normal documents are supported by wave's wavelets
      return wave.getWavelet(substrateId.getContainerId());

    }

  }

  /**
   * Materialize a SWaveMap from its blip substrate. The blip substrate is
   * created if it doesn't exist yet. The Wavelet must exists.
   * <p>
   * TODO: manage auto-creation of Wavelets?
   *
   * @param substrateId
   * @return
   */
  protected SWaveMap loadMap(SubstrateId substrateId) {

    Preconditions.checkArgument(substrateId.isMap(), "Expected a map susbtrate id");

    // Cache instances
    if (nodeStore.containsKey(substrateId)) {
      return (SWaveMap) nodeStore.get(substrateId);
    }

    // Create new instance
    DocEventRouter router = getWaveletDocument(substrateId);
    ObservableDocument document = router.getDocument();

    E mapElement = DocHelper.getElementWithTagName(document, WaveCommons.MAP_TAG);
    if (mapElement == null) {
      mapElement = document.createChildElement(document.getDocumentElement(), WaveCommons.MAP_TAG,
          Collections.<String, String> emptyMap());
    }

    ObservableBasicMap<String, SWaveNode> map = DocumentBasedBasicRMap.create(router, mapElement,
        Serializer.STRING, mapSerializer, WaveCommons.MAP_ENTRY_TAG,
        WaveCommons.MAP_ENTRY_KEY_ATTR, WaveCommons.MAP_ENTRY_VALUE_ATTR);

    SWaveMap n = SWaveMap.create(this, substrateId, map);
    nodeStore.put(substrateId, n);

    return n;
  }

  /**
   * Materialize a SWaveList from its blip substrate. The blip substrate is
   * created if it doesn't exist yet. The Wavelet must exists.
   * <p>
   * TODO: manage auto-creation of Wavelets?
   *
   * @param substrateId
   * @return
   */
  protected SWaveList loadList(SubstrateId substrateId) {

    // Cache instances
    if (nodeStore.containsKey(substrateId)) {
      return (SWaveList) nodeStore.get(substrateId);
    }

    // Create new instance
    DocEventRouter router = getWaveletDocument(substrateId);
    ObservableDocument document = router.getDocument();

    E listElement = DocHelper.getElementWithTagName(document, WaveCommons.LIST_TAG);
    if (listElement == null) {
      listElement = document.createChildElement(document.getDocumentElement(),
          WaveCommons.LIST_TAG,
          Collections.<String, String> emptyMap());
    }

    ObservableElementList<SWaveNode, SWaveNode> list = DocumentBasedElementList.create(router,
        listElement, WaveCommons.LIST_ENTRY_TAG, new SWaveNodeManager.SubstrateListSerializer(d));

    SWaveList n = SWaveList.create(this, substrateId, list);
    nodeStore.put(substrateId, n);

    return n;
  }

  protected SWaveText loadText(SubstrateId substrateId, DocInitialization docInit) {

    Preconditions.checkArgument(substrateId.isText(), "Expected a text susbtrate id");

    // Cache instances
    if (nodeStore.containsKey(substrateId)) {
      return (SWaveText) nodeStore.get(substrateId);
    }


    ObservableWavelet substrateContainer = getWavelet(substrateId);
    Blip blip = substrateContainer.getBlip(substrateId.getDocumentId());
    if (blip == null) {
      blip = substrateContainer.createBlip(substrateId.getDocumentId());

      // TODO The docInit stuff seems not to work, check out LazyContentDocument
      // blip = substrateContainer.createBlip(substrateId.getDocumentId(),
      // docInit);
    }

    SWaveText textRemote = ModelFactory.instance.createWaveText(this, substrateId, blip, docInit,
        documentRegistry.getTextDocument(substrateId));

    nodeStore.put(substrateId, textRemote);
    return textRemote;
  }

  public SWaveList createList(SubstrateId containerSubstrateId) {
    return loadList(SubstrateId.createForList(containerSubstrateId.getContainerId(), idGenerator));
  }

  public SWaveMap createMap(SubstrateId containerSubstrateId) {
    return loadMap(SubstrateId.createForMap(containerSubstrateId.getContainerId(), idGenerator));
  }

  public SWaveText createText(DocInitialization initText, SubstrateId containerSubstrateId) {
    return loadText(SubstrateId.createForText(containerSubstrateId.getContainerId(), idGenerator),
        initText);
  }

  /**
   * Delete reference of this node in the cache
   *
   * @param node
   */
  public void flushCache(SWaveNode node) {
    if (node.getSubstrateId() != null) {
      nodeStore.remove(node.getSubstrateId());
    }
  }


  /**
   * Empty the substrate to mark it as deleted.
   * <p>
   * This method must be called after the substrate reference is removed from
   * its container.
   *
   * @param substrateId
   *          subtrate id
   */
  protected void emptySubstrate(SubstrateId substrateId) {
    ObservableWavelet w = wave.getWavelet(substrateId.getContainerId());
    Document d = null;
    if (SubstrateId.isText(substrateId.getDocumentId())) {
      d = w.getBlip(substrateId.getDocumentId()).getContent();
    } else {
      d = w.getDocument(substrateId.getDocumentId());
    }
    Doc.E root = DocHelper.getFirstChildElement(d, d.getDocumentElement());
    if (root != null)
      d.deleteNode(root);
  }

  //
  // Node Adapter
  //

  /**
   * Transform a local SNode object to SWaveNode recursively.
   * <p>
   * Create new SNodes in the same wavelet containing the parent SNode.
   * <p>
   * SNode trees supported by multipled wavelets is not implemented yet.
   *
   * @param node
   * @param parentNode
   * @param containerId
   * @return
   */
  public SWaveNode transformToWaveNode(SNode node, SWaveNodeContainer parentNode)
      throws SException {

    if (node instanceof SWaveNode)
      return (SWaveNode) node;

    if (node instanceof SList) {
      @SuppressWarnings("unchecked")
      SList<SNode> list = (SList<SNode>) node;
      SWaveList remoteList = createList(parentNode.getSubstrateId());
      remoteList.attach(parentNode);
      remoteList.enableEvents(false);
      for (SNode n : list.values()) {
        remoteList.add(transformToWaveNode(n, remoteList));
      }
      remoteList.enableEvents(true);
      return remoteList;

    } else if (node instanceof SMap) {
      SMap map = (SMap) node;
      SWaveMap remoteMap = createMap(parentNode.getSubstrateId());
      remoteMap.attach(parentNode);
      remoteMap.enableEvents(false);
      for (String k : map.keys()) {
        SNode v = map.pick(k);
        remoteMap.put(k, transformToWaveNode(v, remoteMap));
      }
      remoteMap.enableEvents(true);
      return remoteMap;

    } else if (node instanceof SText) {
      SText text = (SText) node;
      SWaveText remoteText = createText(text.asDocInitialization(),
          parentNode.getSubstrateId());
      remoteText.attach(parentNode);

      return remoteText;

    } else if (node instanceof SPrimitive) {
      SPrimitive primitive = (SPrimitive) node;
      primitive.attach(parentNode);
      return primitive;
    }

    return null;
  }

  //
  // Access control
  //

  /**
   * Checks if the status of the object or any underlying component (wave view,
   * channel, websocket...) is normal.
   * <p>
   * Throws a {@link SException} otherwise.
   * <p>
   * This method should be called before doing any operation in the object.
   */
  protected void check() throws SException {
    if (waveStatus != null)
      waveStatus.check();
  }

  /**
   * Check if node can be written by the current log in user.
   * <p>
   * Actual check is delegated to the node.
   * <p>
   * TODO Only SPrimitives nodes support access control so far.
   *
   * @param node
   * @throws SException
   *           throw exception if access is forbidden
   */
  public void checkWritable(SNode node) throws SException {
    if (node != null && node instanceof SPrimitive) {
      if (!((SPrimitive) node).canWrite(getParticipant()))
        throw new SException(SException.WRITE_FORBIDDEN, null, "Not allowed to write property");
    }
  }

  /**
   * Check if node can be read by the current log in user.
   * <p>
   * Actual check is delegated to the node.
   * <p>
   * TODO Only SPrimitives nodes support access control so far.
   *
   * @param node
   * @throws SException
   *           throw exception if access is forbidden
   */
  public void checkReadable(SNode node) throws SException {
    if (node != null && node instanceof SPrimitive) {
      if (!((SPrimitive) node).canRead(getParticipant()))
        throw new SException(SException.READ_FORBIDDEN, null, "Not allowed to read property");
    }
  }

  //
  // Participants
  //

  public void setPublic(boolean isPublic) {
    ParticipantId publicParticipanId = ParticipantIdUtil.makeAnyoneParticipantId(domain);

    if (isPublic) {
      dataWavelet.addParticipant(publicParticipanId);
      transientWavelet.addParticipant(publicParticipanId);
    } else {
      dataWavelet.removeParticipant(publicParticipanId);
      transientWavelet.removeParticipant(publicParticipanId);
    }
  }

  public boolean isPublic() {
    ParticipantId publicParticipanId = ParticipantId.ofPublic(domain);
    return dataWavelet.getParticipantIds().contains(publicParticipanId);
  }

  public void addParticipant(String participantId) throws InvalidParticipantAddress {
    ParticipantId p = ParticipantId.of(participantId);
    dataWavelet.addParticipant(p);
    transientWavelet.addParticipant(p);
  }

  public void removeParticipant(String participantId) throws InvalidParticipantAddress {
    ParticipantId p = ParticipantId.of(participantId);
    dataWavelet.removeParticipant(p);
    transientWavelet.removeParticipant(p);
  }

  public Set<ParticipantId> getParticipants() {
    return dataWavelet.getParticipantIds();
  }

  public ParticipantId getCreatorId() {
    return dataWavelet.getCreatorId();
  }

  //
  // For debug
  //

  /**
   * @return a list of all container's ids for this object
   */
  public String[] getWavelets() {

    List<String> containerIds = new ArrayList<String>();

    wave.getWavelets().forEach((wavelet) -> {
      containerIds.add(ModernIdSerialiser.INSTANCE.serialiseWaveletId(wavelet.getId()));
    });

    return containerIds.toArray(new String[containerIds.size()]);
  }

  public String[] getDocuments(String strWaveletId) {
    WaveletId waveletId;
    try {
      waveletId = ModernIdSerialiser.INSTANCE.deserialiseWaveletId(strWaveletId);
      return wave.getWavelet(waveletId).getDocumentIds().toArray(new String[0]);
    } catch (Exception e) {
      return null;
    }
  }

  public String getContent(String strWaveletId, String strDocumentId) {

    WaveletId waveletId;
    try {
      waveletId = ModernIdSerialiser.INSTANCE.deserialiseWaveletId(strWaveletId);
      return wave.getWavelet(waveletId).getWaveletData().getDocument(strDocumentId).getContent()
          .getMutableDocument().toXmlString();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Serialize a SNode to be stored in a Swell blip.
   *
   * @param x
   * @return
   */
  public static String serialize(SNode x) {

    // Order matters check SPrimitive first
    if (x instanceof SPrimitive) {
      SPrimitive p = (SPrimitive) x;
      return p.serialize();
    }

    if (x instanceof SWaveNode) {
      SWaveNode r = (SWaveNode) x;
      SubstrateId id = r.getSubstrateId();
      if (id != null)
        return id.serialize();
    }

    return null;
  }

  /**
   * @return the {@link SWaveObject} managed by this node manager.
   */
  public SWaveObject getSWaveObject() {

    if (waveObject == null) {
      waveObject = new SWaveObject(this);
      getMetadata().logSession(session.get());
      this.setListener(new SWaveletListener(waveObject));
    }

    return waveObject;

  }

  public HashedVersion getWaveSubstrateVersion(SWaveNode node) {

    Blip blip = wave.getWavelet(node.getSubstrateId().getContainerId())
        .getBlip(node.getSubstrateId().getDocumentId());

    return blip.getWavelet().getHashedVersion();

  }
}
