package org.swellrt.beta.model.java;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.local.SMapLocal;
import org.waveprotocol.wave.client.editor.playback.DocHistory.Iterator;
import org.waveprotocol.wave.model.document.MutableDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.util.DocProviders;

public class STextJavaLocal implements SText {

  public static STextJavaLocal create(String text) throws SException {

    String xml = "<body><line/>" + text + "</body>";
    return new STextJavaLocal(DocProviders.MOJO.parse(xml));

  }

  private final MutableDocument doc;
  private final SMap fakeCaretMap = new SMapLocal();

  protected STextJavaLocal(MutableDocument mutableDoc) {
    this.doc = mutableDoc;
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
    return doc.toXmlString();
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
  public SMap getLiveCarets() {
    return fakeCaretMap;
  }

  @Override
  public Iterator getHistoryIterator() {
    return null;
  }

  @Override
  public DocInitialization asDocInitialization() {
    return doc.toInitialization();
  }

  @Override
  public String asXmlString() {
    return doc.toXmlString();
  }

}
