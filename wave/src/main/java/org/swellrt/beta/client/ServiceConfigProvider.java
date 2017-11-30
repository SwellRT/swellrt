package org.swellrt.beta.client;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "__swell_config", namespace = JsPackage.GLOBAL)
public interface ServiceConfigProvider {

  @JsProperty
  public boolean getCaptureExceptions();

  @JsProperty
  public int getWebsocketHeartbeatInterval();

  @JsProperty
  public int getWebsocketHeartbeatTimeout();

  @JsProperty
  public boolean getWebsocketDebugLog();

}
