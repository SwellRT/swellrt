package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableBoolean;
import org.swellrt.model.ReadableNumber;
import org.swellrt.model.ReadableTypeVisitable;
import org.swellrt.model.ReadableTypeVisitor;

public class UnmutableBoolean implements ReadableBoolean, ReadableTypeVisitable {


  Boolean value;


  protected UnmutableBoolean(String value) {

    try {
      this.value = Boolean.parseBoolean(value);
    } catch (Exception e) {
      this.value = null;
    }
  }

  @Override
  public void accept(ReadableTypeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public Boolean getValue() {
    return value;
  }

  @Override
  public String toString() {
    return Boolean.toString(value);
  }

  @Override
  public UnmutableMap asMap() {
    return null;
  }


  @Override
  public UnmutableString asString() {
    return null;
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
    return this;
  }


}
