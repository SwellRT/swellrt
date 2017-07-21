package org.swellrt.beta.client.js.editor;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.local.STextLocal;
import org.swellrt.beta.model.wave.WaveSchemas;
import org.waveprotocol.wave.client.common.util.LogicalPanel.Impl;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.document.util.Range;

import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsOptional;

public class STextLocalWeb implements STextWeb, STextLocal {


  public static STextLocalWeb create(String text) throws SException {

    String xml = "<body><line/>"+text+"</body>";

    DocInitialization op;
    try {
      op = DocProviders.POJO.parse(xml).asOperation();
    } catch (IllegalArgumentException e) {
      throw new SException(SException.INTERNAL_ERROR, e);
    }

    return new STextLocalWeb(new ContentDocument(Editor.ROOT_REGISTRIES, op, WaveSchemas.STEXT_SCHEMA_CONSTRAINTS));
  }

  private final STextWebImpl textWeb;

  protected STextLocalWeb(ContentDocument doc) {
    this.textWeb = new STextWebImpl(doc);
  }

  @Override
  public String getRawContent() {
    return textWeb.getContentDocument().getMutableDoc().toXmlString();
  }

  @Override
  public DocInitialization getInitContent() {
    return textWeb.getContentDocument().asOperation();
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
    // nothing to do
  }

  @Override
  public void hideDiffHighlight() {
    // nothing to do
  }


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
  public void push(String path, Object value, @JsOptional Object index) {
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
}
