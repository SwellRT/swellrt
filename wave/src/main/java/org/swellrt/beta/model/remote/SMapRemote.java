package org.swellrt.beta.model.remote;

import java.util.HashMap;
import java.util.Set;

import org.swellrt.beta.model.IllegalCastException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.js.HasJsProxy;
import org.swellrt.beta.model.js.Proxy;
import org.swellrt.beta.model.js.SMapProxyHandler;
import org.waveprotocol.wave.model.adt.ObservableBasicMap;



public class SMapRemote implements SMap, SNodeRemote, HasJsProxy {
  
  
  public static SMapRemote create(SObjectRemote object, SubstrateId substrateId, ObservableBasicMap<String, SNodeRemote> map) {
    return new SMapRemote(object, substrateId, map);
  }
  
  /** the substrate id (wavelet/document id pair) */
  private final SubstrateId substrateId;
  
  /** the root object */
  private final SObjectRemote object;
  
  /** the underlying wave map */
  private final ObservableBasicMap<String, SNodeRemote> map;  
  
  /** cache of SNodeRemote instances in the map */ 
  private final HashMap<String, SNodeRemote> cache;
  
  private Proxy proxy;
  
  protected SMapRemote(SObjectRemote object, SubstrateId substrateId, ObservableBasicMap<String, SNodeRemote> map) {
    this.cache = new HashMap<String, SNodeRemote>();
    this.map = map;
    this.object = object;
    this.substrateId = substrateId;
  }
  
  protected SubstrateId getSubstrateId() {
    return substrateId;
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
  public boolean has(String key) {
    return map.keySet().contains(key);
  }

  @Override 
  public SNode getNode(String key) {
            
    if (!map.keySet().contains(key))
      return null;
    
    SNodeRemote node = null;
    
    if (!cache.containsKey(key)) {
      node = map.get(key);      
      cache.put(key, node);
    } else {
      node = cache.get(key);
    }
    
    return node;
  }

  @Override
  public Object get(String key) {
    
    SNode node = getNode(key);
    
    if (node == null)
      return null;
        
    if (node instanceof SPrimitive)
      return ((SPrimitive) node).getObject();
    
    return node;
  }

  @Override
  public boolean isEmpty() {
    return map.keySet().isEmpty();
  }

  @Override
  public String[] keys() {
    return map.keySet().toArray(new String[map.keySet().size()]);
  }

  @Override
  public SMap put(String key, SNode value) {
    SNodeRemote remoteValue =  object.asRemote(value, false);
    map.put(key, remoteValue);
    cache.put(key, remoteValue);
    return this;
  }

  @Override
  public SMap put(String key, Object value) throws IllegalCastException {
     SNode node = SUtils.castToPrimitive(value);
     return put(key, node);
  }
 
 
  @Override
  public void remove(String key) {   
    if (!map.keySet().contains(key))
      return;
    
    map.remove(key);
    cache.remove(key);
  }

  @Override
  public int size() {
    return map.keySet().size();
  }

  @Override
  public Object asNative() {
    return new Proxy(this, new SMapProxyHandler());
  }

  @Override
  public void setJsProxy(Proxy proxy) {
	  this.proxy = proxy;
  }

  @Override
  public Proxy getJsProxy() {
	  return this.proxy;
  }

}
