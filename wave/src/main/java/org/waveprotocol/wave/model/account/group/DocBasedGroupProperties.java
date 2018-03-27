package org.waveprotocol.wave.model.account.group;

import org.waveprotocol.wave.model.adt.ObservableStructuredValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedStructuredValue;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.Serializer;

public class DocBasedGroupProperties {

  public enum Property {
    NAME {
      @Override
      public String toString() {
        return "name";
      }
    }
  };

  private ObservableStructuredValue<Property, String> properties;

  public static <E> DocBasedGroupProperties create(DocumentEventRouter<? super E, E, ?> router,
      E container) {

    DocBasedGroupProperties groupInfo = new DocBasedGroupProperties(
        DocumentBasedStructuredValue.create(router, container, Serializer.STRING, Property.class));
    return groupInfo;
  }

  private DocBasedGroupProperties(ObservableStructuredValue<Property, String> properties) {
    this.properties = properties;
  }

  public String getName() {
    return properties.get(Property.NAME);
  }
  public void setName(String name) {
    if (name != null && !name.isEmpty())
      properties.set(Property.NAME, name);
  }

}
