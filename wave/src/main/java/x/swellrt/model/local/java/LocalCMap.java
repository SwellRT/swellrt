package x.swellrt.model.local.java;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import x.swellrt.model.CMap;
import x.swellrt.model.CNode;
import x.swellrt.model.CPrimitive;
import x.swellrt.model.IllegalValueConversionException;

public class LocalCMap implements CMap {
  
  private HashMap<String, CNode> delegate;

  public LocalCMap() {
    delegate = new HashMap<String, CNode>();
  }
  
  @Override
  public void clear() {
    delegate.clear();
    
  }

  @Override
  public boolean containsKey(Object arg0) {
    return delegate.containsKey(arg0);
  }

  @Override
  public boolean containsValue(Object arg0) {
    return containsValue(arg0);
  }

  @Override
  public Set<java.util.Map.Entry<String, CNode>> entrySet() {
    return delegate.entrySet();
  }

  @Override
  public CNode get(Object arg0) {
    return delegate.get(arg0);
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public Set<String> keySet() {
    return delegate.keySet();
  }

  @Override
  public CNode put(String arg0, CNode arg1) {
    return delegate.put(arg0, arg1);
  }

  @Override
  public void putAll(Map<? extends String, ? extends CNode> arg0) {
    delegate.putAll(arg0);   
  }

  @Override
  public CNode remove(Object arg0) {
    return delegate.remove(arg0);
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public Collection<CNode> values() {
    return delegate.values();
  }

  @Override
  public CNode put(String key, String value) {
    return delegate.put(key, new CPrimitive(value));    
  }
  

  @Override
  public CNode put(String key, int value) {
    return delegate.put(key, new CPrimitive(value));    
  }

  @Override
  public CNode put(String key, double value) {
    return delegate.put(key, new CPrimitive(value));    
  }

  @Override
  public CNode put(String key, boolean value) {
    return delegate.put(key, new CPrimitive(value));    
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
