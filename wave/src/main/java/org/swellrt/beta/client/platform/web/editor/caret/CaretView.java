package org.swellrt.beta.client.platform.web.editor.caret;

import com.google.gwt.dom.client.Element;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Interface for caret's DOM and some mutable properties that can be provided as
 * native Javascript. *
 */
@JsType(isNative = true, namespace = "swell", name = "Caret")
public interface CaretView {

  /** the DOM element for the caret */
  @JsProperty
  public Element getElement();

  /** update caret's session and user info */
  public CaretInfo update(CaretInfo info);

}
