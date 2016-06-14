package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableBoolean;
import org.swellrt.model.ReadableNumber;
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

  @Override
  public UnmutableMap asMap() {
    return null;
  }


  @Override
  public UnmutableString asString() {
    return this;
  }


  @Override
  public UnmutableList asList() {
    return null;
  }


  @Override
  public UnmutableText asText() {
    return null;
  }

  @Override
  public UnmutableFile asFile() {
    return null;
  }

  @Override
  public ReadableNumber asNumber() {
    return null;
  }

  @Override
  public ReadableBoolean asBoolean() {
    return null;
  }

}
