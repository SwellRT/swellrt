package org.waveprotocol.box.server.swell;

import org.waveprotocol.wave.model.document.indexed.SimpleAnnotationSet;

/**
 * A blip (or document) contribution is a range in a document together with the
 * participant id, author of that range's content.
 * <p>
 * <br>
 * A contribution is an analog concept to annotation, but just referencing an
 * author.
 *
 */
public interface ReadableBlipContributions {

  /** Returns all contributions of the blip as a annotation set */
  public SimpleAnnotationSet getAnnotations();

}
