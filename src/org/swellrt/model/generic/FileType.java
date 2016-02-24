package org.swellrt.model.generic;


import org.swellrt.model.ReadableFile;
import org.swellrt.model.ReadableTypeVisitor;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.SourcesEvents;

public class FileType extends Type implements ReadableFile, SourcesEvents<FileType.Listener> {


  public interface Listener {

    void onValueChanged(AttachmentId oldValue, AttachmentId newValue);

  }

  /**
   * Get an instance of FileType. This method is used for deserialization.
   *
   * @param parent the parent Type instance of this string
   * @param valueIndex the index of the value in the parent's value container
   * @return
   */
  protected static FileType deserialize(Type parent, String valueIndex) {
    FileType f = new FileType();
    f.attach(parent, valueIndex);
    return f;
  }

  public final static String TYPE_NAME = "FileType";
  public final static String PREFIX = "f";
  public final static String VALUE_ATTR = "v";


  private ObservableBasicValue<String> observableValue;
  private ObservableBasicValue.Listener<String> observableValueListener =
      new ObservableBasicValue.Listener<String>() {

        @Override
        public void onValueChanged(String oldValue, String newValue) {
          for (Listener l : listeners)
            try {
              l.onValueChanged(AttachmentId.deserialise(oldValue),
                  AttachmentId.deserialise(newValue));
            } catch (InvalidIdException e) {
              // TODO handle exception
            }
        }
      };


  private String path;

  private Type parent;
  private int valueRef; // the index of this value in the ValuesContainer
  private AttachmentId initValue;

  /**
   * The model owning this object. Despite other Types, FileType needs to know
   * its model before be attached in order to generate URL
   */
  private Model model;

  private boolean isAttached;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();


  protected FileType() {
    this.initValue = null;
  }

  public FileType(AttachmentId initValue, Model model) {
    this.initValue = initValue != null ? initValue : null;
    this.model = model;
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
    observableValue = parent.getValuesContainer().add(initValue.serialise());
    observableValue.addListener(observableValueListener);
    valueRef = parent.getValuesContainer().indexOf(observableValue);
    isAttached = true;
  }

  @Override
  protected void attach(Type parent, String valueIndex) {
    Preconditions.checkArgument(parent.hasValuesContainer(),
        "Invalid parent type for a primitive value");

    this.parent = parent;

    Integer index = null;
    try {
      index = Integer.valueOf(valueIndex);
    } catch (NumberFormatException e) {

    }

    Preconditions.checkNotNull(index, "Value index is null");

    valueRef = index;
    observableValue = parent.getValuesContainer().get(index);

    if (observableValue == null) {
      // return a non-attached value
      // this singals the actual value hasn't been received yet.
      return;
    }


    observableValue.addListener(observableValueListener);

    isAttached = true;
  }

  protected void deattach() {
    Preconditions.checkArgument(isAttached, "Unable to deattach an unattached FileType");
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
    Preconditions.checkArgument(isAttached, "Unable to serialize an unattached FileType");
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
        Preconditions.checkArgument(isAttached, "Unable to initialize an unattached FileType");
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
  // File operations
  //

  public AttachmentId getValue() {
    if (!isAttached())
      return initValue;
    else {
      try {
        return AttachmentId.deserialise(observableValue.get());
      } catch (InvalidIdException e) {
        return null;
      }
    }

  }


  public void setValue(AttachmentId value) {
    if (isAttached()) {
      try {
        if (!value.equals(AttachmentId.deserialise(observableValue.get()))) {
          observableValue.set(value.serialise());
          parent.markValueUpdate(this);
        }
      } catch (InvalidIdException e) {
        // TODO handle exception
        return;
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
    return model == null ? parent.getModel() : model;
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
    return this;
  }

  @Override
  public boolean equals(Object obj) {

    if (obj instanceof FileType) {
      FileType other = (FileType) obj;
      // It's suppossed comparasion between types in the same container
      return (other.valueRef == this.valueRef);
    }


    return false;
  }

}
