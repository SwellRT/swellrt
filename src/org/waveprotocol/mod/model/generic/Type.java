package org.waveprotocol.mod.model.generic;

import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;

public abstract class Type {

  public static Type createInstance(String type, Model model) {

    Type typeInstance = null;

    if (ListType.TYPE.equals(type)) {

      typeInstance = new ListType(model);

    } else if (MapType.TYPE.equals(type)) {

      typeInstance = new MapType(model);

    } else if (StringType.TYPE.equals(type)) {

      typeInstance = new StringType(model, "");

    }

    return typeInstance;
  }

  protected abstract TypeInitializer getTypeInitializer();

  protected abstract String getPrefix();

  protected abstract void attachToString(int indexStringPos,
      ObservableBasicValue<String> observableValue);

  protected abstract void attachToParent(String documentId, ObservableDocument parentDoc,
      Doc.E parentElement);

  protected abstract void attachToModel();

  protected abstract void attachToModel(String documentId);

  protected abstract boolean isAttached();

  protected abstract String serializeToModel();
}
