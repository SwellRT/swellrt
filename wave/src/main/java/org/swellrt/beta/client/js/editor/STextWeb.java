package org.swellrt.beta.client.js.editor;


import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.editor.content.ContentDocument;

import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "TextWeb")
public interface STextWeb {
  
  public void setInteractive() throws SException;
  
  @JsIgnore
  public void setInteractive(LogicalPanel.Impl panel) throws SException;
  
  public void setParent(Element element) throws SException;

  public void setRendered();
  
  public void setShelved();
  
  @JsIgnore
  public ContentDocument getContentDocument();
  
}
