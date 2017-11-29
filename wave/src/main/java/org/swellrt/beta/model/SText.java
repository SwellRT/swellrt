package org.swellrt.beta.model;


import org.swellrt.beta.client.platform.web.editor.STextLocalWeb;
import org.swellrt.beta.common.Platform;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.document.operation.DocInitialization;

import com.google.gwt.core.shared.GWT;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Text")
public interface SText extends SNode {

  @JsIgnore
  public final static Platform PLATFORM = GWT.create(Platform.class);

  public static SText create(@JsOptional Object data) throws SException {
    String text = "";
    if (data != null && data instanceof String)
      text = (String) data;

    if (PLATFORM.isWeb())
      return STextLocalWeb.create(text);
    else
      return null; // not implementation available yet

  }

  public String getRawContent();

  public boolean isEmpty();

  /**
   * A representation of the text data suitable to
   * inject in new documents.
   * @return
   */
  @JsIgnore
  public DocInitialization getInitContent();

  /**
   * Cosume raw operations
   *
   * @param ops
   */
  @JsIgnore
  public void setInitContent(DocInitialization ops);
}
