package org.swellrt.beta.client.platform.web.browser;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "localStorage")
public class LocalStorage {
  
  public static native Object getItem(String name);
  
  public static native void setItem(String name, Object value);
  
  private LocalStorage() {
    
  }
}
