package org.swellrt.model;

public interface ReadableTypeVisitable {

  public void accept(ReadableTypeVisitor visitor);

}
