package org.swellrt.beta.model;


import org.swellrt.beta.client.platform.web.editor.caret.CaretInfo;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.model.document.operation.DocInitialization;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Text")
public interface SText extends SNode {

  public static SText create(@JsOptional Object data) throws SException {
    String text = "";
    if (data != null && data instanceof String)
      text = (String) data;

    return ModelFactory.instance.createLocalText(text);
  }

  /**
   * The returned map stores pairs of (sessionId, {@link CaretInfo}) that must
   * be updated by editors.
   *
   * @return a map of carets' metadata.
   */
  SMap getLiveCarets();

  /**
   * @return an iterator for the document history, starting at the most recent
   *         version.
   */
  DocHistory.Iterator getHistoryIterator();

  /**
   * @return content's of this object as a initial document operation.
   */
  @JsIgnore
  DocInitialization asDocInitialization();

  /**
   * @return the XML view of the current state of this document.
   */
  @JsIgnore
  String asXmlString();
}
