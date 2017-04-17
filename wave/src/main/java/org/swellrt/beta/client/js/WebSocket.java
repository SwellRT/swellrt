package org.swellrt.beta.client.js;


import jsinterop.annotations.JsConstructor;
import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "WebSocket")
public class WebSocket {
  
  public final static int STATE_CONNECTING = 0;
  public final static int STATE_OPEN = 1;
  public final static int STATE_CLOSING = 2;
  public final static int STATE_CLOSED = 3;
  
  @JsFunction
  public interface Function<T> {
    void exec(T o);
  }
  
  @JsConstructor
  public WebSocket(String server) {
  }  
  
  public Function<Void> onopen;
  
  public Function<String> onmessage;
  
  public Function<Void> onclose;
  
  public Function<String> onerror;
  
  public int readyState; // read-only
  
  public native void close();
  
  public native void send(String data);
  
}
