package org.swellrt.beta.model.js;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name="window")
public class Global {

  @JsProperty
  public static native Object getUndefined();

}
