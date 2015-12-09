package org.swellrt.model.generic;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.model.document.Doc;
import org.waveprotocol.wave.model.document.Document;
import org.waveprotocol.wave.model.document.util.DocHelper;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Date;
import java.util.HashMap;

/**
 * 
 * @author pablojan@gmail (Pablo Ojanguren)
 * 
 */
public class MetadataContainer {

  public static final String TAG_METADATA = "metadata";

  //
  // <metadata pc="" tc="" pm="" tm="" ap="" acl="" p="" />
  //
  protected static final String ATTR_PARTICIPANT_CREATE = "pc";
  protected static final String ATTR_TIMESTAMP_CREATE = "tc";
  protected static final String ATTR_PARTICIPANT_MOD = "pm";
  protected static final String ATTR_TIMESTAMP_MOD = "pm";

  protected static final String ATTR_ACCESS_POLICY = "ap";
  protected static final String ATTR_ACCESS_CONTROL_LIST = "acl";

  public static final String ATTR_PATH = "p";

  /**
   * Create or load a metadata section from a document
   * 
   * @param document
   * @param creator
   * @param path
   * @return
   */
  public static MetadataContainer get(Document document) {

    Preconditions.checkArgument(document != null, "Document is null");
    Doc.E eltMetadata = DocHelper.getElementWithTagName(document, TAG_METADATA);
    String now = String.valueOf((new Date()).getTime());

    if (eltMetadata == null) {
      HashMap<String, String> attributes = new HashMap<String, String>();
      attributes.put(ATTR_PARTICIPANT_CREATE, "");
      attributes.put(ATTR_TIMESTAMP_CREATE, "");
      attributes.put(ATTR_PARTICIPANT_MOD, "");
      attributes.put(ATTR_TIMESTAMP_MOD, now);
      attributes.put(ATTR_ACCESS_POLICY, "default");
      attributes.put(ATTR_ACCESS_CONTROL_LIST, "");
      attributes.put(ATTR_PATH, "");
      eltMetadata =
          document.createChildElement(document.getDocumentElement(), TAG_METADATA, attributes);
    }

    return new MetadataContainer(document, eltMetadata);

  }

  private final Document document;
  private final Doc.E element;

  protected MetadataContainer(Document document, Doc.E metadataElement) {
    this.document = document;
    this.element = metadataElement;
  }


  public String getPath() {
    return document.getAttribute(element, ATTR_PATH);
  }

  public void setPath(String path) {
    document.setElementAttribute(element, ATTR_PATH, path);
  }

  public void setDetachedPath() {
    document.setElementAttribute(element, ATTR_PATH, "detached:" + getPath());
  }

  public void setCreator(ParticipantId creator) {
    document.setElementAttribute(element, ATTR_PARTICIPANT_CREATE, creator.getAddress());
    document.setElementAttribute(element, ATTR_TIMESTAMP_CREATE,
        String.valueOf((new Date()).getTime()));
  }

  public void setModifier(ParticipantId modifier) {
    document.setElementAttribute(element, ATTR_PARTICIPANT_MOD, modifier.getAddress());
    document.setElementAttribute(element, ATTR_TIMESTAMP_MOD,
        String.valueOf((new Date()).getTime()));
  }

}
