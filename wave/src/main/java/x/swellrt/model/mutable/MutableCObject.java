package x.swellrt.model.mutable;

import java.util.Collections;

import org.swellrt.model.ModelSchemas;
import org.swellrt.model.adt.DocumentBasedBasicRMap;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TextType;
import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.testing.FakeIdGenerator;
import org.waveprotocol.wave.model.testing.FakeWaveView;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.Wavelet;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import x.swellrt.model.CNode;
import x.swellrt.model.CPrimitive;
import x.swellrt.model.local.java.LocalCMap;

/**
 * 
 * The core implementation of a generic/mutable object data model built on top of Wave data model.
 * 
 * The data model is based on the CObject abstraction. A CObject is similar to a JSON document.
 * 
 * A CObject instance contains a tree-like structure of maps, lists and primitive values. 
 * This structure can be dynamically built and changed at runtime.
 * 
 * The underlying implementation based on the Wave data model provides real-time mutability of CObjects shared
 * among different Wave users. Also, it provides in-flight persistence and eventual consistency.
 * 
 * It is intended to provide wrappers for this model in different languages, at least JavaScript and Java.
 * 
 * 
 * The CObject data model is mapped with Wave data model as follows:
 * 
 *  Wave => CObject
 *  
 *  Wavelet => Container
 *  
 *  Blip/Document => Substrate
 * 
 * 
 * A CNode is each node of the tree structure, it can be a CMap, a CList or CPrimitive.
 * 
 * CNode's data is stored in a substrate Wave document.
 * 
 * Subtree's of a CObject (that is, a set of substrate documents) can be stored in different Container Wavelets.
 * This allows to control performance, as long as a Wavelet is the minimal entity of storage in a Wave.
 * 
 * { 
 *    prop1 : "value1",
 *    prop2 : 12345,
 *    prop3 : {
 *              prop31: "value31"
 *              prop32: "value32"
 *            },
 *     prop4 : [ "a", "b", "c" ]
 *  }
 * 
 * <pre>
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
 * </pre>
 * 
 * @author pablojan@gmail.com
 *
 */
public class MutableCObject {
  
  private static final String MASTER_DATA_WAVELET_NAME = "data+master";
  private static final String ROOT_SUBSTRATE_ID = "m+root";
  
  private static final String SEPARATOR = ":";
  private static final String MAP_TYPE_PREFIX = "m";
  private static final String STRING_TYPE_PREFIX  = "s";
  private static final String BOOLEAN_TYPE_PREFIX  = "b";
  private static final String INTEGER_TYPE_PREFIX  = "i";
  private static final String DOUBLE_TYPE_PREFIX  = "d";
  
  /**
   *
   */
  public class SubstrateMapSerializer implements org.waveprotocol.wave.model.util.Serializer<MutableCNode> {

    
    @Override
    public String toString(MutableCNode x) {

      // Map => m:waveletid:blipid
      
      if (x instanceof MutableCMap) {
        MutableCMap map = (MutableCMap) x;        
        return MAP_TYPE_PREFIX+SEPARATOR+ModernIdSerialiser.INSTANCE.serialiseWaveletId(map.getContainerId())+SEPARATOR+map.getSubstrateId();            
      }
      
      // String => s:value
      
      if (x instanceof CPrimitive) {        
        CPrimitive p = (CPrimitive) x;
        
        if (p.getType() == CPrimitive.TYPE_STRING)
          return STRING_TYPE_PREFIX+SEPARATOR+p.asString();   
        
        if (p.getType() == CPrimitive.TYPE_BOOL)
          return STRING_TYPE_PREFIX+SEPARATOR+p.asString();   

        
      }
      
      return null;
    }

    @Override
    public MutableCNode fromString(String s) {      
      
      Preconditions.checkNotNull(s, "Unable to deserialize a null value");    
      String[] parts = s.split(SEPARATOR);
      Preconditions.checkArgument(parts.length > 0, "Unable to deserialize value from "+s);
      
      String type = parts[0];
      
      if (type.equals(MAP_TYPE_PREFIX)) {
        Preconditions.checkArgument(parts.length == 3, "Unable to deserialize map from "+s);
        String containerId = parts[1];
        String substrateId = parts[2];
        
        try {
          return loadMutableCMap(ModernIdSerialiser.INSTANCE.deserialiseWaveletId(containerId), substrateId);
        
        } catch (InvalidIdException e) {
          // TODO handle gracefully
          return null;
        }
      }
      
      if (type.equals(STRING_TYPE_PREFIX)) {
        Preconditions.checkArgument(parts.length == 2, "Unable to deserialize primitive type from "+s);
        String stringValue = parts[1];     
        return new CPrimitive(stringValue);
      }
      
      return null;
    }

