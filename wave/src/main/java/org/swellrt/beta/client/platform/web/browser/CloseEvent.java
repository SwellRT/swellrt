package org.swellrt.beta.client.platform.web.browser;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "CloseEvent")
public class CloseEvent extends Event {
  
  @JsConstructor
  public CloseEvent() {
    
  }
  
  public int code;
  
  public String reason;
  
  public boolean wasClean;
  
}
