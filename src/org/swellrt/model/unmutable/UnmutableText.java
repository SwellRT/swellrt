package org.swellrt.model.unmutable;

import org.swellrt.model.ReadableText;
import org.swellrt.model.TypeVisitor;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.Document;

public class UnmutableText implements ReadableText {

  private Document document;

  protected UnmutableText(Document document) {
    this.document = document;
  }

  @Override
  public void accept(TypeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public int getSize() {
    return document.size();
  }

  @Override
  public String getXml() {
    return document.toXmlString();
  }

  @Override
  public Iterable<AnnotationInterval<String>> getAllAnnotations(int start, int end) {
    return document.annotationIntervals(start, end, null);
  }

  @Override
  public String getAnnotation(int location, String key) {
    return document.getAnnotation(location, key);
  }

}
