package org.swellrt.beta.client.platform.web.editor.caret;

import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsType;

/**
 * Interface for caret's DOM and some mutable properties that can be provided as
 * native Javascript. *
 */
@JsType(isNative = true, namespace = "swell", name = "Caret")
public interface CaretView {

  public Element element();

  public String compositionState(String state);

  /** set the caret's session and user info */
  public CaretInfo info(CaretInfo info);

}
