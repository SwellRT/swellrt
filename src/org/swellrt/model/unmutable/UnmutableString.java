package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableString;
import org.swellrt.model.TypeVisitor;

public class UnmutableString implements ReadableString {


  String value;

  protected UnmutableString(String value) {
    this.value = value;
  }

  @Override
  public void accept(TypeVisitor visitor) {
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
