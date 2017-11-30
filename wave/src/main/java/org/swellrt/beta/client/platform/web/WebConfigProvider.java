package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.ServiceConfigProvider;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "__swell_config", namespace = JsPackage.GLOBAL)
public class WebConfigProvider implements ServiceConfigProvider {

  @JsProperty
  public native Boolean getCaptureExceptions();

  @JsProperty
  public native Integer getWebsocketHeartbeatInterval();

  @JsProperty
  public native Integer getWebsocketHeartbeatTimeout();

  @JsProperty
  public native Boolean getWebsocketDebugLog();

}
