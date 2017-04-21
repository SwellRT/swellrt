package org.swellrt.beta.client.js;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true, name = "__swellrt_config", namespace = JsPackage.GLOBAL)
public class Config {

  @JsProperty
  public static native Boolean getCaptureExceptions();

  @JsProperty
  public static native Integer getWebsocketHeartbeatInterval();

  @JsProperty
  public static native Integer getWebsocketHeartbeatTimeout();

  @JsProperty
  public static native Boolean getWebsocketDebugLog();

  @JsOverlay
  public final static int websocketHeartbeatInterval() {
    int DEFAULT = 60000; // ms
    try {
      Integer value = getWebsocketHeartbeatInterval();
      return value != null ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  @JsOverlay
  public final static int websocketHeartbeatTimeout() {
    int DEFAULT = 2000; // ms
    try {
      Integer value = getWebsocketHeartbeatTimeout();
      return value != null ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  @JsOverlay
  public final static boolean websocketDebugLog() {
    boolean DEFAULT = false;
    try {
      Boolean value = getWebsocketDebugLog();
      return value != null ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  @JsOverlay
  public final static boolean captureExceptions() {
    boolean DEFAULT = true;
    try {
      Boolean value = getCaptureExceptions();
      return value != null ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

}
