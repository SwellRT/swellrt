package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableText;
import org.swellrt.model.ReadableTypeVisitable;
import org.swellrt.model.ReadableTypeVisitor;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;

import java.util.Set;

public class UnmutableText implements ReadableText, ReadableTypeVisitable {

  private final ReadableBlipData blipData;

  protected UnmutableText(ReadableBlipData blipData) {
    this.blipData = blipData;
  }

  @Override
  public void accept(ReadableTypeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public int getSize() {
    return blipData.getContent().getMutableDocument().size();
  }

  @Override
  public String getXml() {
    return blipData.getContent().getMutableDocument().toXmlString();
  }

  @Override
  public Iterable<AnnotationInterval<String>> getAllAnnotations(int start, int end) {
    return blipData.getContent().getMutableDocument().annotationIntervals(start, end, null);
  }

  @Override
  public String getAnnotation(int location, String key) {
    return blipData.getContent().getMutableDocument().getAnnotation(location, key);
  }

  @Override
  public String getDocumentId() {
    return blipData.getId();
  }

  @Override
  public ParticipantId getAuthor() {
    return blipData.getAuthor();
  }

  @Override
  public long getLastUpdateTime() {
    return blipData.getLastModifiedTime();
  }

  @Override
  public Set<ParticipantId> getContributors() {
    return blipData.getContributors();
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
    return this;
  }

}
