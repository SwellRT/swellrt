package org.swellrt.model.generic;


import org.swellrt.model.ReadableBoolean;
import org.swellrt.model.ReadableNumber;
import org.swellrt.model.ReadableString;
import org.swellrt.model.ReadableTypeVisitor;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.SourcesEvents;

public class StringType extends Type implements ReadableString, SourcesEvents<StringType.Listener> {


  public interface Listener {

    void onValueChanged(String oldValue, String newValue);

  }

  /**
   * Get an instance of StringType. This method is used for deserialization.
   * 
   * @param parent the parent Type instance of this string
   * @param valueIndex the index of the value in the parent's value container
   * @return
   */
  protected static StringType deserialize(Type parent, String valueIndex) {
    StringType string = new StringType();
    string.attach(parent, valueIndex);
    return string;
  }

  public final static String TYPE_NAME = "StringType";
  public final static String PREFIX = "str";
  public final static String VALUE_ATTR = "v";


  private ObservableBasicValue<String> observableValue;
  private ObservableBasicValue.Listener<String> observableValueListener;

  private String path;

  private Type parent;
  private int valueRef; // the index of this value in the ValuesContainer
  private String initValue;



  private boolean isAttached;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();


  protected StringType() {

    this.initValue = null;
    this.observableValueListener = new ObservableBasicValue.Listener<String>() {

      @Override
      public void onValueChanged(String oldValue, String newValue) {
        for (Listener l : listeners)
          l.onValueChanged(oldValue, newValue);
      }
    };
  }

  public StringType(String initValue) {

    this.initValue = initValue != null ? initValue : "";
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
  protected void attach(Type parent) {
    Preconditions.checkArgument(parent.hasValuesContainer(),
        "Invalid parent type for a primitive value");
    this.parent = parent;
    observableValue = parent.getValuesContainer().add(initValue);
    observableValue.addListener(observableValueListener);
    valueRef = parent.getValuesContainer().indexOf(observableValue);
    isAttached = true;
  }

  @Override
  protected void attach(Type parent, int slotIndex) {
    this.parent = parent;
    valueRef = slotIndex;

    if (initValue != null && !initValue.isEmpty())
      observableValue = parent.getValuesContainer().add(initValue, slotIndex);
    else
      observableValue = parent.getValuesContainer().get(slotIndex);

    if (observableValue == null) {
      // return a non-attached value
      // this singals the actual value hasn't been received yet.
      return;
    }
    observableValue.addListener(observableValueListener);
    isAttached = true;
  }

  @Override
  protected void attach(Type parent, String valueIndex) {
    Preconditions.checkArgument(parent.hasValuesContainer(),
        "Invalid parent type for a primitive value");
    Integer index = null;
    try {
      index = Integer.valueOf(valueIndex);
    } catch (NumberFormatException e) {

    }

    Preconditions.checkNotNull(index, "Value index is null");
    attach(parent, index);
  }

  protected void deattach() {
    Preconditions.checkArgument(isAttached, "Unable to deattach an unattached StringType");
    observableValue.removeListener(this.observableValueListener);
    observableValue = null;
    isAttached = false;
  }


  @Override
  protected boolean isAttached() {
    return isAttached;
  }


  @Override
  protected String serialize() {
    Preconditions.checkArgument(isAttached, "Unable to serialize an unattached StringType");
    return PREFIX + "+" + Integer.toString(valueRef);
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
        Preconditions.checkArgument(isAttached, "Unable to initialize an unattached StringType");
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
  // String operations
  //

  public String getValue() {
    if (!isAttached())
      return initValue;
    else
      return observableValue.get();
  }


  public void setValue(String value) {
    if (isAttached()) {
      if (!value.equals(observableValue.get())) {
        observableValue.set(value);
        parent.markValueUpdate(this);
      }
    }
  }

  @Override
  public String getDocumentId() {
    return null;
  }


  @Override
  public String getType() {
    return TYPE_NAME;
  }


  @Override
  protected void setPath(String path) {
    this.path = path;
  }

  @Override
  public String getPath() {
    if (path == null && parent != null && isAttached) {
      path = parent.getPath() + "." + parent.getValueReference(this);
    }
    return path;
  }

  @Override
  protected boolean hasValuesContainer() {
    return false;
  }

  @Override
  protected ValuesContainer getValuesContainer() {
    return null;
  }

  @Override
  protected String getValueReference(Type value) {
    return null;
  }

  protected Integer getValueRefefence() {
    return valueRef;
  }

  @Override
  public Model getModel() {
    return parent.getModel();
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
    return this;
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
  public boolean equals(Object obj) {

    if (obj instanceof StringType) {
      StringType other = (StringType) obj;
      // It's suppossed comparasion between types in the same container
      return (other.valueRef == this.valueRef);
    }


    return false;
  }

}
