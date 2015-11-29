package org.swellrt.model.generic;

import org.swellrt.model.ReadableText;
import org.swellrt.model.ReadableTypeVisitor;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.document.Doc.N;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.util.Point;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.Collections;
import java.util.Set;

public class TextType extends Type implements ReadableText, SourcesEvents<TextType.Listener> {

  public interface Listener {

  }

  protected static Type deserialize(Type parent, String substrateDocumentId) {
    Preconditions.checkArgument(substrateDocumentId.startsWith(PREFIX),
        "Not a TextType instance id");
    TextType txt = new TextType(parent.getModel());
    txt.attach(parent, substrateDocumentId);
    return txt;

  }

  public final static String TYPE_NAME = "TextType";
  /** Keep old blip prefix to keep compatibility with document registry */
  public final static String PREFIX = "b";
  public final static String VALUE_ATTR = "t";

  private Model model;
  private Blip blip; // the Wave document as a Blip
  private String initContent;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  private Type parent;
  private String path;
  private boolean isAttached;


  public static boolean isTextBlipId(String blipId) {
    return blipId.startsWith(PREFIX);
  }

  protected TextType(Model model) {
    this.model = model;
    this.isAttached = false;
  }

  protected void setInitContent(String textOrXml) {
    this.initContent = textOrXml;
  }

  @Override
  protected void attach(Type parent) {
    Preconditions.checkArgument(!isAttached, "Already attached text type");
    String substrateDocumentId = model.generateDocId(PREFIX);
    blip = model.createBlip(substrateDocumentId);
    attach(parent, substrateDocumentId);

    if (initContent == null)

    initContent = "";
    // Set the doc's body tag to displayed properly by editor.
    XmlStringBuilder sb =
        XmlStringBuilder.createFromXmlString("<body><line/>" + this.initContent + "</body>");
    blip.getContent().appendXml(sb);

    attach(parent, substrateDocumentId);

  }

  @Override
  protected void attach(Type parent, String substrateDocumentId) {
    this.parent = parent;
    blip = model.getBlip(substrateDocumentId);
    isAttached = true;
  }

  @Override
  protected void deattach() {
    Preconditions.checkArgument(isAttached, "Unable to deattach an unattached TextType");
    isAttached = false;
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
    Preconditions.checkArgument(isAttached, "Unable to serialize an unattached TextType");
    return blip.getId();
  }

  @Override
  public void addListener(Listener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(Listener listener) {
    listeners.remove(listener);
  }

  @Override
  public String getDocumentId() {
    return blip.getId();
  }

  @Override
  public Model getModel() {
    return model;
  }

  @Override
  public String getType() {
    return TYPE_NAME;
  }

  public ObservableDocument getMutableDocument() {
    return blip.getWavelet().getDocument(blip.getId());
  }

  @Override
  protected void setPath(String path) {
    this.path = path;
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

  @Override
  public String getPath() {
    if (path == null && parent != null && isAttached) {
      path = parent.getPath() + "." + parent.getValueReference(this);
    }
    return path;
  }


  //
  // Text operations
  //

  /**
   * Insert a text in the doc's location. Text must not contain XML.
   *
   * @param location
   * @param text
   */
  public void insertText(int location, String text) {
    Document doc = blip.getContent();
    doc.insertText(location, text);
  }

  /**
   * Insert a new line element in a doc's location.
   *
   * @param location
   */
  public void insertNewLine(int location) {
    Document doc = blip.getContent();
    Point<N> point = doc.locate(location);
    doc.createElement(point, "line", Collections.<String, String> emptyMap());
  }

  /**
   * Delete text in the range.
   *
   * @param start
   * @param end
   */
  public void deleteText(int start, int end) {
    Document doc = blip.getContent();
    doc.deleteRange(start, end);
  }

  /**
   * Returns the size of the document.
   *
   * @return
   */
  public int getSize() {
    Document doc = blip.getContent();
    return doc.size();
  }

  /**
   * Returns a XML string of the whole doc. This is function has moderate time
   * cost.
   *
   *
   * @return
   */
  public String getXml() {
    Document doc = blip.getContent();
    return doc.toXmlString();
  }


  public void setAnnotation(int start, int end, String key, String value) {
    Document doc = blip.getContent();
    doc.setAnnotation(start, end, key, value);
  }

  public String getAnnotation(int location, String key) {
    Document doc = blip.getContent();
    return doc.getAnnotation(location, key);
  }

  public Iterable<AnnotationInterval<String>> getAllAnnotations(int start, int end) {
    Document doc = blip.getContent();
    return doc.annotationIntervals(start, end, null);
  }

  @Override
  public ParticipantId getAuthor() {
    return blip.getAuthorId();
  }

  @Override
  public long getLastUpdateTime() {
    return blip.getLastModifiedTime();
  }

  @Override
  public Set<ParticipantId> getContributors() {
    return blip.getContributorIds();
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
    return this;
  }


}
