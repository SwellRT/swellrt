package org.swellrt.model;

public interface ReadableTypeFactory {

  public ReadableType get(String documentId);

  public ReadableType get(String documentId, String tag, String type);
}
