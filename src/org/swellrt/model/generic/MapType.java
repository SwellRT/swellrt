package org.swellrt.model.generic;

import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicMap;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapType extends Type implements SourcesEvents<MapType.Listener> {


  public interface Listener {

    void onValueChanged(String key, Type oldValue, Type newValue);

    void onValueRemoved(String key, Type value);

  }



  protected static Type createAndAttach(Model model, String id) {
    // Model Root Doc is a map and more... allow model+root as map doc
    Preconditions.checkArgument(id.startsWith(PREFIX) || id.startsWith(Model.ROOT_DOC_PREFIX),
        "MapType.fromString() not a map id");
    MapType map = new MapType(model);
    map.attach(id);
    return map;

  }

  public final static String TYPE_NAME = "MapString";
  public final static String ROOT_TAG = "map";
  public final static String PREFIX = "map";

  private final static String ENTRY_TAG_NAME = "entry";
  private final static String KEY_ATTR_NAME = "k";
  private final static String VALUE_ATTR_NAME = "v";

  private ObservableBasicMap<String, Type> observableMap;
  private ObservableBasicMap.Listener<String, Type> observableMapListener;

  private Model model;

  private ObservableDocument backendDocument;
  private String backendDocumentId;
  private Doc.E backendRootElement;

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
  protected void attach(String docId) {


    if (docId == null) {

      docId = model.generateDocId(getPrefix());
      backendDocument = model.createDocument(docId);

    } else
      backendDocument = model.getDocument(docId);

    backendDocumentId = docId;

    // Create a root tag to ensure the document is persisted.
    // If the doc is created empty and it's not populated with data it won't
    // exist when the wavelet is open again.
    backendRootElement = DocHelper.getElementWithTagName(backendDocument, ROOT_TAG);
    if (backendRootElement == null)
      backendRootElement =
          backendDocument.createChildElement(backendDocument.getDocumentElement(), ROOT_TAG,
              Collections.<String, String> emptyMap());

    DocEventRouter router = DefaultDocEventRouter.create(backendDocument);

    this.observableMap =
        DocumentBasedBasicMap.create(router, backendRootElement, Serializer.STRING,
            model.getTypeSerializer(), ENTRY_TAG_NAME, KEY_ATTR_NAME, VALUE_ATTR_NAME);

    this.observableMap.addListener(observableMapListener);

    this.isAttached = true;

  }

  protected void deattach() {
    Preconditions.checkArgument(isAttached, "Unable to deattach an unattached MapType");
    // nothing to do. wavelet doesn't provide doc deletion
  }

  @Override
  protected String getPrefix() {
    return PREFIX;
  }


  @Override
  protected boolean isAttached() {
    return isAttached;
  }

  @Override
  protected String serializeToModel() {
    Preconditions.checkArgument(isAttached, "Unable to serialize an unattached MapType");
    return backendDocumentId;
  }

  @Override
  protected ListElementInitializer getListElementInitializer() {
    return new ListElementInitializer() {

      @Override
      public String getType() {
        return PREFIX;
      }

      @Override
      public String getBackendId() {
        return serializeToModel();
      }
    };
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
    Preconditions.checkArgument(!value.isAttached(),
        "MapType.put(): forbidden to add an already attached Type");

    value.attach(null);

    if (!observableMap.put(key, value)) {
      return null;
    }

    return value;
  }



  public StringType put(String key, String value) {
    Preconditions.checkArgument(isAttached, "MapType.put(): not attached to model");

    StringType strValue = new StringType(model, value);
    strValue.attach(null);

    if (!observableMap.put(key, strValue)) {
      return null;
    }

    return strValue;
  }

  public Set<String> keySet() {
    return observableMap.keySet();
  }

  public void remove(String key) {
    Preconditions.checkArgument(isAttached, "MapType.remove(): not attached to model");
    observableMap.remove(key);
  }

  @Override
  public String getDocumentId() {
    return backendDocumentId;
  }

  @Override
  public Model getModel() {
    return model;
  }

  @Override
  public String getType() {
    return TYPE_NAME;
  }




}
