package org.swellrt.beta.client.platform.web.editor.caret;

import jsinterop.annotations.JsFunction;

/**
 * Caret views can be created from custom code providing a factory as editor
 * config. property. This is the interface for them.
 *
 */
@JsFunction
public interface CaretViewFactory {

  public CaretView exec();

}
