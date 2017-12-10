package org.swellrt.beta.client.platform.web.browser;


import org.swellrt.beta.client.wave.ws.CloseEvent;
import org.swellrt.beta.client.wave.ws.Event;
import org.swellrt.beta.client.wave.ws.MessageEvent;
import org.swellrt.beta.client.wave.ws.WebSocket.Function;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "WebSocket")
public class BrowserWebSocket {

  @JsConstructor
  public BrowserWebSocket(String server) {
  }

  public Function<Event> onopen;

  public Function<MessageEvent> onmessage;

  public Function<CloseEvent> onclose;

  public Function<Event> onerror;

  public int readyState; // read-only

  public native void close();

  public native void send(String data);

}
