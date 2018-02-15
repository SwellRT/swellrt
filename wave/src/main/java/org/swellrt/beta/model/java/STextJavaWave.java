package org.swellrt.beta.model.java;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.wave.SubstrateId;
import org.swellrt.beta.model.wave.mutable.SWaveNodeManager;
import org.swellrt.beta.model.wave.mutable.SWaveText;
import org.waveprotocol.wave.client.editor.playback.DocHistory.Iterator;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.wave.Blip;

public class STextJavaWave extends SWaveText {

  protected STextJavaWave(SWaveNodeManager nodeManager, SubstrateId substrateId, Blip blip) {
    super(nodeManager, substrateId, blip);
  }

  @Override
  public SMap getLiveCarets() {
    return getNodeManager().getTransient().getCaretsForDocument(getSubstrateId().getDocumentId());
  }

  @Override
  public Iterator getHistoryIterator() {
    return null;
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
    return blip.getContent().toXmlString();
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
  public DocInitialization asDocInitialization() {
    return blip.getContent().toInitialization();
  }

  @Override
  public String asXmlString() {
    return blip.getContent().toXmlString();
  }

}
