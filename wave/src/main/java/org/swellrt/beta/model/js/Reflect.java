package org.swellrt.beta.model.js;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public class Reflect {
  
  public static native Object getOwnPropertyDescriptor(Object target, String key);

}
