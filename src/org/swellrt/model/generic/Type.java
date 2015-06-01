package org.swellrt.model.generic;



public abstract class Type {

  public static Type createInstance(String type, String backendId, Model model) {

    Type instance = null;

    if (StringType.PREFIX.equals(type)) {

      instance = StringType.createAndAttach(model, backendId);

    } else if (ListType.PREFIX.equals(type)) {

      instance = ListType.createAndAttach(model, backendId);

    } else if (MapType.PREFIX.equals(type)) {

      instance = MapType.createAndAttach(model, backendId);

    } else if (TextType.PREFIX.equals(type)) {

      instance = TextType.createAndAttach(model, backendId);
    }

    return instance;
  }

  /**
   * Attach the instance to a new or existing backend document when data is
   * actually stored.
   */
  protected abstract void attach(String docId);

  /**
   * Remove the backend document
   */
  protected abstract void deattach();


  /**
   * Provide a initializer of this Type instance to be added in a ListType.
   * 
   * @return
   */
  protected abstract ListElementInitializer getListElementInitializer();

  /**
   * Get the document prefix used by documents of this type.
   * 
   * @return
   */
  protected abstract String getPrefix();

  /**
   * Check if the instance is supported by a document.
   * 
   * @return
   */
  protected abstract boolean isAttached();

  /**
   * Provide a serialized Id for referencing the instance from others instance
   * of the model, by now just for MapType.
   * 
   * @return
   */
  protected abstract String serializeToModel();

}
