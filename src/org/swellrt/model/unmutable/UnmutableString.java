package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableString;
import org.swellrt.model.ReadableTypeVisitable;
import org.swellrt.model.ReadableTypeVisitor;

public class UnmutableString implements ReadableString, ReadableTypeVisitable {


  String value;

  protected UnmutableString(String value) {
    this.value = value;
  }

  @Override
  public void accept(ReadableTypeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value;
  }

}
