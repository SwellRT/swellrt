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
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MapType extends Type implements ReadableMap, SourcesEvents<MapType.Listener> {


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

        if (newValue == null) {

          for (Listener l : listeners)
            l.onValueRemoved(key, oldValue);


        } else {

          // Under some circunstances, value's update will reach after map entry
          // update so the new value is not attached.
          // To avoid the issue, we wait for the value's update and then
          // trigger again the event.

          if (newValue.isAttached()) {

            for (Listener l : listeners)
              l.onValueChanged(key, oldValue, newValue);

          } else {

            final Integer index = newValue.getValueRefefence();
            if (index != null)
              values.registerEventHandler(index, new ValuesContainer.EventHandler() {

                @Override
                public void run(String value) {

                  newValue.attach(MapType.this, "" + index.intValue());

                  for (Listener l : listeners)
                    l.onValueChanged(key, oldValue, newValue);

                }
              });
            }
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

    // Be careful with order of following steps!

    // Get or create substrate document
    if (!model.getModelDocuments().contains(backendDocumentId)) {
      backendDocument = model.createDocument(backendDocumentId);
      isNew = true;
    } else {
      backendDocument = model.getDocument(backendDocumentId);
    }

    // To debug doc operations
    /*
    backendDocument.addListener(new DocumentHandler<Doc.N, Doc.E, Doc.T>() {

      @Override
      public void onDocumentEvents(
          org.waveprotocol.wave.model.document.indexed.DocumentHandler.EventBundle<N, E, T> event) {

        for (DocumentEvent<Doc.N, Doc.E, Doc.T> e : event.getEventComponents()) {
          trace("(" + backendDocumentId + ") Doc Event " + e.toString());
        }

      }

    });
    */

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

    // Attach to a new substrate document o to a values container
    value.attach(this);

    // This should be always a new put, otherwise the !value.isAttached
    // precondition
    // would be false
    if (!observableMap.put(key, value)) {
      value.deattach();
      return null;
    }

    value = observableMap.get(key);
    value.setPath(getPath() + "." + key);

    cachedMap.put(key, value);


    if (oldValue != null) {
      oldValue.deattach();
    }

    return value;
  }



  public StringType put(String key, String value) {
    Preconditions.checkArgument(isAttached, "Unable to put values into an unattached Map");

    // if key exists, change primitive directly
    if (keySet().contains(key)) {
      Type existingValue = observableMap.get(key);
      if (existingValue.getType().equals(StringType.TYPE_NAME)) {
        StringType existingStringValue = (StringType) existingValue;
        existingStringValue.setValue(value);
        return existingStringValue;
      }
    }

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

}
