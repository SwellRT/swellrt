package org.swellrt.beta.model.remote;

import java.util.HashMap;
import java.util.Set;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.js.HasJsProxy;
import org.swellrt.beta.model.js.Proxy;
import org.swellrt.beta.model.js.SMapProxyHandler;
import org.waveprotocol.wave.model.adt.ObservableBasicMap;



public class SMapRemote extends SNodeRemoteContainer implements SMap, HasJsProxy, ObservableBasicMap.Listener<String, SNodeRemote> {
  
  
  public static SMapRemote create(SObjectRemote object, SubstrateId substrateId, ObservableBasicMap<String, SNodeRemote> map) {
    return new SMapRemote(object, substrateId, map);
  }
  
  /** the underlying wave map */
  private final ObservableBasicMap<String, SNodeRemote> map;  
  
  /** cache of SNodeRemote instances in the map */ 
  private final HashMap<String, SNodeRemote> cache;
  
  private Proxy proxy;

      
  protected SMapRemote(SObjectRemote object, SubstrateId substrateId, ObservableBasicMap<String, SNodeRemote> map) {
    super(substrateId, object);
    this.cache = new HashMap<String, SNodeRemote>();
    this.map = map;
    this.map.addListener(this);
  }
  
  
  @Override
  protected void clearCache() {
    cache.clear();
    for (SNodeRemote n: cache.values())
      if (n instanceof SNodeRemoteContainer)
        ((SNodeRemoteContainer) n).clearCache();
  }
  
  /**
   * Perform a sanity check. Raise an exception if this node
   * can't perform the operation or the container object is
   * in a bad state.
   * <p>
   * Don't use it for read operations to avoid client frameworks
   * (like angular2) receiving exceptions in templates.
   */
  protected void check() throws SException {
    if (this.getParent() == null)
      throw new SException(SException.NOT_ATTACHED_NODE);
    
    getObject().check();
  }
  
  @Override
  public void clear() throws SException {
    check();
    
    Set<String> keys = map.keySet();
    for (String k: keys) {
      map.remove(k);
    }
    
    cache.clear();
  }

  @Override
  public boolean has(String key) throws SException {
    check();
    return map.keySet().contains(key);
  }

  @Override 
  public SNode getNode(String key) throws SException {

    // Don't call check here, this is a read operation!
    
    if (!map.keySet().contains(key))
      return null;
    
    SNodeRemote node = null;
    
    if (!cache.containsKey(key)) {
      node = map.get(key);
      
      if (node instanceof SPrimitive) {
        ((SPrimitive) node).setNameKey(key);
      }
      
      // This should be always true!
      if (node instanceof SNodeRemote)
       ((SNodeRemote) node).attach(this); // lazily set parent
      
      cache.put(key, node);
    
    } else {
      node = cache.get(key);
    }
    
    return node;
  }

  @Override
  public Object get(String key) throws SException {

    SNode node = getNode(key);
    getObject().checkReadable(node);
    
    if (node == null)
      return null;
        
    if (node instanceof SPrimitive)
      return ((SPrimitive) node).get();
    
    return node;
  }

  @Override
  public boolean isEmpty() {
    return map.keySet().isEmpty();
  }

  @Override
  public String[] keys() throws SException {
    check();
    return map.keySet().toArray(new String[map.keySet().size()]);
  }

  @Override
  public SMap put(String key, SNode value) throws SException {
    check();
    getObject().checkWritable(getNode(key));
    SNodeRemote remoteValue =  getObject().asRemote(value, this, false);
    map.put(key, remoteValue);
    cache.put(key, remoteValue);
    return this;
  }

  @Override
  public SMap put(String key, Object value) throws SException {
     SNode node = SUtils.castToSNode(value);
     return put(key, node);
  }
 
 
  @Override
  public void remove(String key) throws SException {
    check();
    getObject().checkWritable(getNode(key));
    
    if (!map.keySet().contains(key))
      return;
    
    SNodeRemote nr = map.get(key);
    if (nr instanceof SNodeRemoteContainer) {
      SNodeRemoteContainer nrc = (SNodeRemoteContainer) nr;
      nrc.deattach();
    }   

    map.remove(key);
    cache.remove(key);
    
    getObject().deleteNode(nr);    
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
  
  //
  // Event handling
  //
  

  @Override
  public void onEntrySet(String key, SNodeRemote oldValue, SNodeRemote newValue) {
     //System.out.println("Map("+this.toString()+") onEntrySet [key="+key+" oldValue="+(oldValue != null ? oldValue : "null")+ " newValue="+(newValue != null ? newValue : "null")+"]");
     try {
       
       check(); // Ignore events if state is inconsistent
       
       SNode eventValue = null;
       int eventType = -1;
      
       SNode cachedValue = cache.remove(key); // refresh cache in any case
       
       // on removed
       if (newValue == null) {
         eventType = SEvent.REMOVED_VALUE;        
         if (cachedValue instanceof SNodeRemoteContainer)
             ((SNodeRemoteContainer) cachedValue).deattach();           

         eventValue = cachedValue;
       
       // on added
       } else if (oldValue == null) {
         eventType = SEvent.ADDED_VALUE;
         // ensure attach the node, set keyname       
         eventValue = getNode(key); 
         
       // on updated  
       } else {
         eventType = SEvent.UPDATED_VALUE;
         // ensure attach the node, set keyname
         eventValue = getNode(key);
         
       }
      
       SEvent e = new SEvent(eventType, this, key, eventValue);
       triggerEvent(e);
       
     } catch (SException e) {       
       // Swallow it
     }
     
  }

  @Override
  public String toString() {
    return "SMapRemote ["+getSubstrateId()+"]";
  }
}
