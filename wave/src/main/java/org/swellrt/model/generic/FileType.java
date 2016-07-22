package org.swellrt.model.generic;


import org.swellrt.model.ReadableBoolean;
import org.swellrt.model.ReadableFile;
import org.swellrt.model.ReadableNumber;
import org.swellrt.model.ReadableTypeVisitor;
import org.waveprotocol.wave.media.model.AttachmentId;
import org.waveprotocol.wave.model.adt.ObservableBasicValue;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.SourcesEvents;

public class FileType extends Type implements ReadableFile, SourcesEvents<FileType.Listener> {

  public static class Value {

    AttachmentId attachmentId;
    String contentType;

    public String serialize() {
      return attachmentId.serialise()
          + (contentType == null || contentType.isEmpty() ? "" : "," + contentType);
    }

    protected Value(AttachmentId attachmentId, String contentType) {
      this.attachmentId = attachmentId;
      this.contentType = contentType;
    }

    public static Value deserialize(String valueString) throws InvalidModelStringValue {
      Preconditions.checkNotNull(valueString, "String value can't be null");
      String[] parts = valueString.split(",");
      Value v = null;
      try {
        v = new Value(AttachmentId.deserialise(parts[0]), parts.length > 1 ? parts[1] : null);
      } catch (InvalidIdException e) {
        throw new InvalidModelStringValue();
      }

      return v;
    }

    public AttachmentId getAttachmentId() {
      return attachmentId;
    }

    public String getContentType() {
      return contentType;
    }

  }



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
  private Value initValue;

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

  public FileType(AttachmentId attachmentId, String contentType, Model model) {
    this.initValue = attachmentId != null ? new FileType.Value(attachmentId, contentType) : null;
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
    observableValue = parent.getValuesContainer().add(initValue.serialize());
    observableValue.addListener(observableValueListener);
    valueRef = parent.getValuesContainer().indexOf(observableValue);
    isAttached = true;
  }

  @Override
  protected void attach(Type parent, int slotIndex) {
    this.parent = parent;
    valueRef = slotIndex;

    if (initValue != null && initValue.attachmentId != null)
      observableValue = parent.getValuesContainer().add(initValue.serialize(), slotIndex);
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


  private boolean reAttach() {

    if (parent != null && valueRef >= 0) {
      attach(parent, valueRef);
      return isAttached();
    }

    return false;
  }

  //
  // File operations
  //

  public AttachmentId getValue() {
    return getFileId();
  }

  public AttachmentId getFileId() {
    if (!isAttached())
      if (!reAttach())
        return initValue.attachmentId;

    String valueString = observableValue.get();
    if (valueString == null || valueString.isEmpty()) return null;

    try {
      return FileType.Value.deserialize(valueString).attachmentId;
    } catch (InvalidModelStringValue e) {
      return null;
    }


  }

  public String getContentType() {
    if (!isAttached())
      return initValue.contentType;
    else {

      String valueString = observableValue.get();
      if (valueString == null || valueString.isEmpty()) return null;

      try {
        return FileType.Value.deserialize(valueString).contentType;
      } catch (InvalidModelStringValue e) {
        return null;
      }
    }
  }


  public void setValue(AttachmentId attachmentId, String contentType) {
    Preconditions.checkNotNull(attachmentId, "Attachment id can't be null");
    if (isAttached()) {
      FileType.Value v = new FileType.Value(attachmentId, contentType);
      observableValue.set(v.serialize());
      parent.markValueUpdate(this);
    }
  }

  public void setValue(FileType file) {
    setValue(file.getFileId(), file.getContentType());
  }

  public void clearValue() {
    if (isAttached()) {
      observableValue.set("");
      parent.markValueUpdate(this);
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
  public ReadableNumber asNumber() {
    return null;
  }

  @Override
  public ReadableBoolean asBoolean() {
    return null;
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
		FileType other = (FileType) obj;
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
