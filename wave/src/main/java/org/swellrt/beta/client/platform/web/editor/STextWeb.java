package org.swellrt.beta.client.platform.web.editor;


import org.swellrt.beta.model.SText;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.client.wave.InteractiveDocument;

import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "TextWeb")
public interface STextWeb extends SText {

  @JsIgnore
  public InteractiveDocument getInteractiveDocument();

  @JsIgnore
  public ContentDocument getContentDocument();

  @JsIgnore
  public DocHistory getDocHistory();

  /** @return the root DOM element of the rendered view of the text */
  public Element getElement();

}
