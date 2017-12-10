package org.swellrt.beta.client.wave.ws;

import jsinterop.annotations.JsFunction;

public interface WebSocket {

  public static interface Factory {
    WebSocket create();
  }

  @JsFunction
  public interface Function<T> {
    void exec(T o);
  }

  public static final int CONNECTING = 0;

  public static final int OPEN = 1;

  public static final int CLOSING = 2;

  public static final int CLOSED = 3;

  void connect(String server) throws Exception;

  void send(String data);

  void close();

  void onOpen(Function<Event> func);

  void onMessage(Function<MessageEvent> func);

  void onClose(Function<CloseEvent> func);

  void onError(Function<Event> func);

  int getReadyState();
}
