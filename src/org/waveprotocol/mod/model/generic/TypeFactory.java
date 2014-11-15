package org.waveprotocol.mod.model.generic;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.Preconditions;

import java.util.Map;

public class TypeFactory implements
    org.waveprotocol.wave.model.adt.docbased.Factory<Doc.E, Type, TypeInitializer> {


  private Model model;

  protected TypeFactory(Model model) {
    this.model = model;
  }

  @Override
  public Type adapt(DocumentEventRouter<? super E, E, ?> router, E element) {

    Map<String, String> attributes = router.getDocument().getAttributes(element);
    Preconditions.checkArgument(attributes != null,
        "Adapting an element to Type but attributes not found");

    String type = attributes.get("t");
    Preconditions.checkArgument(type != null,
        "Adapting an element to Type but 't' attribute not found");

    Type typeInstance = Type.createInstance(type, model);
    typeInstance.attachToParent(null, (ObservableDocument) router.getDocument(), element);

    return typeInstance;
  }

  @Override
  public org.waveprotocol.wave.model.adt.docbased.Initializer createInitializer(
      final TypeInitializer initialState) {

    return new org.waveprotocol.wave.model.adt.docbased.Initializer() {

      @Override
      public void initialize(Map<String, String> target) {
        target.put("t", initialState.getType());
        if (initialState.getSimpleValue() != null) {
          target.put("v", initialState.getSimpleValue());
        }
      }

    };
}

}