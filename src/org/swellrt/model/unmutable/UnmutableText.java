package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableText;
import org.swellrt.model.TypeVisitor;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;

import java.util.Set;

public class UnmutableText implements ReadableText {

  private final ReadableBlipData blipData;

  protected UnmutableText(ReadableBlipData blipData) {
    this.blipData = blipData;
  }

  @Override
  public void accept(TypeVisitor visitor) {
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


}
