package org.swellrt.model.generic;

import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicMap;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.Collections;
import java.util.Set;

public class MapType extends Type implements SourcesEvents<MapType.Listener> {


  /**
   * Listener to map events.
   */
  public interface Listener {

    void onValueChanged(String key, Type oldValue, Type newValue);

    void onValueRemoved(String key, Type value);

  }


  /**
   * Get an instance of MapType within the model backed by a document. This
   * method is used for deserialization.
   * 
   * @param model
   * @param substrateDocumentId
   * @return
   */
  protected static MapType deserialize(Type parent, String substrateDocumentId) {
    Preconditions.checkArgument(substrateDocumentId.startsWith(PREFIX),
        "Not a document id for MapType");
    MapType map = new MapType(parent.getModel());
    map.attach(parent, substrateDocumentId);
    return map;
  }

  protected static MapType deserialize(Model model, String substrateDocumentId) {
    Preconditions.checkArgument(substrateDocumentId.startsWith(PREFIX),
        "Not a document id for MapType");
    MapType map = new MapType(model);
    map.attach(null, substrateDocumentId);
    return map;
  }


  public final static String TYPE_NAME = "MapType";
  public final static String PREFIX = "map";

  private final static String TAG_MAP = "map";
  private final static String TAG_ENTRY = "entry";
  private final static String KEY_ATTR_NAME = "k";
  private final static String VALUE_ATTR_NAME = "v";

  private ObservableBasicMap<String, Type> observableMap;
  private ObservableBasicMap.Listener<String, Type> observableMapListener;

  private Model model;

  private ObservableDocument backendDocument;
  private String backendDocumentId;

  private MetadataContainer metadata;
  private ValuesContainer values;

  private Doc.E backendMapElement;

  private boolean isAttached;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  private DefaultDocEventRouter router;


  /**
   * Constructor for MapType instances.
   *
   * @param model The model metadata object where this instance belongs to.
   */
  protected MapType(Model model) {

    this.model = model;
    this.isAttached = false;

    observableMapListener = new ObservableBasicMap.Listener<String, Type>() {

      @Override
      public void onEntrySet(String key, Type oldValue, Type newValue) {

        if (newValue == null) {
          for (Listener l : listeners)
            l.onValueRemoved(key, oldValue);

        } else {
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
  protected void attach(Type parent) {
    Preconditions.checkArgument(!isAttached, "Already attached map type");
    String substrateDocumentId = model.generateDocId(getPrefix());
    attach(parent, substrateDocumentId);
  }

  @Override
  protected void attach(Type parent, String substrateDocumentId) {
    Preconditions.checkArgument(!isAttached, "Already attached map type");
    Preconditions.checkNotNull(substrateDocumentId, "Document id is null");

    backendDocumentId = substrateDocumentId;

    boolean isNew = false;

    // Get or create substrate document
    if (!model.getModelDocuments().contains(backendDocumentId)) {
      backendDocument = model.createDocument(backendDocumentId);
      isNew = true;
    } else {
      backendDocument = model.getDocument(backendDocumentId);
    }

    router = DefaultDocEventRouter.create(backendDocument);

    // Metadata section
    metadata = MetadataContainer.get(backendDocument);

    if (isNew) {
      metadata.setCreator(model.getCurrentParticipantId());
    }


    // Map section
    backendMapElement = DocHelper.getElementWithTagName(backendDocument, TAG_MAP);
    if (backendMapElement == null) {
      backendMapElement =
          backendDocument.createChildElement(backendDocument.getDocumentElement(), TAG_MAP,
          Collections.<String, String> emptyMap());
    }

    // Initialize observable map
    this.observableMap =
        DocumentBasedBasicMap.create(router, backendMapElement, Serializer.STRING,
            new MapSerializer(this), TAG_ENTRY, KEY_ATTR_NAME, VALUE_ATTR_NAME);

    this.observableMap.addListener(observableMapListener);

    // Initialize values section
    values = ValuesContainer.get(backendDocument, router, this);

    // Attached!
    this.isAttached = true;

  }

  protected void deattach() {
    Preconditions.checkArgument(isAttached, "Unable to detach an unattached Map");
    metadata.setDetachedPath();
    isAttached = false;
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
  protected String serialize() {
    Preconditions.checkArgument(isAttached, "Unable to serialize an unattached Map");
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
        return serialize();
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
    Preconditions.checkArgument(isAttached, "Unable to get values from an unattached Map");

    if (observableMap.keySet().contains(key)) {
      return observableMap.get(key);
    }

    return null;
  }


  public Type put(String key, Type value) {
    Preconditions.checkArgument(isAttached, "Unable to put values into an unattached Map");
    Preconditions.checkArgument(!value.isAttached(),
        "Already attached Type instances can't be put into a Map");

    Type oldValue = observableMap.get(key);

    // Attach to a new substrate document o to a values container
    value.attach(this);

    if (!observableMap.put(key, value)) {
      value.deattach();
      return null;
    }

    value.setPath(getPath() + "." + key);

    if (oldValue != null) {
      oldValue.deattach();
    }

    return value;
  }



  public StringType put(String key, String value) {
    Preconditions.checkArgument(isAttached, "Unable to put values into an unattached Map");

    StringType strValue = new StringType(value);
    put(key, strValue);

    return strValue;
  }

  public Set<String> keySet() {
    return observableMap.keySet();
  }

  public void remove(String key) {
    Preconditions.checkArgument(isAttached, "Unable to remove values from an unattached Map");
    Type removedValue = observableMap.get(key);
    if (removedValue != null) {
      observableMap.remove(key);
      removedValue.deattach();
    }

  }

  @Override
  public String getDocumentId() {
    return backendDocumentId;
  }

  @Override
  public String getType() {
    return TYPE_NAME;
  }

  @Override
  public String getPath() {
    return metadata.getPath();
  }

  @Override
  protected void setPath(String path) {
    metadata.setPath(path);
  }

  @Override
  protected boolean hasValuesContainer() {
    return true;
  }

  @Override
  protected ValuesContainer getValuesContainer() {
    return values;
  }

  @Override
  protected String getValueReference(Type value) {
    for (String key : observableMap.keySet()) {
      if (observableMap.get(key).equals(value.serialize())) return key;
    }
    return null;
  }

  @Override
  public Model getModel() {
    return model;
  }


}
