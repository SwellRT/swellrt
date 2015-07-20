package org.swellrt.model;

public interface ReadableType {

  void accept(TypeVisitor visitor);

}
