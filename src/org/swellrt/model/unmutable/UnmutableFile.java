package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableBoolean;
import org.swellrt.model.ReadableFile;
import org.swellrt.model.ReadableNumber;
import org.swellrt.model.ReadableTypeVisitable;
import org.swellrt.model.ReadableTypeVisitor;
import org.waveprotocol.wave.media.model.AttachmentId;

public class UnmutableFile implements ReadableFile, ReadableTypeVisitable {


  AttachmentId attachmentId;
  String contentType;

  protected UnmutableFile(AttachmentId attachmentId, String contentType) {
    this.attachmentId = attachmentId;
    this.contentType = contentType;
  }

  @Override
  public void accept(ReadableTypeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public AttachmentId getValue() {
    return attachmentId;
  }

  @Override
  public String toString() {
    return attachmentId.serialise()
        + (contentType != null && !contentType.isEmpty() ? "," + contentType : "");
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

  @Override
  public AttachmentId getFileId() {
    return attachmentId;
  }

  @Override
  public String getContentType() {
    return contentType;
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
