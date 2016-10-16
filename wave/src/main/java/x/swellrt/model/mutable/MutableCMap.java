package x.swellrt.model.mutable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.id.WaveletId;

import x.swellrt.model.CMap;
import x.swellrt.model.CNode;
import x.swellrt.model.CPrimitive;
import x.swellrt.model.IllegalValueConversionException;

public class MutableCMap implements CMap, MutableCNode {
  
  
  public static MutableCMap create(MutableCObject object, WaveletId containerId, String substrateId, ObservableBasicMap<String, MutableCNode> map) {
    return new MutableCMap(object, containerId, map, substrateId);
  }
  
  private final WaveletId containerId;
  private final String substrateId;
  private final MutableCObject object;
  private final ObservableBasicMap<String, MutableCNode> map;  
  private final HashMap<String, CNode> cache;
  
  
  
  protected MutableCMap(MutableCObject object, WaveletId containerId, ObservableBasicMap<String, MutableCNode> map, String substrateId) {
    this.cache = new HashMap<String, CNode>();
    this.map = map;
    this.object = object;
    this.substrateId = substrateId;
    this.containerId = containerId;
  }
  
  protected String getSubstrateId() {
    return substrateId;
  }
  
  protected WaveletId getContainerId() {
    return containerId;
  }
  
  
  protected void clearCache() {
    cache.clear();
  }
  

  @Override
  public void clear() {
    
    Set<String> keys = map.keySet();
    for (String k: keys) {
      map.remove(k);
    }
    
    cache.clear();
  }

  @Override
  public boolean containsKey(Object key) {
    return map.keySet().contains(key);
  }

  @Override
  public boolean containsValue(Object value) {
    throw new UnsupportedOperationException("Not implemented to avoid performace issues");
  }

  @Override
  public Set<java.util.Map.Entry<String, CNode>> entrySet() {
    throw new UnsupportedOperationException("Not implemented to avoid performace issues");
  }

  @Override
  public CNode get(Object key) {
    
    if (!(key instanceof String))
      return null;
    
    String skey = (String) key;
    
    if (!map.keySet().contains(skey))
      return null;
    
    if (!cache.containsKey(skey)) {
      MutableCNode node = map.get(skey);      
      cache.put(skey, node);
    }
    
    return cache.get(skey);
  }

  @Override
  public boolean isEmpty() {
    return map.keySet().isEmpty();
  }

  @Override
  public Set<String> keySet() {
    return map.keySet();
  }

  @Override
  public CNode put(String key, CNode value) {
    MutableCNode mutableValue =  object.asMutableNode(value, false);
    map.put(key, mutableValue);
    cache.put(key, mutableValue);
    return mutableValue;
  }

  @Override
  public CNode put(String key, String value) {
     return put(key, new CPrimitive(value));
  }
  
  @Override
  public CNode put(String key, int value) {
    return put(key, new CPrimitive(value));
  }

  @Override
  public CNode put(String key, double value) {
    return put(key, new CPrimitive(value));  }

  @Override
  public CNode put(String key, boolean value) {
    return put(key, new CPrimitive(value));
  }
  
  
  @Override
  public void putAll(Map<? extends String, ? extends CNode> m) {
    throw new UnsupportedOperationException("Not implemented to avoid performace issues");   
  }

  @Override
  public CNode remove(Object key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int size() {
    return map.keySet().size();
  }

  @Override
  public Collection<CNode> values() {
    throw new UnsupportedOperationException("Not implemented to avoid performace issues");
  }


  @Override
  public Map<String, CNode> asMap() {   
    return this;
  }

  @Override
  public List<CNode> asList() {
    throw new IllegalValueConversionException();
  }

  @Override
  public String asString() {
    throw new IllegalValueConversionException();
  }

  @Override
  public int asInteger() {
    throw new IllegalValueConversionException();
  }

  @Override
  public double asDouble() {
    throw new IllegalValueConversionException();
  }

  @Override
  public boolean asBoolean() {
    throw new IllegalValueConversionException();
  }


}
