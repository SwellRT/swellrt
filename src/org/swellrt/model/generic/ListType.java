package org.swellrt.model.generic;


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

public class ListType extends Type implements SourcesEvents<ListType.Listener> {

  public interface Listener {

    public void onValueAdded(Type entry);

    public void onValueRemoved(Type entry);

  }


  protected static Type createAndAttach(Model model, String id) {

    Preconditions.checkArgument(id.startsWith(PREFIX), "ListType.createAndAttach() not a list id");
    ListType list = new ListType(model);
    list.attach(id);
    return list;

  }

  public final static String TYPE_NAME = "ListType";
  public final static String PREFIX = "list";


  public final static String ROOT_TAG = "list";
  private final static String ITEM_TAG = "item";

  private ObservableElementList<Type, ListElementInitializer> observableList;
  private ObservableElementList.Listener<Type> observableListListener;


  private Model model;

  private String backendDocumentId;
  private ObservableDocument backendDocument;
  private Doc.E backendRootElement;


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

    this.observableList =
        DocumentBasedElementList.create(router, backendRootElement, ITEM_TAG,
            new ListElementFactory(model));
    this.observableList.addListener(observableListListener);

    this.isAttached = true;
  }

  protected void deattach() {
    Preconditions.checkArgument(isAttached, "Unable to deattach an unattached MapType");

    // nothing to do. wavelet doesn't provide doc deletion
  }


  @Override
  protected boolean isAttached() {
    return isAttached;
  }


  @Override
  protected String serializeToModel() {
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
    listeners.remove(listener);
  }

  //
  // List operations
  //

  public Type add(Type value) {
    Preconditions.checkArgument(isAttached, "ListType.add(): not attached to model");
    Preconditions.checkArgument(!value.isAttached(),
        "ListType.add(): forbidden to add an already attached Type");

    value.attach(null);
    Type listValue = observableList.add(value.getListElementInitializer());

    // return the value generated from list to double check add() success
    // also it is the cached value in the observable list
    return listValue;
  }

  public Type add(int index, Type value) {

    Preconditions.checkArgument(index >= 0 && index <= observableList.size(),
        "ListType.add(): add to index out of bounds");
    Preconditions.checkArgument(isAttached, "ListType.add(): not attached to model");
    Preconditions.checkArgument(!value.isAttached(),
        "ListType.add(): forbidden to add an already attached Type");

    value.attach(null);
    Type listValue = observableList.add(index, value.getListElementInitializer());

    // return the value generated from list to double check add() success
    // also it is the cached value in the observable list
    return listValue;
  }


  public Type remove(int index) {
    if (observableList == null) return null;
    Type removedInstance = observableList.get(index);
    if (!observableList.remove(removedInstance)) return null;
    return removedInstance;
  }


  public Type get(int index) {
    if (observableList == null) return null;
    Preconditions.checkArgument(index >= 0 && index < observableList.size(),
        "ListType.get(): add to index out of bounds");
    return observableList.get(index);
  }

  public int indexOf(Type type) {
    return observableList != null ? observableList.indexOf(type) : -1;
  }

  public int size() {
    return observableList != null ? observableList.size() : 0;
  }

  public Iterable<Type> getValues() {
    return observableList != null ? observableList.getValues() : Collections.<Type> emptyList();
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
