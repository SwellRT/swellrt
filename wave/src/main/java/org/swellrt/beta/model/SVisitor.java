package org.swellrt.beta.model;

public interface SVisitor<T extends SNode> {

  public void visit(SPrimitive primitive);

  public void visit(SMap map);

  public void visit(SList<T> list);

  public void visit(SText text);

}
