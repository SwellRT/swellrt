package org.swellrt.model.generic;

import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.Blip;
import org.waveprotocol.wave.model.wave.SourcesEvents;

public class TextType extends Type implements SourcesEvents<TextType.Listener> {

  public interface Listener {

  }

  protected static Type createAndAttach(Model model, String id) {

    Preconditions.checkArgument(id.startsWith(PREFIX), "Not a TextType instance id");
    TextType txt = new TextType(model);
    txt.attach(id);
    return txt;

  }

  /** Keep old blip prefix to keep compatibility with document registry */
  public final static String PREFIX = "b";
  public final static String VALUE_ATTR = "t";

  private Model model;
  private Blip blip; // the Wave document as a Blip
  private String initContent;

  private final CopyOnWriteSet<Listener> listeners = CopyOnWriteSet.create();

  private boolean isAttached;

  protected TextType(Model model) {
    this.model = model;
    this.isAttached = false;
  }

  protected void setInitContent(String textOrXml) {
    this.initContent = textOrXml;
  }


  @Override
  protected void attach(String docId) {

    if (docId == null) {
      // Creating a new blip
      docId = model.generateDocId(PREFIX);
      blip = model.createBlip(docId);

      if (initContent == null)
        initContent = "";
      // Set the doc's body tag to displayed properly by editor.
      XmlStringBuilder sb = XmlStringBuilder.createFromXmlString("<body><line/>"+this.initContent+"</body>");
      blip.getContent().appendXml(sb);

    } else {
      blip = model.getBlip(docId);
    }
    Preconditions.checkNotNull(blip, "Unable to attach TextType, couldn't create or get blip");
    isAttached = true;
  }

  @Override
  protected void deattach() {
    Preconditions.checkArgument(isAttached, "Unable to deattach an unattached TextType");
    // nothing to do. wavelet doesn't provide doc deletion
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

  @Override
  protected String getPrefix() {
    return PREFIX;
  }

  @Override
  protected boolean isAttached() {
    return isAttached;
  }

  @Override
  protected String serializeToModel() {
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

  public ContentDocument getDocument() {
    Preconditions
        .checkArgument(isAttached, "ContentDocument not available for unattached TextType");
    return model.getDocument(blip);
  }

}
