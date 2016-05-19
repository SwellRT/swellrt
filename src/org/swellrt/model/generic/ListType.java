package org.swellrt.model.generic;


import org.swellrt.model.ReadableBoolean;
import org.swellrt.model.ReadableList;
import org.swellrt.model.ReadableNumber;
import org.swellrt.model.ReadableTypeVisitor;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.Collections;

public class ListType extends Type implements ReadableList<Type>, SourcesEvents<ListType.Listener> {

  public interface Listener {

    public void onValueAdded(Type entry);

    public void onValueRemoved(Type entry);

  }

  /**
   * Get an instance of ListType within the model backed by a document. This
   * method is used for deserialization.
   *
   * @param model
   * @param substrateDocumentId
   * @return
   */
  protected static ListType deserialize(Type parent, String substrateDocumentId) {
    Preconditions.checkArgument(substrateDocumentId.startsWith(PREFIX),
        "Not a document id for ListType");

    ListType list = new ListType(parent.getModel());
    list.attach(parent, substrateDocumentId);
    return list;
  }

  public final static String TYPE_NAME = "ListType";
  public final static String PREFIX = "list";


  public final static String TAG_LIST = "list";
  public final static String TAG_LIST_ITEM = "item";
  public final static String ATTR_LIST_ITEM_REF = "r";
  public final static String ATTR_LIST_ITEM_TYPE = "t";


  private ObservableElementList<Type, ListElementInitializer> observableList;
  private ObservableElementList.Listener<Type> observableListListener;


  private Model model;

  private String backendDocumentId;
  private ObservableDocument backendDocument;
  private Doc.E backendRootElement;

  private MetadataContainer metadata;
  private ValuesContainer values;

  private boolean isAttached;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();


  protected ListType(Model model) {
    this.model = model;
    this.isAttached = false;


    observableListListener = new ObservableElementList.Listener<Type>() {

      @Override
      public void onValueAdded(Type entry) {
        for (ListType.Listener l : listeners)
          l.onValueAdded(entry);
      }

      @Override
      public void onValueRemoved(Type entry) {
        for (ListType.Listener l : listeners)
          l.onValueRemoved(entry);
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
  protected void attach(Type parent) {
    Preconditions.checkArgument(!isAttached, "Already attached list type");
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
          ListType.this.trace("(" + backendDocumentId + ") Doc Event " + e.toString());
        }

      }

    });
    */

    DocEventRouter router = DefaultDocEventRouter.create(backendDocument);

    // Load metadata section
    metadata = MetadataContainer.get(backendDocument);

    if (isNew) {
      metadata.setCreator(model.getCurrentParticipantId());
    }

    // Load list section
    backendRootElement = DocHelper.getElementWithTagName(backendDocument, TAG_LIST);
    if (backendRootElement == null) {
      backendRootElement =
          backendDocument.createChildElement(backendDocument.getDocumentElement(), TAG_LIST,
              Collections.<String, String> emptyMap());
    }

    // Initialize values section. Always before observable list
    values = ValuesContainer.get(backendDocument, router, this);

    // Attached! Before the observable list initialization to allow access
    // during initialization
    this.isAttached = true;

    // Initialize observable list
    this.observableList =
        DocumentBasedElementList.create(router, backendRootElement, TAG_LIST_ITEM,
            new ListElementFactory(this));
    this.observableList.addListener(observableListListener);


  }


  protected void deattach() {
    Preconditions.checkArgument(isAttached, "Unable to deatach an unattached MapType");
    metadata.setDetachedPath();
    isAttached = false;
  }


  @Override
  protected boolean isAttached() {
    return isAttached;
  }


  @Override
  protected String serialize() {
    Preconditions.checkArgument(isAttached, "Unable to serialize an unattached ListType");
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
  // List operations
  //

  public Type add(Type value) {
    Preconditions.checkArgument(isAttached, "Unable to add values to an unattached List");
    Preconditions.checkArgument(!value.isAttached(),
        "Already attached Type instances can't be put into a List");

    value.attach(this);
    value = observableList.add(value.getListElementInitializer());
    int index = observableList.indexOf(value);
    value.setPath(getPath() + "." + index);

    // return the value generated from list to double check add() success
    // also it is the cached value in the observable list
    return value;
  }

  public Type add(int index, Type value) {

    Preconditions.checkArgument(index >= 0 && index <= observableList.size(),
        "Index out of list bounds");
    Preconditions.checkArgument(isAttached, "Unable to add values to an unattached List");
    Preconditions.checkArgument(!value.isAttached(),
        "Already attached Type instances can't be put into a List");

    value.attach(this);
    value = observableList.add(index, value.getListElementInitializer());
    value.setPath(getPath() + "." + index);

    // return the value generated from list to double check add() success
    // also it is the cached value in the observable list
    return value;
  }


  public Type remove(int index) {
    Preconditions.checkArgument(isAttached, "Unable to remove values from an unattached List");
    Type removedInstance = observableList.get(index);
    if (!observableList.remove(removedInstance)) return null;

    removedInstance.deattach();
    return removedInstance;
  }


  public Type get(int index) {
    Preconditions.checkArgument(isAttached, "Unable to get values from an unattached List");
    Preconditions.checkArgument(index >= 0 && index < observableList.size(),
        "Index out of list bounds");
    return observableList.get(index);
  }

  public int indexOf(Type type) {
    Preconditions.checkArgument(isAttached, "Unable to get index from an unattached List");
    return observableList.indexOf(type);
  }

  public int size() {
    Preconditions.checkArgument(isAttached, "Unable to get size from an unattached List");
    return observableList.size();
  }

  public Iterable<Type> getValues() {
    // Don't check for isAttached to allow JS model wrapper to try to get values
    return isAttached ? observableList.getValues() : null;
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
    int index = observableList.indexOf(value);
    return index >= 0 ? "" + index : null;
  }

  @Override
  public void accept(ReadableTypeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public MapType asMap() {
    return null;
  }

  @Override
  public StringType asString() {
    return null;
  }

  @Override
  public ListType asList() {
    return this;
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

}
