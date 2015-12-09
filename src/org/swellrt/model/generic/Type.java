package org.swellrt.model.generic;

import org.swellrt.model.ReadableType;

/**
 * Parent abstract class for data types stored in a SwellRT wavelet.
 *
 *
 * @author pablojan@gmail (Pablo Ojanguren)
 *
 */
public abstract class Type implements ReadableType {

  /**
   * Deserialize a Type instance based on its parent instance a its substrate
   * document or primitive value reference.
   *
   * @param parent Parent type instance.
   * @param ref a substrate document id or reference of a primitive value
   * @return
   */
  protected static Type deserialize(Type parent, String ref) {

    Type instance = null;

    if (ref.startsWith(StringType.PREFIX)) {
      instance = StringType.deserialize(parent, ref.substring(StringType.PREFIX.length() + 1));

    } else if (ref.startsWith(ListType.PREFIX)) {
      instance = ListType.deserialize(parent, ref);

    } else if (ref.startsWith(MapType.PREFIX)) {
      instance = MapType.deserialize(parent, ref);

    } else if (ref.startsWith(TextType.PREFIX)) {
      instance = TextType.deserialize(parent, ref);
    }

    return instance;
  }

  /** Attach a type instance to an existing substrate document. */
  protected abstract void attach(Type parent, String substrateDocumentIdOrValueIndex);


  /** Attach a type instance to a new substrate document. */
  protected abstract void attach(Type parent);

  /** Remove the backend document */
  protected abstract void deattach();


  /** Provide a initializer of this Type instance to be added in a ListType. */
  protected abstract ListElementInitializer getListElementInitializer();

  /** Get the document prefix used by documents of this type. */
  protected abstract String getPrefix();

  /** Check if the instance is supported by a document. */
  protected abstract boolean isAttached();

  /**
   * Provide a serialized Id for referencing the instance from others instance
   * of the model, by now just for MapType.
   */
  protected abstract String serialize();

  /**
   * Set type instance path. To be invoked after {@link #attach(Type)} by parent
   * object.
   */
  protected abstract void setPath(String path);

  /** Check if this instance is a container of primitive values. */
  protected abstract boolean hasValuesContainer();

  /** Get the container of primitive values of this instance. Null otherwise. */
  protected abstract ValuesContainer getValuesContainer();

  /** Get the reference string of the value stored in this instance. */
  protected abstract String getValueReference(Type value);

  /**
   * Used to mark updates of a primitive value in the container, in order to
   * generate a convinient sequence of DocOps.
   */
  protected void markValueUpdate(Type value) {
    // Default no-op
  }

  /** Get the path of this instance in a collaborative object model */
  public abstract String getPath();

  /** Return the Id of the Wave document storing this Type instance */
  public abstract String getDocumentId();

  /** Return the Model object managing this Type instance */
  public abstract Model getModel();

  /** Return this instance's Type as String */
  public abstract String getType();

}
