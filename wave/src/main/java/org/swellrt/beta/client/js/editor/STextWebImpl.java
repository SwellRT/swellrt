package org.swellrt.beta.client.js.editor;

import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.common.util.LogicalPanel.Impl;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

public class STextWebImpl implements STextWeb {
  
  private final ContentDocument doc;
  private LogicalPanel.Impl docDisplay;
  
  protected STextWebImpl(ContentDocument doc) {
    this.doc = doc;
  }
  
  @Override
  public ContentDocument getContentDocument() {
    return doc;
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
    
    docDisplay.getElement().appendChild(
        doc.getFullContentView().getDocumentElement().getImplNodelet());

  }
  
  
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



}
