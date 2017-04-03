package org.swellrt.beta.model.js;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL)
public class Reflect {
  
  public static native boolean set(Object target, String key, Object value);
  public static native boolean has(Object target, String key);
  public static native boolean defineProperty(Object target, String key, Object attributes);
  public static native Object getOwnPropertyDescriptor(Object target, String key);

}
