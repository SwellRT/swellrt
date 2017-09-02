package org.swellrt.beta.model.wave.mutable;

import java.util.HashMap;
import java.util.Set;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.SUtils;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.js.HasJsProxy;
import org.swellrt.beta.model.js.Proxy;
import org.swellrt.beta.model.js.SMapProxyHandler;
import org.swellrt.beta.model.wave.SubstrateId;
import org.waveprotocol.wave.model.adt.ObservableBasicMap;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;



public class SWaveMap extends SWaveNodeContainer implements SMap, HasJsProxy, ObservableBasicMap.Listener<String, SWaveNode> {


  public static SWaveMap create(SWaveNodeManager nodeManager, SubstrateId substrateId, ObservableBasicMap<String, SWaveNode> map) {
    return new SWaveMap(nodeManager, substrateId, map);
  }

  /** the underlying wave map */
  private final ObservableBasicMap<String, SWaveNode> map;

  /** cache of SNodeRemote instances in the map */
  private final HashMap<String, SWaveNode> cache;

  private Proxy proxy;


  protected SWaveMap(SWaveNodeManager nodeManager, SubstrateId substrateId, ObservableBasicMap<String, SWaveNode> map) {
    super(substrateId, nodeManager);
    this.cache = new HashMap<String, SWaveNode>();
    this.map = map;
    this.map.addListener(this);
  }


  @Override
  protected void clearCache() {
    cache.clear();
    for (SWaveNode n: cache.values())
      if (n instanceof SWaveNodeContainer)
        ((SWaveNodeContainer) n).clearCache();
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

    getNodeManager().check();
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
  public SNode pick(String key) throws SException {

    // Don't call check here, this is a read operation!

    if (!map.keySet().contains(key))
      return null;

    SWaveNode node = null;

    if (!cache.containsKey(key)) {
      node = map.get(key);

      if (node instanceof SPrimitive) {
        ((SPrimitive) node).setNameKey(key);
      }

      // This should be always true!
      if (node instanceof SWaveNode)
       node.attach(this); // lazily set parent

      cache.put(key, node);

    } else {
      node = cache.get(key);
    }

    getNodeManager().checkReadable(node);

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
    SUtils.isValidMapKey(key);
    check();
    getNodeManager().checkWritable(pick(key));
    SWaveNode remoteValue = getNodeManager().transformToWaveNode(value, this);
    map.put(key, remoteValue);
    if (cache.containsKey(key)) {
      cache.remove(key); // use this to clear cache when value is an existen
                         // SNode
    } else {
      cache.put(key, remoteValue);
    }
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
    getNodeManager().checkWritable(pick(key));

    if (!map.keySet().contains(key))
      return;

    SWaveNode nr = map.get(key);
    if (nr instanceof SWaveNodeContainer) {
      SWaveNodeContainer nrc = (SWaveNodeContainer) nr;
      nrc.deattach();
    }

    map.remove(key);
    cache.remove(key);

    getNodeManager().deleteFromStore(nr);
    getNodeManager().emptySubstrate(nr.getSubstrateId());
  }

  @Override
  public void removeSafe(String key) throws SException {
    check();
    getNodeManager().checkWritable(pick(key));

    if (!map.keySet().contains(key))
      return;

    SWaveNode nr = map.get(key);
    if (nr instanceof SWaveNodeContainer) {
      SWaveNodeContainer nrc = (SWaveNodeContainer) nr;
      nrc.deattach();
    }

    map.remove(key);
    cache.remove(key);

    getNodeManager().deleteFromStore(nr);
    // don't call getNodeManager().emptySubstrate();
  }

  @Override
  public int size() {
    return map.keySet().size();
  }

  public Object js() {
    if (proxy == null)
      proxy = new Proxy(this, new SMapProxyHandler());
    return proxy;
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
  public void onEntrySet(String key, SWaveNode oldValue, SWaveNode newValue) {
     //System.out.println("Map("+this.toString()+") onEntrySet [key="+key+" oldValue="+(oldValue != null ? oldValue : "null")+ " newValue="+(newValue != null ? newValue : "null")+"]");
     try {

       check(); // Ignore events if state is inconsistent

       SNode eventValue = null;
       int eventType = -1;

       SNode cachedValue = cache.remove(key); // refresh cache in any case

       // on removed
       if (newValue == null) {
         eventType = SEvent.REMOVED_VALUE;
         if (cachedValue instanceof SWaveNodeContainer)
             ((SWaveNodeContainer) cachedValue).deattach();

         eventValue = cachedValue;

       // on added
       } else if (oldValue == null) {
         eventType = SEvent.ADDED_VALUE;
         // ensure attach the node, set keyname
         eventValue = pick(key);

       // on updated
       } else {
         eventType = SEvent.UPDATED_VALUE;
         // ensure attach the node, set keyname
         eventValue = pick(key);

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

  @SuppressWarnings("rawtypes")
  @JsIgnore
  @Override
  public void accept(SVisitor visitor) {
    visitor.visit(this);
  }

  //
  // -----------------------------------------------------
  //

  @Override
  public void set(String path, Object value) {
    SNode.set(this, path, value);
  }

  @Override
  public void push(String path, Object value, @JsOptional Object index) {
    SNode.push(this, path, value, index);
  }

  @Override
  public Object pop(String path) {
    return SNode.pop(this, path);
  }

  @Override
  public int length(String path) {
    return SNode.length(this, path);
  }

  @Override
  public boolean contains(String path, String property) {
    return SNode.contains(this, path, property);
  }

  @Override
  public void delete(String path) {
    SNode.delete(this, path);
  }

  @Override
  public Object get(String path) {
    return SNode.get(this, path);
  }

  @Override
  public SNode node(String path) throws SException {
    return SNode.node(this, path);
  }
}
