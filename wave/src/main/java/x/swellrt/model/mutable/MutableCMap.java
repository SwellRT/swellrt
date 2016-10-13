package x.swellrt.model.mutable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.waveprotocol.wave.model.adt.ObservableBasicMap;

import x.swellrt.model.CMap;
import x.swellrt.model.CNode;

public class MutableCMap implements CMap, MutableCNode {
  
  
  public static MutableCMap create(MutableCObject controller, ObservableBasicMap<String, MutableCNode> map, String substrateId) {
    return new MutableCMap(controller, map, substrateId);
  }
  
  private String substrateId;
  private MutableCObject controller;
  private ObservableBasicMap<String, MutableCNode> map;  
  private HashMap<String, CNode> cache;
  
  
  
  protected MutableCMap(MutableCObject controller, ObservableBasicMap<String, MutableCNode> map, String substrateId) {
    this.cache = new HashMap<String, CNode>();
    this.map = map;
    this.controller = controller;
    this.substrateId = substrateId;
  }
  
  protected String getSubstrateId() {
    return substrateId;
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
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Set<java.util.Map.Entry<String, CNode>> entrySet() {
    // TODO Auto-generated method stub
    return null;
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
    MutableCNode mutableValue =  controller.asMutable(value);
    map.put(key, mutableValue);
    cache.put(key, mutableValue);
    return mutableValue;
  }

  @Override
  public void putAll(Map<? extends String, ? extends CNode> m) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public CNode remove(Object key) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public int size() {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Collection<CNode> values() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public CNode put(String key, String value) {
    // TODO Auto-generated method stub
    return null;
  }

}
