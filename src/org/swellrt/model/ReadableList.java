package org.swellrt.model;

public interface ReadableList extends ReadableType {

  ReadableType get(int index);

  int size();

  Iterable<ReadableType> getValues();
}
