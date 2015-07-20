package org.swellrt.model;

public interface TypeVisitor {

  void visit(ReadableModel instance);

  void visit(ReadableString instance);

  void visit(ReadableMap instance);

  void visit(ReadableList instance);

  void visit(ReadableText instance);


}
