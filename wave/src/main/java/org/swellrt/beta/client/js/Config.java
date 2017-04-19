package org.swellrt.beta.client.js;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name="__swellrt_config", namespace = JsPackage.GLOBAL)

public class Config {

  @JsProperty
  public static native Integer getWebSocketKeepAliveInterval();

  
  @JsOverlay
  public final static int webSocketKeepAliveInterval() {
    int DEFAULT = 60000; // ms
    try {
      Integer value = getWebSocketKeepAliveInterval();
      return value != null ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }   
  }
  
}
