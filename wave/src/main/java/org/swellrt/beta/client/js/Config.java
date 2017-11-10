package org.swellrt.beta.client.js;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "__swell_config", namespace = JsPackage.GLOBAL)
public class Config {

  @JsProperty
  public static native Boolean getCaptureExceptions();

  @JsProperty
  public static native Integer getWebsocketHeartbeatInterval();

  @JsProperty
  public static native Integer getWebsocketHeartbeatTimeout();

  @JsProperty
  public static native Boolean getWebsocketDebugLog();

}
