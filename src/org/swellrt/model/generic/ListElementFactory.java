package org.swellrt.model.generic;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

public class ListElementFactory implements
    org.waveprotocol.wave.model.adt.docbased.Factory<Doc.E, Type, ListElementInitializer> {


  private Type parent;

  protected ListElementFactory(Type parent) {
    this.parent = parent;
  }

  @Override
  public Type adapt(DocumentEventRouter<? super E, E, ?> router, E element) {

    Map<String, String> attributes = router.getDocument().getAttributes(element);
    Preconditions.checkArgument(attributes != null,
        "Adapting a list element to Type but attributes not found");

    String type = attributes.get(ListType.ATTR_LIST_ITEM_TYPE);
    Preconditions.checkArgument(type != null,
        "Adapting a list element to Type but attribute for type not found");

    String ref = attributes.get(ListType.ATTR_LIST_ITEM_REF);
    Preconditions.checkArgument(ref != null,
        "Adapting a list element to Type but attribute for reference not found");

    return Type.deserialize(parent, ref);
  }

  @Override
  public org.waveprotocol.wave.model.adt.docbased.Initializer createInitializer(
      final ListElementInitializer initialState) {

    return new org.waveprotocol.wave.model.adt.docbased.Initializer() {

      @Override
      public void initialize(Map<String, String> target) {
        target.put(ListType.ATTR_LIST_ITEM_TYPE, initialState.getType());
        if (initialState.getBackendId() != null) {
          target.put(ListType.ATTR_LIST_ITEM_REF, initialState.getBackendId());
        }
      }

    };
}

}