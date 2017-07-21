package org.swellrt.beta.model.wave.mutable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.swellrt.beta.client.PlatformBasedFactory;
import org.swellrt.beta.client.WaveStatus;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.WaveCommons;
import org.swellrt.beta.model.wave.adt.DocumentBasedBasicRMap;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager.Deserializer;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager.SubstrateListSerializer;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager.SubstrateMapSerializer;
import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;
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
          return materializeText(substrateId, null);
  
        return null;
  
      } else {
        return SPrimitive.deserialize(s);
      }
  
    }
  
    protected abstract SWaveNode materializeList(SubstrateId substrateId);
  
    protected abstract SWaveNode materializeMap(SubstrateId substrateId);
  
    protected abstract SWaveNode materializeText(SubstrateId substrateId, DocInitialization docInit);
  
  
  
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
    protected SWaveNode materializeText(SubstrateId substrateId, DocInitialization docInit) {
      return loadText(substrateId, docInit);
    }

  };

  private SWaveNodeManager.SubstrateMapSerializer mapSerializer = new SWaveNodeManager.SubstrateMapSerializer(d);

  private final Map<SubstrateId, SWaveNode> nodeStore = new HashMap<SubstrateId, SWaveNode>();
  private final ParticipantId participantId;
  private final IdGenerator idGenerator;
  private final String domain;
  private final ObservableWaveView wave;
  private final WaveStatus waveStatus;
  private final PlatformBasedFactory platformBasedFactory;

  private final ObservableWavelet masterContainerWavelet;


  private ObservableWavelet currentContainerWavelet;

  public static SWaveNodeManager of(ParticipantId participantId, IdGenerator idGenerator,
      String domain, ObservableWaveView wave, WaveStatus waveStatus, PlatformBasedFactory platformBasedFactory) {

    ObservableWavelet masterWavelet = wave
        .getWavelet(WaveletId.of(domain, WaveCommons.MASTER_DATA_WAVELET_NAME));

    if (masterWavelet == null) {
      masterWavelet = wave
          .createWavelet(WaveletId.of(domain, WaveCommons.MASTER_DATA_WAVELET_NAME));
    }

    return new SWaveNodeManager(participantId, idGenerator, domain, wave, waveStatus,
        platformBasedFactory);
  }

  private SWaveNodeManager(ParticipantId participantId, IdGenerator idGenerator, String domain,
      ObservableWaveView wave, WaveStatus waveStatus, PlatformBasedFactory platformBasedFactory) {
    this.participantId = participantId;
    this.idGenerator = idGenerator;
    this.domain = domain;
    this.wave = wave;
    this.masterContainerWavelet = wave
        .getWavelet(WaveletId.of(domain, WaveCommons.MASTER_DATA_WAVELET_NAME));
    this.currentContainerWavelet = null;
    this.waveStatus = waveStatus;
    this.platformBasedFactory = platformBasedFactory;
  }

  public void setListener(SWaveletListener listener) {
    this.wave.getWavelet(WaveletId.of(domain, WaveCommons.MASTER_DATA_WAVELET_NAME))
        .addListener(listener);
  }

  public ParticipantId getParticipant() {
    return participantId;
  }

  public String getId() {
    return ModernIdSerialiser.INSTANCE.serialiseWaveId(wave.getWaveId());
  }

  /**
   * Create a new data container (Wavelet) for this Object (Wave)
   *
   * @return
   */
  public String createContainer() {
    ObservableWavelet wavelet = wave.createWavelet(WaveletId.of(domain,
        WaveCommons.CONTAINER_DATA_WAVELET_PREFIX + idGenerator.newUniqueToken()));
    return ModernIdSerialiser.INSTANCE.serialiseWaveletId(wavelet.getId());
  }

  /**
   * @return a list of all container's ids for this object
   */
  public String[] getContainers() {

    List<String> containerIds = new ArrayList<String>();

    wave.getWavelets().forEach((wavelet) -> {
      if (wavelet.getId().getId().startsWith(WaveCommons.CONTAINER_DATA_WAVELET_PREFIX)) {
        containerIds.add(ModernIdSerialiser.INSTANCE.serialiseWaveletId(wavelet.getId()));
      }
    });

    return containerIds.toArray(new String[containerIds.size()]);
  }

  public SWaveMap loadRoot() {
    return loadMap(
        SubstrateId.ofMap(masterContainerWavelet.getId(), WaveCommons.ROOT_SUBSTRATE_ID));
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
  public SWaveMap loadMap(SubstrateId substrateId) {

    Preconditions.checkArgument(substrateId.isMap(), "Expected a map susbtrate id");

    // Cache instances
    if (nodeStore.containsKey(substrateId)) {
      return (SWaveMap) nodeStore.get(substrateId);
    }

    // Create new instance
    ObservableWavelet substrateContainer = wave.getWavelet(substrateId.getContainerId());
    ObservableDocument document = substrateContainer.getDocument(substrateId.getDocumentId());
    DefaultDocEventRouter router = DefaultDocEventRouter.create(document);

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
  public SWaveList loadList(SubstrateId substrateId) {

    // Cache instances
    if (nodeStore.containsKey(substrateId)) {
      return (SWaveList) nodeStore.get(substrateId);
    }

    // Create new instance
    ObservableWavelet substrateContainer = wave.getWavelet(substrateId.getContainerId());
    ObservableDocument document = substrateContainer.getDocument(substrateId.getDocumentId());
    DefaultDocEventRouter router = DefaultDocEventRouter.create(document);

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

  public SWaveText loadText(SubstrateId substrateId, DocInitialization docInit) {

    Preconditions.checkArgument(substrateId.isText(), "Expected a text susbtrate id");

    // Cache instances
    if (nodeStore.containsKey(substrateId)) {
      return (SWaveText) nodeStore.get(substrateId);
    }

    ObservableWavelet substrateContainer = wave.getWavelet(substrateId.getContainerId());
    Blip blip = substrateContainer.getBlip(substrateId.getDocumentId());
    if (blip == null) {
      blip = substrateContainer.createBlip(substrateId.getDocumentId());

      // TODO The docInit stuff seems not to work, check out LazyContentDocument
      // blip = substrateContainer.createBlip(substrateId.getDocumentId(),
      // docInit);
    }

    SWaveText textRemote = platformBasedFactory.getSTextRemote(this, substrateId, blip);
    if (docInit != null) {
      textRemote.setInitContent(docInit);
    }
    nodeStore.put(substrateId, textRemote);
    return textRemote;
  }

  public void setContainer(String containerId) throws SException {
    WaveletId waveletId = null;
    try {
      waveletId = ModernIdSerialiser.INSTANCE.deserialiseWaveletId(containerId);
    } catch (InvalidIdException e) {
      throw new SException(SException.INVALID_ID);
    }
    ObservableWavelet wavelet = wave.getWavelet(waveletId);
    Preconditions.checkArgument(wavelet != null, "Container doesn't exist");
    currentContainerWavelet = wavelet;
  }

  public void resetContainer() {
    currentContainerWavelet = null;
  }

  private WaveletId getEffectiveContainerId(WaveletId containerId) {

    if (currentContainerWavelet != null)
      return currentContainerWavelet.getId();
    else if (containerId != null)
      return containerId;
    else
      return masterContainerWavelet.getId();

  }

  public SWaveList createList(WaveletId containerId) {
    return loadList(SubstrateId.createForList(getEffectiveContainerId(containerId), idGenerator));
  }

  public SWaveMap createMap(WaveletId containerId) {
    return loadMap(SubstrateId.createForMap(getEffectiveContainerId(containerId), idGenerator));
  }

  public SWaveText createText(DocInitialization initText, WaveletId containerId) {
    return loadText(SubstrateId.createForText(getEffectiveContainerId(containerId), idGenerator),
        initText);
  }

  /**
   * Delete reference of this node in the cache and delete substrate
   *
   * @param node
   */
  public void deleteNode(SWaveNode node) {
    if (node.getSubstrateId() != null) {
      emptySubstrate(node.getSubstrateId());
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
  private void emptySubstrate(SubstrateId substrateId) {
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
   * Use parent node's container for the new nodes except if a particular
   * container has been set in the manager.
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
      SWaveList remoteList = createList(parentNode.getSubstrateId().getContainerId());
      remoteList.attach(parentNode);
      remoteList.enableEvents(false);
      for (SNode n : list.values()) {
        remoteList.add(transformToWaveNode(n, remoteList));
      }
      remoteList.enableEvents(true);
      return remoteList;

    } else if (node instanceof SMap) {
      SMap map = (SMap) node;
      SWaveMap remoteMap = createMap(parentNode.getSubstrateId().getContainerId());
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
      SWaveText remoteText = createText(text.getInitContent(),
          parentNode.getSubstrateId().getContainerId());
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

    try {

      if (isPublic)
        masterContainerWavelet.addParticipant(ParticipantId.of("", domain));
      else
        masterContainerWavelet.removeParticipant(ParticipantId.of("", domain));

    } catch (InvalidParticipantAddress e) {

    }
  }

  public boolean isPublic() {
    try {
      return masterContainerWavelet.getParticipantIds().contains(ParticipantId.of("", domain));
    } catch (InvalidParticipantAddress e) {
      return false;
    }
  }

  public void addParticipant(String participantId) throws InvalidParticipantAddress {
    masterContainerWavelet.addParticipant(ParticipantId.of(participantId));
  }

  public void removeParticipant(String participantId) throws InvalidParticipantAddress {
    masterContainerWavelet.removeParticipant(ParticipantId.of(participantId));
  }

  public String[] getParticipants() {
    String[] array = new String[masterContainerWavelet.getParticipantIds().size()];
    int i = 0;
    for (ParticipantId p : masterContainerWavelet.getParticipantIds()) {
      array[i++] = p.getAddress();
    }
    return array;
  }

  public SMap getUserObject() {

    ObservableWavelet userWavelet = wave.getUserData();
    if (userWavelet == null)
      userWavelet = wave.createUserData();

    SWaveMap root = loadMap(
          SubstrateId.ofMap(userWavelet.getId(), WaveCommons.USER_ROOT_SUBSTRATED_ID));
    root.attach(SWaveNodeContainer.Void);

    return root;
  }

  //
  // For debug
  //

  public String[] getBlips() {
    return masterContainerWavelet.getDocumentIds()
        .toArray(new String[masterContainerWavelet.getDocumentIds().size()]);
  }

  public String getBlipXML(String blipId) {
    if (masterContainerWavelet.getDocumentIds().contains(blipId)) {
      if (SubstrateId.isText(blipId)) {
        return masterContainerWavelet.getBlip(blipId).getContent().toXmlString();
      } else {
        return masterContainerWavelet.getDocument(blipId).toXmlString();
      }
    }

    return null;
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

}
