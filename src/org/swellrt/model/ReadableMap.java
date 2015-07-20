package org.swellrt.model;

import java.util.Set;

public interface ReadableMap extends ReadableType {

  ReadableType get(String key);

  Set<String> keySet();


}
