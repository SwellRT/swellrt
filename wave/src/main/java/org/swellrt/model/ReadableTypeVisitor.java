package org.swellrt.model;


public interface ReadableTypeVisitor {

  void visit(ReadableModel instance);

  void visit(ReadableString instance);

  void visit(ReadableMap instance);

  void visit(ReadableList<? extends ReadableType> instance);

  void visit(ReadableText instance);

  void visit(ReadableFile instance);

  void visit(ReadableNumber instance);

  void visit(ReadableBoolean instance);

}
