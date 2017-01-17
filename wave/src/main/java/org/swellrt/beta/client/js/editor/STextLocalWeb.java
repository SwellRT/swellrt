package org.swellrt.beta.client.js.editor;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.local.STextLocal;
import org.swellrt.beta.model.wave.SWaveSchemas;
import org.waveprotocol.wave.client.common.util.LogicalPanel.Impl;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.util.DocProviders;

import com.google.gwt.dom.client.Element;

public class STextLocalWeb implements STextWeb, STextLocal {

    
  public static STextLocalWeb create(String text) throws SException {
    
    String xml = "<body><line/>"+text+"</body>";
    
    DocInitialization op;
    try {
      op = DocProviders.POJO.parse(xml).asOperation();
    } catch (IllegalArgumentException e) {
      throw new SException(SException.INTERNAL_ERROR, e);
    }
  
    return new STextLocalWeb(new ContentDocument(Editor.ROOT_REGISTRIES, op, SWaveSchemas.STEXT_SCHEMA_CONSTRAINTS));
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
  public ContentDocument getContentDocument() {
    return textWeb.getContentDocument();
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


}
