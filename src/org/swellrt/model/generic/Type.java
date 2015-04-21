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
    }

    return instance;
  }

  /**
   * Provides a independent document-based backend to this Type instance
   */
  protected abstract void attach(String docId);

  /**
   * Remove the backend document or document artifact
   */
  protected abstract void deattach();

  /**
   * Provides a document element backend to this Type instance
   * 
   * @param docEventRouter
   * @param parentElement
   */
  // protected abstract void attach(DocEventRouter docEventRouter, Doc.E
  // parentElement);

  protected abstract ListElementInitializer getListElementInitializer();

  protected abstract String getPrefix();

  protected abstract boolean isAttached();

  protected abstract String serializeToModel();

}
