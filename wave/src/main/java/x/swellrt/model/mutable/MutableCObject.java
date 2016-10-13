package x.swellrt.model.mutable;

import java.util.Collections;

import org.swellrt.model.adt.DocumentBasedBasicRMap;
import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.opbased.ObservableWaveView;

import x.swellrt.model.CNode;
import x.swellrt.model.local.java.LocalCMap;

public class MutableCObject {
  
  private static final String MASTER_DATA_WAVELET = "w+master";
  
  private static final String SEPARATOR = ":";
  private static final String MAP_TYPE_PREFIX = "m";
  private static final String STRING_TYPE_PREFIX  = "s";
  
  /**
   * Objects are encoded as map values with format "type:value"
   * 
   * 
   * 
   * @author pablojan
   *
   */
  public class SubstrateMapSerializer implements org.waveprotocol.wave.model.util.Serializer<MutableCNode> {

    
    @Override
    public String toString(MutableCNode x) {

      // Map -> m:blipid
      // List -> l:blipid
      // String -> s:<string>
      // Integer -> i:<integer>
      // Boolean -> b:(true|false)

      
      if (x instanceof MutableCMap) {
        MutableCMap map = (MutableCMap) x;        
        return MAP_TYPE_PREFIX+SEPARATOR+map.getSubstrateId();            
      }
      
      if (x instanceof MutableCPrimitive) {
        
        MutableCPrimitive p = (MutableCPrimitive) x;
        
        if (p.getType() == MutableCPrimitive.TYPE_STRING)
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
        Preconditions.checkArgument(parts.length > 1, "Unable to deserialize map from "+s);
        String substrateId = parts[1];        
        return loadMap(substrateId);
      }
      
      if (type.equals(STRING_TYPE_PREFIX)) {
        Preconditions.checkArgument(parts.length > 1, "Unable to deserialize primitive type from "+s);
        String stringValue = parts[1];     
        return new MutableCPrimitive(stringValue);
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
  
  private final ObservableWaveView wave;
  private ObservableWavelet masterWavelet;
  
  private SubstrateMapSerializer mapSerializer;
  
  /**
   * Get a MutableCObject instance from the underlying Wave view.
   * Initialize the Wave accordingly.
   * 
   */
  public static MutableCObject ofWave(String domain, ObservableWaveView wave) {
    
    Preconditions.checkArgument(domain != null && !domain.isEmpty(), "Domain is not provided");
    Preconditions.checkArgument(wave != null, "Wave can't be null");
    
    // Initialize Wave if necessary    
    ObservableWavelet masterWavelet = wave.getWavelet(WaveletId.of(domain, MASTER_DATA_WAVELET));
    if (masterWavelet == null) {
      masterWavelet = wave.createWavelet(WaveletId.of(domain, MASTER_DATA_WAVELET));
      // TODO check creator and participant rules
    }
    
    return new MutableCObject(domain, wave);
  }
  
  private MutableCObject(String domain, ObservableWaveView wave) {
    this.wave = wave;
    this.masterWavelet = wave.getWavelet(WaveletId.of(domain, MASTER_DATA_WAVELET));
    this.mapSerializer = new SubstrateMapSerializer();
  }
  
  
  
  /**
   * Check if the node is a mutable/wave based node or
   * it needs to be adapted because it is, for example, a pure java object. 
   * 
   * If node is already attached to a mutable node, this method should raise
   * an exception
   * 
   * @param node
   * @return
   */
  protected MutableCNode asMutable(CNode node) {
    
    if (node instanceof MutableCNode)
      return (MutableCNode) node;
    
    if (node instanceof LocalCMap) {
      LocalCMap localMap = (LocalCMap) node;
      MutableCMap mutableMap = loadMap(generateSubstrateId());
      
      for (String k: localMap.keySet()) {
        CNode v = localMap.get(k);
        MutableCNode mv = asMutable(v);
        mutableMap.put(k, mv);
      }
      
      return mutableMap;
    }
    
      
    
    return null;
  }
  
  private String generateSubstrateId() {
    return null;
  }
  
  
  private MutableCMap loadMap(String substrateId) {
   
    ObservableDocument document = wavelet.getDocument(substrateId);    
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

    
    return MutableCMap.create(this, map, substrateId);
     
  }
  
  
  
  public static void main() {
  
    /*
     
      Client.open(id) -> wave -> MutableCObject.create(wave);
      
      
      
      wavelet <- wave.getWavelet("w+data");
      mapRoot <- wavelet.get("m+root");
            
      mutableMapRoot <- loadMap(mapRoot);
      
     */
    
   
    
  }
  
  
}