    @Override
    public MutableCNode fromString(String s, MutableCNode defaultValue) {
      return fromString(s);
    }
    
    
  }
  
  private static final String MAP_TAG = "map";
  private static final String MAP_ENTRY_TAG = "entry";
  private static final String MAP_ENTRY_KEY_ATTR = "k";
  private static final String MAP_ENTRY_VALUE_ATTR = "v";
  
  private final String domain;
  private final IdGenerator idGenerator;
  private final ObservableWaveView wave;
  private ObservableWavelet masterWavelet;
  
  private SubstrateMapSerializer mapSerializer;
  
  private MutableCMap root;
  
  /**
   * Get a MutableCObject instance with a substrate Wave.
   * Initialize the Wave accordingly.
   * 
   */
  public static MutableCObject ofWave(IdGenerator idGenerator, String domain, ObservableWaveView wave) {
    
    Preconditions.checkArgument(domain != null && !domain.isEmpty(), "Domain is not provided");
    Preconditions.checkArgument(wave != null, "Wave can't be null");
    
    // Initialize master Wavelet if necessary    
    ObservableWavelet masterWavelet = wave.getWavelet(WaveletId.of(domain, MASTER_DATA_WAVELET_NAME));
    if (masterWavelet == null) {
      masterWavelet = wave.createWavelet(WaveletId.of(domain, MASTER_DATA_WAVELET_NAME));
    }
     
    MutableCObject object = new MutableCObject(idGenerator, domain, wave);
    object.initialize();
    
    return object;
  }
  
  private MutableCObject(IdGenerator idGenerator, String domain, ObservableWaveView wave) {
    this.wave = wave;
    this.masterWavelet = wave.getWavelet(WaveletId.of(domain, MASTER_DATA_WAVELET_NAME));
    this.mapSerializer = new SubstrateMapSerializer();
    this.domain = domain;
    this.idGenerator = idGenerator;
  }
  
  /**
   * Perform initialization tasks: load root map
   */
  private void initialize() {
    root = loadMutableCMap(masterWavelet.getId(), ROOT_SUBSTRATE_ID);
  }

  /**
   * Check if the node is a mutable/wave based node or
   * it needs to be adapted because it is, for example, a pure java object. 
   * 
   * If node is already attached to a mutable node, this method should raise
   * an exception.
   * 
   * 
   * 
   * @param node
   * @param newContainer 
   * @return
   */
  protected MutableCNode asMutableNode(CNode node, boolean newContainer) {
    
    if (node instanceof MutableCNode)
      return (MutableCNode) node;
    
    ObservableWavelet containerWavelet = masterWavelet;
    
    if (newContainer) {
      containerWavelet = createContainerWavelet();
    }
    
    return toMutableNode(node, containerWavelet);    
  }
  

  private MutableCNode toMutableNode(CNode node, ObservableWavelet containerWavelet) {
        
    if (node instanceof LocalCMap) {
      LocalCMap localMap = (LocalCMap) node;
      MutableCMap mutableMap = loadMutableCMap(containerWavelet.getId(), createSubstrateId("m", containerWavelet));
      
      for (String k: localMap.keySet()) {
        CNode v = localMap.get(k);
        MutableCNode mv = toMutableNode(v, containerWavelet);
        mutableMap.put(k, mv);
      }
      
      return mutableMap;
    }
    
    if (node instanceof CPrimitive) {
      return (CPrimitive) node;
    }
    
    return null;
  }
  
  
  private ObservableWavelet createContainerWavelet() {
    return wave.createWavelet();
  }
  
  private String createSubstrateId(String prefix, Wavelet containerWavelet) {
    return prefix+"+"+idGenerator.newUniqueToken();
  }
  
  
  
  private MutableCMap loadMutableCMap(WaveletId containerId, String substrateId) {
   
    ObservableWavelet substrateContainer = wave.getWavelet(containerId);
    ObservableDocument document = substrateContainer.getDocument(substrateId);    
    DefaultDocEventRouter router = DefaultDocEventRouter.create(document);
    
    E mapElement = DocHelper.getElementWithTagName(document, MAP_TAG);
    if (mapElement == null) {
      mapElement = document.createChildElement(document.getDocumentElement(), MAP_TAG,
          Collections.<String, String> emptyMap());
    }
    
    ObservableBasicMap<String, MutableCNode> map =
        DocumentBasedBasicRMap.create(router, 
            mapElement, 
            Serializer.STRING,
            mapSerializer, 
            MAP_ENTRY_TAG, 
            MAP_ENTRY_KEY_ATTR, 
            MAP_ENTRY_VALUE_ATTR);

    
    return MutableCMap.create(this, containerId, substrateId, map);
     
  }
  
  public MutableCMap getRoot() {
    return root;
  }
  
  
  
}
