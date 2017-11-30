package org.swellrt.beta.client;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "__swell_config", namespace = JsPackage.GLOBAL)
public interface ServiceConfigProvider {

  public Boolean getCaptureExceptions();

  public Integer getWebsocketHeartbeatInterval();

  public Integer getWebsocketHeartbeatTimeout();

  public Boolean getWebsocketDebugLog();

}
