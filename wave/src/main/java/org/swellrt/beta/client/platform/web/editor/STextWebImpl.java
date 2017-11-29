package org.swellrt.beta.client.platform.web.editor;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SUtils;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.common.util.LogicalPanel.Impl;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

public class STextWebImpl implements STextWeb {

  private final InteractiveDocument idoc;
  private final ContentDocument doc;
  private LogicalPanel.Impl docDisplay;

  protected STextWebImpl(ContentDocument doc) {
    this.doc = doc;
    this.idoc = null;
  }

  protected STextWebImpl(InteractiveDocument doc) {
    this.idoc = doc;
    this.doc = doc.getDocument();
  }

  @Override
  public InteractiveDocument getInteractiveDocument() {
    return idoc;
  }

  /**
   * Attach and display this document into a DOM container.
   *
   * @param element
   * @throws SException
   */
  @Override
  public void setParent(Element element) throws SException {

    Preconditions.checkArgument(element != null, "DOM element is null");

    docDisplay = new LogicalPanel.Impl() {
      {
        setElement(element);
      }
    };

    doc.setRendering();

    docDisplay.getElement().appendChild(
        doc.getFullContentView().getDocumentElement().getImplNodelet());

  }


  @Override
  public void setRendered() {
    doc.setRendering();
  }


  @Override
  public void setInteractive() throws SException {
    if (docDisplay == null) {
      docDisplay = new LogicalPanel.Impl() {
        {
          setElement(Document.get().createDivElement());
        }
      };
    }
    try {
      doc.setInteractive(docDisplay);
    } catch (IllegalStateException e) {
      throw new SException(SException.INTERNAL_ERROR, e, "Document can't move to interactive state");
    }
  }


  @Override
  public void setShelved() {
    try {
      doc.setShelved();
    } catch (IllegalStateException e) {

    }
  }

  @Override
  public void setInteractive(Impl panel) throws SException {
    try {
      doc.setInteractive(panel);
    } catch (IllegalStateException e) {
      throw new SException(SException.INTERNAL_ERROR, e, "Document can't move to interactive state");
    }
  }

  @Override
  public boolean isEmpty() {
    return SUtils.isEmptyDocument(doc.getMutableDoc());
  }

  @Override
  public Range insert(Range at, String content) {
    Preconditions.checkArgument(at != null, "Can't insert text in a null position");
    content = SUtils.sanitizeString(content);
    if (content.length() > 0) {
      doc.getMutableDoc().insertText(at.getStart(), content);
      return new Range(at.getStart(), at.getStart() + content.length());
    }
    return null;
  }

  @Override
  public Range replace(Range at, String content) {
    Preconditions.checkArgument(at != null, "Can't replace text of a null range");
    if (!at.isCollapsed())
      doc.getMutableDoc().deleteRange(at.getStart(), at.getEnd());

    return insert(at, content);
  }

  @Override
  public ContentDocument getContentDocument() {
    return doc;
  }

  @Override
  public void showDiffHighlight() {
    if (idoc != null) {
      idoc.startShowDiffs();
    }
  }

  @Override
  public void hideDiffHighlight() {
    if (idoc != null) {
      idoc.stopShowDiffs();
    }
  }



}
