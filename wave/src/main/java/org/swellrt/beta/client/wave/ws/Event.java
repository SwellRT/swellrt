package org.swellrt.beta.client.wave.ws;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Event")
public class Event {

  @JsConstructor
  public Event() {
    
  }
  
  public String type;
  
  
}
