package org.swellrt.model.generic;

import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.ObservableElementList.Listener;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.adt.docbased.Factory;
import org.waveprotocol.wave.model.adt.docbased.Initializer;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.document.util.DocumentEventRouter;
import org.waveprotocol.wave.model.util.Serializer;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ValuesContainer {

  public static interface EventHandler {

    void run(String value);

  }

  public static final String TAG_VALUES = "values";
  public static final String TAG_ITEM = "i";
  public static final String ATTR_VALUE = "v";

  /**
   * Factory to add new primitive values to the list of values
   */
  private static final Factory<Doc.E, ObservableBasicValue<String>, String> VALUE_FACTORY =
      new Factory<Doc.E, ObservableBasicValue<String>, String>() {

        @Override
        public ObservableBasicValue<String> adapt(DocumentEventRouter<? super E, E, ?> router,
            E element) {
          return DocumentBasedBasicValue.create(router, element, Serializer.STRING, ATTR_VALUE);
        }

        @Override
        public Initializer createInitializer(final String initialState) {

          return new Initializer() {

            @Override
            public void initialize(Map<String, String> target) {
              target.put(ATTR_VALUE, initialState);
            }

          };
        }
      };


  public static ValuesContainer get(Document document, DocEventRouter router, Type parent) {

    Doc.E eltValues = DocHelper.getElementWithTagName(document, TAG_VALUES);
    if (eltValues == null) {
      eltValues =
          document.createChildElement(document.getDocumentElement(), TAG_VALUES,
              Collections.<String, String> emptyMap());
    }

    return new ValuesContainer(DocumentBasedElementList.create(router, eltValues, TAG_ITEM,
        VALUE_FACTORY), parent);
  }



  private final ObservableElementList<ObservableBasicValue<String>, String> values;
  private final Type parent;
  /** Handlers for events on particular indexes **/
  private final Map<Integer, EventHandler> eventHandlers;


  protected ValuesContainer(ObservableElementList<ObservableBasicValue<String>, String> values,
      Type parent) {
    this.values = values;
    this.parent = parent;
    this.eventHandlers = new HashMap<Integer, EventHandler>();
    this.values.addListener(new Listener<ObservableBasicValue<String>>() {

      @Override
      public void onValueAdded(ObservableBasicValue<String> entry) {
        int entryIndex = ValuesContainer.this.values.indexOf(entry);
        if (eventHandlers.containsKey(entryIndex)) {
          EventHandler handler = eventHandlers.get(entryIndex);
          eventHandlers.remove(entryIndex);
          handler.run(entry.get());
        }
      }

      @Override
      public void onValueRemoved(ObservableBasicValue<String> entry) {

      }

    });
  }

  public void registerEventHandler(Integer index, EventHandler handler) {
    eventHandlers.put(index, handler);
  }

  public Type deserialize(String s) {

    if (s.startsWith(StringType.PREFIX)) {
      String index = s.substring(StringType.PREFIX.length());
      StringType t = StringType.deserialize(parent, index);
      t.setPath(parent.getPath() + "." + index);
      return t;
    }

    return null;
  }

  public ObservableBasicValue<String> add(String value) {
    return values.add(value);
  }

  public int indexOf(ObservableBasicValue<String> value) {
    return values.indexOf(value);
  }

  public ObservableBasicValue<String> get(int index) {

    if (values.size() == 0 || index >= values.size()) {
      // perhaps the values container hasn't got the data yet
      // return null to indicate to primitive value a deferred value
      return null;
    }

    return values.get(index);
  }

}
