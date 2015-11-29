package org.swellrt.model;


public interface ReadableList<T> extends ReadableType {

  T get(int index);

  int size();

  Iterable<T> getValues();
}
