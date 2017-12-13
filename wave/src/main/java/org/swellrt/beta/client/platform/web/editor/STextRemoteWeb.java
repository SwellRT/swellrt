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
import org.waveprotocol.wave.client.common.util.LogicalPanel.Impl;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.wave.Blip;

import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsIgnore;

/**
 * A text document supported by a remote wave document.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class STextRemoteWeb extends SWaveText implements STextWeb {

  private final STextWebImpl textWeb;

  public STextRemoteWeb(SWaveNodeManager nodeManager, SubstrateId substrateId, Blip blip, InteractiveDocument doc) {
    super(nodeManager, substrateId, blip);
    this.textWeb = new STextWebImpl(doc);
  }


  @Override
  public String getRawContent() {
    return textWeb.getContentDocument().getMutableDoc().toXmlString();
  }

  @Override
  public DocInitialization getInitContent() {
    return textWeb.getContentDocument().getMutableDoc().toInitialization();
  }

  @Override
  public InteractiveDocument getInteractiveDocument() {
    return textWeb.getInteractiveDocument();
  }

  @Override
  public void setInteractive() throws SException {
    textWeb.setInteractive();
  }


  @Override
  public void setParent(Element element) throws SException {
    textWeb.setParent(element);
  }


  @Override
  public void setShelved() {
    textWeb.setShelved();
  }

  @Override
  public void setRendered() {
    textWeb.setRendered();
  }

  @Override
  public void setInteractive(Impl panel) throws SException {
    textWeb.setInteractive(panel);
  }

  @Override
  public void setInitContent(DocInitialization ops) {
    textWeb.getContentDocument().getMutableDoc().hackConsume(Nindo.fromDocOp(ops, true));
  }

  @Override
  public boolean isEmpty() {
    return textWeb.isEmpty();
  }


  @Override
  public Range insert(Range at, String content) {
    return textWeb.insert(at, content);
  }

  @Override
  public Range replace(Range at, String content) {
    return textWeb.replace(at, content);
  }


  @Override
  public ContentDocument getContentDocument() {
    return textWeb.getContentDocument();
  }


  @Override
  public void showDiffHighlight() {
    InteractiveDocument idoc = textWeb.getInteractiveDocument();
    if (idoc != null) {
      idoc.startShowDiffs();
    }
  }


  @Override
  public void hideDiffHighlight() {
    InteractiveDocument idoc = textWeb.getInteractiveDocument();
    if (idoc != null) {
      idoc.stopShowDiffs();
    }
  }


  @SuppressWarnings("rawtypes")
  @JsIgnore
  @Override
  public void accept(SVisitor visitor) {
    visitor.visit(this);
  }

  //
  // -----------------------------------------------------
  //

  @Override
  public void set(String path, Object value) {
  }

  @Override
  public void push(String path, Object value) {
  }

  @Override
  public Object pop(String path) {
    return null;
  }

  @Override
  public int length(String path) {
    return -1;
  }

  @Override
  public boolean contains(String path, String property) {
    return false;
  }

  @Override
  public void delete(String path) {
  }

  @Override
  public Object get(String path) {
    return null;
  }

  @Override
  public SNode node(String path) throws SException {
    return null;
  }

  @Override
  public SMap asMap() {
    throw new IllegalStateException("Node is not a map");
  }

  @Override
  public SList<? extends SNode> asList() {
    throw new IllegalStateException("Node is not a list");
  }

  @Override
  public String asString() {
    throw new IllegalStateException("Node is not a string");
  }

  @Override
  public double asDouble() {
    throw new IllegalStateException("Node is not a number");
  }

  @Override
  public int asInt() {
    throw new IllegalStateException("Node is not a number");
  }

  @Override
  public boolean asBoolean() {
    throw new IllegalStateException("Node is not a boolean");
  }

  @Override
  public SText asText() {
    return this;
  }
}
