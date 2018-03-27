package org.swellrt.beta.client;

import org.swellrt.beta.client.wave.WaveWebSocketClient;

import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Constants")
public class ServiceConstants {

  public static final String ANONYMOUS_USER_ID = "!";

  public static final String STATUS_CONNECTED = WaveWebSocketClient.ConnectState.CONNECTED.toString();
  public static final String STATUS_DISCONNECTED = WaveWebSocketClient.ConnectState.DISCONNECTED.toString();
  public static final String STATUS_ERROR = WaveWebSocketClient.ConnectState.ERROR.toString();
  public static final String STATUS_CONNECTING = WaveWebSocketClient.ConnectState.CONNECTING.toString();
  public static final String STATUS_TURBULENCE = WaveWebSocketClient.ConnectState.TURBULENCE
      .toString();

  public static final int OBJECT_UNKNOW_STATUS = -1;
  public static final int OBJECT_ERROR = 1;
  public static final int OBJECT_UPDATE = 2;
  public static final int OBJECT_CLOSED = 3;
  public static final int OBJECT_PARTICIPANT_ADDED = 4;
  public static final int OBJECT_PARTICIPANT_REMOVED = 5;

}
