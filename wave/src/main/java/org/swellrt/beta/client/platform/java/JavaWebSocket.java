package org.swellrt.beta.client.platform.java;

import java.net.URI;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.swellrt.beta.client.wave.ws.CloseEvent;
import org.swellrt.beta.client.wave.ws.Event;
import org.swellrt.beta.client.wave.ws.MessageEvent;
import org.swellrt.beta.client.wave.ws.WebSocket;

public class JavaWebSocket implements WebSocket {

  WebSocketClient cc;

  Function<Event> onopen;
  Function<MessageEvent> onmessage;
  Function<CloseEvent> onclose;
  Function<Event> onerror;

  int readyState = WebSocket.CLOSED;

  @Override
  public void connect(String server) throws Exception {

    readyState = WebSocket.CONNECTING;

    cc = new WebSocketClient(new URI(server)) {

      @Override
      public void onOpen(ServerHandshake handshakedata) {
        readyState = WebSocket.OPEN;
        onopen.exec(new Event() {
        });
      }

      @Override
      public void onMessage(String message) {
        MessageEvent event = new MessageEvent() {
        };
        event.data = message;
        event.type = "message";
        onmessage.exec(event);
      }

      @Override
      public void onClose(int code, String reason, boolean remote) {
        readyState = WebSocket.CLOSED;
        CloseEvent event = new CloseEvent() {
        };
        event.code = code;
        event.reason = reason;
        event.type = "close";
        onclose.exec(event);
      }

      @Override
      public void onError(Exception ex) {
        Event event = new Event() {

        };
        event.type = "error";
        onerror.exec(event);
      }

    };

    cc.connect();
  }

  @Override
  public void send(String data) {
    cc.send(data);
  }

  @Override
  public void close() {
    readyState = WebSocket.CLOSING;
    cc.close();
  }

  @Override
  public void onOpen(Function<Event> func) {
    onopen = func;
  }

  @Override
  public void onMessage(Function<MessageEvent> func) {
    onmessage = func;
  }

  @Override
  public void onClose(Function<CloseEvent> func) {
    onclose = func;
  }

  @Override
  public void onError(Function<Event> func) {
    onerror = func;
  }

  @Override
  public int getReadyState() {
    return readyState;
  }

}
