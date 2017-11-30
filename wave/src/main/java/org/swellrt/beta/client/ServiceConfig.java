package org.swellrt.beta.client;

public class ServiceConfig {

  /** TODO eventually avoid this global static dependency */
  public static ServiceConfigProvider configProvider;

  /**
   * Yes, this method looks stupid. But when passing a javascript value as
   * parameter it captures undefined values. <br>
   * This method can throw RuntimeException.
   *
   * @param value
   * @return
   */
  private static boolean checkPositiveInteger(Integer value) {
    return value > 0;
  }

  public final static int websocketHeartbeatInterval() {
    int DEFAULT = 60000; // ms
    try {
      Integer value = configProvider.getWebsocketHeartbeatInterval();
      return checkPositiveInteger(value) ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  public final static int websocketHeartbeatTimeout() {
    int DEFAULT = 2000; // ms
    try {
      Integer value = configProvider.getWebsocketHeartbeatTimeout();
      return checkPositiveInteger(value) ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  public final static boolean websocketDebugLog() {
    boolean DEFAULT = false;
    try {
      Boolean value = configProvider.getWebsocketDebugLog();
      return value != null ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  public final static boolean captureExceptions() {
    boolean DEFAULT = true;
    try {
      Boolean value = configProvider.getCaptureExceptions();
      return value != null ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

}
