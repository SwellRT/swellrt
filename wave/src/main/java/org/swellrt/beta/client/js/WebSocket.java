package org.swellrt.beta.client.js;


import org.swellrt.beta.client.js.event.CloseEvent;
import org.swellrt.beta.client.js.event.Event;
import org.swellrt.beta.client.js.event.MessageEvent;

import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "WebSocket")
public class WebSocket {

  @JsOverlay
  public static final int CONNECTING = 0;
  
  @JsOverlay
  public static final int OPEN = 1;
  
  @JsOverlay
  public static final int CLOSING = 2;
  
  @JsOverlay
  public static final int CLOSED = 3;
  
  
  @JsFunction
  public interface Function<T> {
    void exec(T o);
  }
 
  @JsConstructor
  public WebSocket(String server) {
  }  
  
  public Function<Event> onopen;
  
  public Function<MessageEvent> onmessage;
  
  public Function<CloseEvent> onclose;
  
  public Function<Event> onerror;
  
  public int readyState; // read-only
  
  public native void close();
  
  public native void send(String data);
  
}
