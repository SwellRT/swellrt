package org.swellrt.beta.client.platform.web.editor;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.Blip;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsOptional;

/**
 * A text document supported by a remote wave document.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class STextWebRemote extends SWaveText implements STextWeb {

  private final InteractiveDocument interactiveDoc;
  private final LogicalPanel panel = new LogicalPanel.Impl() {
    {
      setElement(Document.get().createDivElement());
    }
  };

  public STextWebRemote(SWaveNodeManager nodeManager, SubstrateId substrateId, Blip blip,
      DocInitialization docInit, InteractiveDocument interactiveDoc) {
    super(nodeManager, substrateId, blip);

    Preconditions.checkArgument(interactiveDoc != null,
        "STextWebRemote object requires a InteractiveDocument");

    if (docInit != null && interactiveDoc != null) {
      interactiveDoc.getDocument().consume(docInit);
    }

    this.interactiveDoc = interactiveDoc;
  }


  @Override
  public SMap getLiveCarets() {
    return getNodeManager().getTransient().getCaretsForDocument(getSubstrateId().getDocumentId());
  }

  @Override
  public SNode node(String path) throws SException {
    return null;
  }

  @Override
  public void set(String path, Object value) {

  }

  @Override
  public Object get(String path) {
    return null;
  }

  @Override
  public void push(String path, Object value) {
  }

  @Override
  public Object pop(String path) {
    return null;
  }

  @Override
  public void delete(String path) {
  }

  @Override
  public int length(String path) {
    return 0;
  }

  @Override
  public boolean contains(String path, String property) {
    return false;
  }

  @Override
  public void accept(SVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public SMap asMap() {
    return null;
  }

  @Override
  public SList<? extends SNode> asList() {
    return null;
  }

  @Override
  public String asString() {
    return this.interactiveDoc.getDocument().getMutableDoc().toXmlString();
  }

  @Override
  public double asDouble() {
    return 0;
  }

  @Override
  public int asInt() {
    return 0;
  }

  @Override
  public boolean asBoolean() {
    return false;
  }

  @Override
  public SText asText() {
    return this;
  }

  @Override
  public InteractiveDocument getContentDocument() {
    return interactiveDoc;
  }

  @Override
  public DocInitialization asDocInitialization() {
    return interactiveDoc.getDocument().asOperation();
  }

  @Override
  public String asXmlString() {
    return this.interactiveDoc.getDocument().getMutableDoc().toXmlString();
  }


  @Override
  public void attachToDOM(Element element) {
    Preconditions.checkNotNull(element, "Can't attach text to empty element");
    interactiveDoc.getDocument().setInteractive(panel);
    Element textElement = interactiveDoc.getDocument().getFullContentView().getDocumentElement()
        .getImplNodelet();
    element.appendChild(textElement);
  }

  @Override
  public void deattachFromDOM() {
    Element textElement = interactiveDoc.getDocument().getFullContentView().getDocumentElement()
        .getImplNodelet();

    if (textElement != null) {
      textElement.removeFromParent();
      interactiveDoc.getDocument().setShelved();
    }

  }

  @Override
  public SPlaybackText getPlaybackTextFor(@JsOptional String historyType) {

    if (historyType == null || REV_HISTORY.equals(historyType))
      return SPlaybackText.createForRevisionHistory(this);
    else if (historyType.equals(TAG_HISTORY))
      return SPlaybackText.createForTagHistory(this);

    return null;
  }

  @Override
  public boolean isAttachedToDOM() {
    Element nodelet = interactiveDoc.getDocument().getFullContentView().getDocumentElement()
        .getImplNodelet();
    return nodelet != null && nodelet.getParentElement() != null;
  }

}
