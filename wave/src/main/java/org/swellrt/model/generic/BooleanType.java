package org.swellrt.model.generic;


import org.swellrt.model.ReadableBoolean;
import org.swellrt.model.ReadableNumber;
import org.swellrt.model.ReadableTypeVisitor;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.SourcesEvents;

public class BooleanType extends Type implements ReadableBoolean,
    SourcesEvents<BooleanType.Listener> {


  public interface Listener {

    void onValueChanged(String oldValue, String newValue);

  }

  /**
   * Get an instance of BooleanType. This method is used for deserialization.
   *
   * @param parent the parent Type instance of this string
   * @param valueIndex the index of the value in the parent's value container
   * @return
   */
  protected static BooleanType deserialize(Type parent, String valueIndex) {
    BooleanType string = new BooleanType();
    string.attach(parent, valueIndex);
    return string;
  }

  public final static String TYPE_NAME = "BooleanType";
  public final static String PREFIX = "bl";
  public final static String VALUE_ATTR = "v";


  private ObservableBasicValue<String> observableValue;
  private ObservableBasicValue.Listener<String> observableValueListener;

  private String path;

  private Type parent;
  private int valueRef; // the index of this value in the ValuesContainer
  private String initValue;



  private boolean isAttached;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();


  protected BooleanType() {

    this.initValue = null;
    this.observableValueListener = new ObservableBasicValue.Listener<String>() {

      @Override
      public void onValueChanged(String oldValue, String newValue) {
        for (Listener l : listeners)
          l.onValueChanged(oldValue, newValue);
      }
    };
  }

  public BooleanType(boolean initValue) {
    init(String.valueOf(initValue));
  }

  public BooleanType(String initValue) {
    init(initValue);
  }

  private void init(String initValue) {
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
    observableValue = parent.getValuesContainer().add(String.valueOf(initValue));
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
    Preconditions.checkArgument(isAttached, "Unable to deattach an unattached BooleanType");
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
    Preconditions.checkArgument(isAttached, "Unable to serialize an unattached BooleanType");
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
        Preconditions.checkArgument(isAttached, "Unable to initialize an unattached BooleanType");
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

  private boolean reAttach() {

    if (parent != null && valueRef >= 0) {
      attach(parent, valueRef);
      return isAttached();
    }

    return false;
  }

  //
  // Number operations
  //

  public Boolean getValue() {
    if (!isAttached())
      if (!reAttach())
        return Boolean.valueOf(initValue);

    return Boolean.valueOf(observableValue.get());
  }

  public void setValue(boolean value) {
    setValue(String.valueOf(value));
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
    return this;
  }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((parent == null) ? 0 : parent.hashCode());
		result = prime * result + valueRef;
		return result;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BooleanType other = (BooleanType) obj;
		if (parent == null) {
			if (other.parent != null)
				return false;
		} else if (!parent.equals(other.parent))
			return false;
		if (valueRef != other.valueRef)
			return false;
		return true;
	}

}
