package org.waveprotocol.mod.model.generic;

import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.adt.docbased.DocumentBasedBasicValue;
import org.waveprotocol.wave.model.document.Doc.E;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.Serializer;
import org.waveprotocol.wave.model.wave.SourcesEvents;

public class StringType extends Type implements SourcesEvents<StringType.Listener> {


  public interface Listener {

    void onValueChanged(String oldValue, String newValue);

  }


  protected static StringType fromString(Model model, String s) {

    Preconditions.checkArgument(s.startsWith(PREFIX), "StringType.fromString() not a StringType");
    int indexStringPos = Integer.valueOf(s.split("\\+")[1]);

    ObservableBasicValue<String> observableValue =
        model.getStringIndex().get(Integer.valueOf(indexStringPos));

    StringType str = new StringType(model, observableValue.get());
    str.attachToString(indexStringPos, observableValue);

    return str;

  }


  public final static String TYPE = "str";
  public final static String PREFIX = "str";
  public final static String VALUE_ATTR = "v";


  private ObservableBasicValue<String> observableValue;
  private ObservableBasicValue.Listener<String> observableValueListener;

  private Model model;
  private int indexStringPos; // Index in the String Index
  private String initValue;

  private boolean isAttached;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();


  public StringType(Model model, String value) {
    this.model = model;
    this.initValue = value;

    this.observableValueListener = new ObservableBasicValue.Listener<String>() {

      @Override
      public void onValueChanged(String oldValue, String newValue) {
        for (Listener l: listeners)
          l.onValueChanged(oldValue, newValue);
      }
    };
  }



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
    // Nothing to do
  }

  protected void attachToString(int indexStringPos, ObservableBasicValue<String> observableValue) {
    this.indexStringPos = indexStringPos;
    this.observableValue = observableValue;
    this.observableValue.addListener(observableValueListener);
    isAttached = true;
  }

  @Override
  protected void attachToParent(String documentId, ObservableDocument parentDoc, E parentElement) {

    DocEventRouter router = DefaultDocEventRouter.create(parentDoc);

    this.observableValue =
        DocumentBasedBasicValue.create(router, parentElement, Serializer.STRING, VALUE_ATTR);
    this.observableValue.addListener(observableValueListener);
    isAttached = true;

  }

  @Override
  protected boolean isAttached() {
    return isAttached;
  }


  @Override
  protected String serializeToModel() {
    Preconditions.checkArgument(isAttached, "Unable to serialize an unattached StringType");
    return PREFIX + "+" + Integer.toString(indexStringPos);
  }

  @Override
  protected TypeInitializer getTypeInitializer() {
    return new TypeInitializer() {

      @Override
      public String getType() {
        return TYPE;
      }

      @Override
      public String getSimpleValue() {
        return initValue;
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
  // String operations
  //

  public String getValue() {
    if (!isAttached())
      return initValue;
    else
      return observableValue.get();
  }


  public void setValue(String value) {
    if (isAttached()) observableValue.set(value);
  }





}
