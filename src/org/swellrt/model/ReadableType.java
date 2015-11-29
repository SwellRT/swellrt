package org.swellrt.model;


/**
 * Class hierarchy declaring public contract of Type classes. Used to keep
 * aligned features between generic and unmutable versions of SwellRT data
 * models.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface ReadableType extends ReadableTypeVisitable {

  public abstract ReadableMap asMap();

  public abstract ReadableString asString();

  public abstract ReadableList<?> asList();

  public abstract ReadableText asText();


}
