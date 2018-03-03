package org.swellrt.beta.client.platform.web.editor;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SList;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SText;
import org.swellrt.beta.model.SVisitor;
import org.swellrt.beta.model.local.SMapLocal;
import org.swellrt.beta.model.wave.WaveSchemas;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.util.DocProviders;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsOptional;

public class STextWebLocal implements STextWeb {


  public static STextWebLocal create(String text) throws SException {

    String xml = "<body><line/>"+text+"</body>";

    DocInitialization op;
    try {
      op = DocProviders.POJO.parse(xml).asOperation();
    } catch (IllegalArgumentException e) {
      throw new SException(SException.INTERNAL_ERROR, e);
    }

    return new STextWebLocal(
        new ContentDocument(Editor.ROOT_REGISTRIES, op, WaveSchemas.STEXT_SCHEMA_CONSTRAINTS));

  }

  private final ContentDocument doc;
  private final SMap fakeCaretMap = new SMapLocal();
  private final LogicalPanel panel = new LogicalPanel.Impl() {
    {
      setElement(Document.get().createDivElement());
    }
  };

  protected STextWebLocal(ContentDocument doc) {
    this.doc = doc;
  }

  @Override
  public SMap getLiveCarets() {
    return this.fakeCaretMap;
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
    return doc.getMutableDoc().toXmlString();
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
    return new InteractiveDocument() {

      @Override
      public void stopShowDiffs() {
      }

      @Override
      public void stopRendering() {
        doc.setShelved();
      }

      @Override
      public void stopDiffSuppression() {

      }

      @Override
      public void stopDiffRetention() {
      }

      @Override
      public void startShowDiffs() {
      }

      @Override
      public void startRendering(Registries registries, LogicalPanel panel) {
        doc.setInteractive(panel);
      }

      @Override
      public void startDiffSuppression() {
      }

      @Override
      public void startDiffRetention() {
      }

      @Override
      public boolean isCompleteDiff() {
        return false;
      }

      @Override
      public ContentDocument getDocument() {
        return doc;
      }

      @Override
      public void clearDiffs() {
      }
    };
  }


  @Override
  public DocInitialization asDocInitialization() {
    return doc.getMutableDoc().toInitialization();
  }

  @Override
  public String asXmlString() {
    return doc.getMutableDoc().toXmlString();
  }

  @Override
  public void attachToDOM(Element element) {
    Preconditions.checkNotNull(element, "Can't attach text to empty element");
    doc.setInteractive(panel);
    Element textElement = doc.getFullContentView().getDocumentElement()
        .getImplNodelet();
    element.appendChild(textElement);
  }

  @Override
  public void deattachFromDOM() {
    Element textElement = doc.getFullContentView().getDocumentElement().getImplNodelet();

    if (textElement != null) {
      textElement.removeFromParent();
      doc.setShelved();
    }
  }

  @Override
  public SPlaybackText getPlaybackTextFor(@JsOptional String historyType) {
    // Local texts can't have playback
    return null;
  }

}
