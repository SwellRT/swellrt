package org.swellrt.model;

import org.waveprotocol.wave.model.document.AnnotationInterval;

public interface ReadableText extends ReadableType {

  int getSize();

  String getXml();

  Iterable<AnnotationInterval<String>> getAllAnnotations(int start, int end);

  String getAnnotation(int location, String key);

}
