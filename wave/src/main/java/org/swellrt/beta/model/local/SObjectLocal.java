package org.swellrt.beta.model.local;


import java.util.HashSet;
import java.util.Set;

import org.swellrt.beta.model.IllegalCastException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SObject;
import org.swellrt.beta.model.js.Proxy;
import org.swellrt.beta.model.js.SMapProxyHandler;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "LocalObject")
public class SObjectLocal implements SObject {
  
  
  private final Set<String> participants;
  private final SMap delegateMap;

  public static SObjectLocal create() {
    return new SObjectLocal(new SMapLocal());
  }
  
  @JsIgnore
  public SObjectLocal(SMap map) {
    this.delegateMap = map;
    this.participants = new HashSet<String>();
  }
  
  @Override
  public Object get(String key) {
    return delegateMap.get(key);
  }

  @JsIgnore
  @Override
  public SMap put(String key, SNode value) {
    delegateMap.put(key, value);
    return this;
  }

  @Override
  public SMap put(String key, Object object) throws IllegalCastException {
    delegateMap.put(key, object);
    return this;
  }

  @Override
  public void remove(String key) {
    delegateMap.remove(key);
  }

  
  @Override
  public boolean has(String key) {
    return delegateMap.has(key);
  }

  @Override
  public String[] keys() {
    return delegateMap.keys();
  }

  @Override
  public Object asNative() {
    return new Proxy(delegateMap, new SMapProxyHandler());
  }


  @Override
  public String getId() {   
    return null;
  }

  @Override
  public void addParticipant(String participantId) {
    participants.add(participantId);
    
  }

  @Override
  public void removeParticipant(String participantId) {
    participants.remove(participantId);
  }

  @Override
  public String[] getParticipants() {
    return participants.toArray(new String[participants.size()]);
  }

  @Override
  public SNode getNode(String key) {
    return delegateMap.getNode(key);
  }

  @Override
  public void clear() {
    delegateMap.clear();
  }

  @Override
  public boolean isEmpty() {
    return delegateMap.isEmpty();
  }

  @Override
  public int size() {
    return delegateMap.size();
  }
  
  
}
