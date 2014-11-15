package org.waveprotocol.mod.model.generic;

import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicMap;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapType extends Type implements SourcesEvents<MapType.Listener> {


  public interface Listener {

    void onValueChanged(String key, Type oldValue, Type newValue);

    void onValueRemoved(String key, Type value);

  }


  protected static MapType fromString(Model model, String s) {

    Preconditions.checkArgument(s.startsWith(PREFIX), "MapType.fromString() is not a MapType");
    MapType map = new MapType(model);
    map.attachToModel(s);
    return map;

  }


  public final static String TYPE = "map";
  public final static String PREFIX = "map";

  private final static String ENTRY_TAG_NAME = "entry";
  private final static String KEY_ATTR_NAME = "k";
  private final static String VALUE_ATTR_NAME = "v";

  private ObservableBasicMap<String, Type> observableMap;
  private ObservableBasicMap.Listener<String, Type> observableMapListener;

  private Model model;

  private ObservableDocument document;
  private String documentId;
  private Doc.E element;

  private boolean isAttached;

  private Map<String, Type> map;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  public MapType(Model model) {

    this.model = model;
    this.isAttached = false;
    this.map = new HashMap<String, Type>();

    observableMapListener = new ObservableBasicMap.Listener<String, Type>() {

      @Override
      public void onEntrySet(String key, Type oldValue, Type newValue) {

        if (newValue == null) {

          map.remove(key);
          for (Listener l : listeners)
            l.onValueRemoved(key, oldValue);

        } else {

          map.put(key, newValue);
          for (Listener l : listeners)
            l.onValueChanged(key, oldValue, newValue);

        }

      }
    };
  }

  //
  // Type interface
  //


  @Override
  protected String getPrefix() {
    return PREFIX;
  }

  @Override
  protected void attachToModel() {
    model.attach(this);
  }

  @Override
  protected void attachToModel(String documentId) {
    model.attach(this, documentId);
  }


  @Override
  protected void attachToParent(String documentId, ObservableDocument parentDoc, Doc.E parentElement) {

    DocEventRouter router = DefaultDocEventRouter.create(parentDoc);

    this.observableMap =
        DocumentBasedBasicMap.create(router, parentElement, Serializer.STRING,
            model.getTypeSerializer(), ENTRY_TAG_NAME, KEY_ATTR_NAME, VALUE_ATTR_NAME);

    this.observableMap.addListener(observableMapListener);

    this.element = parentElement;
    this.documentId = documentId;
    this.document = parentDoc;

    this.isAttached = true;

  }

  @Override
  protected void attachToString(int indexStringPos, ObservableBasicValue<String> observableValue) {
    // Nothing to do
  }

  @Override
  protected boolean isAttached() {
    return isAttached;
  }

  @Override
  protected String serializeToModel() {
    Preconditions.checkArgument(isAttached, "Unable to serialize an unattached MapType");
    return documentId;
  }

  @Override
  protected TypeInitializer getTypeInitializer() {
    return TypeInitializer.MapTypeInitializer;
  }


  //
  // Listeners
  //


  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }


  @Override
  public void removeListener(Listener listener) {
    listeners.add(listener);
  }


  //
  // Map operations
  //


  public Type get(String key) {

    if (!map.containsKey(key)) {
      if (observableMap.keySet().contains(key)) {
        map.put(key, observableMap.get(key));
      } else
        return null;
    }

    return map.get(key);
  }


  public Type put(String key, Type value) {
    Preconditions.checkArgument(isAttached, "MapType.put(): not attached to model");

    if (!value.isAttached()) value.attachToModel();

    if (!observableMap.put(key, value)) {
      return null;
    }

    return value;
  }



  public StringType put(String key, String value) {
    Preconditions.checkArgument(isAttached, "MapType.put(): not attached to model");

    StringType svalue = new StringType(model, value);
    svalue.attachToModel();

    if (!observableMap.put(key, svalue)) {
      return null;
    }

    return svalue;
  }

  public Set<String> keySet() {
    return map.keySet();
  }

  public void remove(String key) {
    Preconditions.checkArgument(isAttached, "MapType.remove(): not attached to model");
    observableMap.remove(key);
  }





}
