package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableFile;
import org.swellrt.model.ReadableTypeVisitable;
import org.swellrt.model.ReadableTypeVisitor;
import org.waveprotocol.wave.media.model.AttachmentId;

public class UnmutableFile implements ReadableFile, ReadableTypeVisitable {


  AttachmentId value;

  protected UnmutableFile(AttachmentId value) {
    this.value = value;
  }

  @Override
  public void accept(ReadableTypeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public AttachmentId getValue() {
    return value;
  }

  @Override
  public String toString() {
    return value.serialise();
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
    return this;
  }

}
