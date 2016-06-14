package org.swellrt.model;

import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

public interface ReadableText extends ReadableType {

  int getSize();

  String getXml();

  String getText(int start, int end);

  Iterable<AnnotationInterval<String>> getAllAnnotations(int start, int end);

  String getAnnotation(int location, String key);

  String getDocumentId();

  ParticipantId getAuthor();

  long getLastUpdateTime();

  Set<ParticipantId> getContributors();

}
