package org.swellrt.model.generic;

import org.swellrt.model.ReadableBoolean;
import org.swellrt.model.ReadableMap;
import org.swellrt.model.ReadableNumber;
import org.swellrt.model.ReadableTypeVisitor;
import org.swellrt.model.adt.DocumentBasedBasicRMap;
import org.waveprotocol.wave.model.adt.ObservableBasicMap;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.DocumentEventGroupListener;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MapType extends Type implements ReadableMap, SourcesEvents<MapType.Listener>,
    DocumentEventGroupListener {

  public class Event {

    protected Event(String key, Type oldValue, Type newValue) {
      super();
      this.key = key;
      this.oldValue = oldValue;
      this.newValue = newValue;
    }

    public String key;

    public Type oldValue;

    public Type newValue;

  }

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

  public final static String TAG_MAP = "map";
  public final static String TAG_ENTRY = "entry";
  public final static String KEY_ATTR_NAME = "k";
  public final static String VALUE_ATTR_NAME = "v";

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

  private final Map<String, Type> cachedMap = new HashMap<String, Type>();

  private final List<Event> pendingEvents = new ArrayList<Event>();

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
      public void onEntrySet(final String key, final Type oldValue, final Type newValue) {
        // Events are not delivered yet. Wait until all events are delivered.
        pendingEvents.add(new Event(key, oldValue, newValue));
      }
    };
  }

  private void deliverPendingEvents() {

    for (Event e: pendingEvents) {

      if (e.newValue == null) {

        for (Listener l : listeners)
          l.onValueRemoved(e.key, e.oldValue);

      } else {

        for (Listener l : listeners)
          l.onValueChanged(e.key, e.oldValue, e.newValue);

      }
    }

    pendingEvents.clear();
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
  protected void attach(Type parent, int slotIndex) {
    throw new IllegalStateException("This method is not allowed for a MapType");
  }

  @Override
  protected void attach(Type parent, String substrateDocumentId) {
    Preconditions.checkArgument(!isAttached, "Already attached map type");
    Preconditions.checkNotNull(substrateDocumentId, "Document id is null");

    backendDocumentId = substrateDocumentId;

    boolean isNew = false;

    // Be careful with order of following steps!

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

    // Initialize values section. Always before loading the observable map
    this.values = ValuesContainer.get(backendDocument, router, this);

    // Attached! Before the observable list initialization to allow access
    // during initialization
    this.isAttached = true;

    // Initialize observable map
    this.observableMap =
        DocumentBasedBasicRMap.create(router, backendMapElement, Serializer.STRING,
            new MapSerializer(this), TAG_ENTRY, KEY_ATTR_NAME, VALUE_ATTR_NAME);

    this.observableMap.addListener(observableMapListener);

    router.setEventGroupListener(this);
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
    listeners.remove(listener);
  }


  //
  // Map operations
  //

  @Override
  public Type get(String key) {
    Preconditions.checkArgument(isAttached, "Unable to get values from an unattached Map");

    if (observableMap.keySet().contains(key)) {

      if (!cachedMap.containsKey(key)) cachedMap.put(key, observableMap.get(key));

      return cachedMap.get(key);
    }

    return null;
  }


  public Type put(String key, Type value) {
    Preconditions.checkArgument(isAttached, "Unable to put values into an unattached Map");
    Preconditions.checkArgument(!value.isAttached(),
        "Already attached Type instances can't be put into a Map");

    Type oldValue = observableMap.get(key);

    value.attach(this);

    // This should be always a new put, otherwise the !value.isAttached
    // precondition would be false
    observableMap.put(key, value);

    value = observableMap.get(key);
    if (value == null) return null;

    value.setPath(getPath() + "." + key);
    cachedMap.put(key, value);

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

  @Override
  public Set<String> keySet() {
    return observableMap.keySet();
  }

  @Override
  public boolean hasKey(String key) {
    return observableMap.keySet().contains(key);
  }


  public void remove(String key) {
    Preconditions.checkArgument(isAttached, "Unable to remove values from an unattached Map");
    Type removedValue = observableMap.get(key);
    if (removedValue != null) {
      cachedMap.remove(key);
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
      if (observableMap.get(key).equals(value)) return key;
    }
    return null;
  }

  @Override
  public Model getModel() {
    return model;
  }

  @Override
  public void accept(ReadableTypeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public MapType asMap() {
    return this;
  }

  @Override
  public StringType asString() {
    return null;
  }

  @Override
  public ListType asList() {
    return null;
  }

  @Override
  public TextType asText() {
    return null;
  }

  @Override
  public FileType asFile() {
    return null;
  }

  @Override
  public ReadableNumber asNumber() {
    return null;
  }

  @Override
  public ReadableBoolean asBoolean() {
    return null;
  }

  @Override
  protected void markValueUpdate(Type value) {
    // Force a redundant put to trigger a convinient DocOp
    // for a primitive value update.
    String key = getValueReference(value);
    if (key != null) observableMap.put(key, value);
  }

  @Override
  public void onBeginEventGroup(String groupId) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onEndEventGroup(String groupId) {
    deliverPendingEvents();
  }

}
