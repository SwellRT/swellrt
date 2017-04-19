package org.swellrt.beta.client.js.event;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "MessageEvent")
public class MessageEvent extends Event {
  
  @JsConstructor
  public MessageEvent() {
    
  }
  
  public Object data;
  
  public String origin;
  
}