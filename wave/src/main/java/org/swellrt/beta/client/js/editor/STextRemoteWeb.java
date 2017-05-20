package org.swellrt.beta.client.js.editor;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.remote.SObjectRemote;
import org.swellrt.beta.model.remote.STextRemote;
import org.swellrt.beta.model.remote.SubstrateId;
import org.waveprotocol.wave.client.common.util.LogicalPanel.Impl;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.Nindo;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.wave.Blip;

import com.google.gwt.dom.client.Element;

/**
 * A text document supported by a remote wave document.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class STextRemoteWeb extends STextRemote implements STextWeb {

  private final STextWebImpl textWeb;

  public STextRemoteWeb(SObjectRemote object, SubstrateId substrateId, Blip blip, InteractiveDocument doc) {
    super(object, substrateId, blip);
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
}
