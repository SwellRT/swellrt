package org.swellrt.beta.client;

public class ServiceConfig {

  /** TODO eventually avoid this global static dependency */
  public static ServiceConfigProvider configProvider;

  public static final int WS_HEARBEAT_INTERVAL = 60000; // ms
  public static final int WS_HEARBEAT_TIMEOUT = 2000; // ms
  public static final boolean WS_ENABLE_LOG = false;

  public static final boolean CAPTURE_EXCEPTIONS = false;

  public static final int RPC_INITIAL_BACKOFF = 1000; // ms
  public static final int RPC_MAX_BACKOFF = 60000; // ms

  public static final int RPC_MAX_BURST_RATE = 10;
  public static final double RPC_MAX_STEADY_RATE = 1.0;
  public static final double RPC_MIN_RETRY_TIME = 5000; // ms

  public static final int PRESENCE_PING_RATE = 10000; // ms

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
    try {
      Integer value = configProvider.getWebsocketHeartbeatInterval();
      return value != null && checkPositiveInteger(value) ? value : WS_HEARBEAT_INTERVAL;
    } catch (RuntimeException e) {
      return WS_HEARBEAT_INTERVAL;
    }
  }

  public final static int websocketHeartbeatTimeout() {
    try {
      Integer value = configProvider.getWebsocketHeartbeatTimeout();
      return value != null && checkPositiveInteger(value) ? value : WS_HEARBEAT_TIMEOUT;
    } catch (RuntimeException e) {
      return WS_HEARBEAT_TIMEOUT;
    }
  }

  public final static boolean websocketDebugLog() {
    try {
      Boolean value = configProvider.getWebsocketDebugLog();
      return value != null ? value : WS_ENABLE_LOG;
    } catch (RuntimeException e) {
      return WS_ENABLE_LOG;
    }
  }

  public final static boolean captureExceptions() {

    try {
      Boolean value = configProvider.getCaptureExceptions();
      return value != null ? value : CAPTURE_EXCEPTIONS;
    } catch (RuntimeException e) {
      return CAPTURE_EXCEPTIONS;
    }

  }

  public final static int rpcInitialBackoff() {
    return RPC_INITIAL_BACKOFF;
  }

  public final static int rpcMaxBackoff() {
    return RPC_MAX_BACKOFF;
  }

  public final static int presencePingRateMs() {
    try {
      Integer value = configProvider.getTrackPresencePingRateMs();
      return value != null && checkPositiveInteger(value) ? value : PRESENCE_PING_RATE;
    } catch (RuntimeException e) {
      return PRESENCE_PING_RATE;
    }
  }
}
