package org.swellrt.beta.client;

import org.swellrt.beta.client.js.Config;
import org.swellrt.beta.client.js.Console;

public class ServiceConfig {

  /**
   * Yes, this method looks absurd. But passing a native value
   * as parameter allows to check undefined values.
   * <br>
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
      Integer value = Config.getWebsocketHeartbeatInterval();
      Console.log("websocketHeartbeatInterval is " + value);
      return checkPositiveInteger(value) ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  public final static int websocketHeartbeatTimeout() {
    int DEFAULT = 2000; // ms
    try {
      Integer value = Config.getWebsocketHeartbeatTimeout();
      Console.log("websocketHeartbeatTimeout is " + value);
      return checkPositiveInteger(value) ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  public final static boolean websocketDebugLog() {
    boolean DEFAULT = false;
    try {
      Boolean value = Config.getWebsocketDebugLog();
      return value != null ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

  public final static boolean captureExceptions() {
    boolean DEFAULT = true;
    try {
      Boolean value = Config.getCaptureExceptions();
      return value != null ? value : DEFAULT;
    } catch (RuntimeException e) {
      return DEFAULT;
    }
  }

}
