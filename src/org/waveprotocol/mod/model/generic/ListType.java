package org.waveprotocol.mod.model.generic;


import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedElementList;
import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.Collections;

public class ListType extends Type implements SourcesEvents<ListType.Listener> {

  public interface Listener {

    public void onValueAdded(Type entry);

    public void onValueRemoved(Type entry);

  }

  protected static ListType fromString(Model model, String s) {

    Preconditions.checkArgument(s.startsWith(PREFIX), "ListType.fromString() is not a StringType");
    ListType list = new ListType(model);
    list.attachToModel(s);
    return list;

  }


  public final static String TYPE = "list";
  public final static String PREFIX = "list";


  private final static String ITEM_TAG = "item";

  private ObservableElementList<Type, TypeInitializer> observableList;
  private ObservableElementList.Listener<Type> observableListListener;


  private Model model;

  private String documentId;
  private Doc.E element;

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
  protected void attachToParent(String documentId, ObservableDocument parentDoc, E parentElement) {

    DocEventRouter router = DefaultDocEventRouter.create(parentDoc);

    this.observableList =
        DocumentBasedElementList.create(router, parentElement, ITEM_TAG, new TypeFactory(model));


    this.element = parentElement;
    this.documentId = documentId;

    this.isAttached = true;
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
  protected void attachToString(int indexStringPos, ObservableBasicValue<String> observableValue) {
    // Nothing to do

  }


  @Override
  protected boolean isAttached() {
    return isAttached;
  }


  @Override
  protected String serializeToModel() {
    Preconditions.checkArgument(isAttached, "Unable to serialize an unattached ListType");
    return documentId;
  }

  @Override
  protected TypeInitializer getTypeInitializer() {
    return TypeInitializer.ListTypeInitializer;
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

    Type attachedValue = observableList.add(value.getTypeInitializer());

    return attachedValue;
  }

  public Type add(int index, Type value) {

    Preconditions.checkArgument(index >= 0 && index <= observableList.size(),
        "ListType.add(): add to index out of bounds");
    Preconditions.checkArgument(isAttached, "ListType.add(): not attached to model");
    Preconditions.checkArgument(!value.isAttached(),
        "ListType.add(): forbidden to add an already attached Type");

    Type attachedValue = observableList.add(index, value.getTypeInitializer());

    return attachedValue;
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

}
