package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.platform.web.browser.BrowserWebSocket;
import org.swellrt.beta.client.wave.ws.CloseEvent;
import org.swellrt.beta.client.wave.ws.Event;
import org.swellrt.beta.client.wave.ws.MessageEvent;
import org.swellrt.beta.client.wave.ws.WebSocket;

public class WebWebSocket implements WebSocket {

  private BrowserWebSocket ws;

  @Override
  public void connect(String server) {
    ws = new BrowserWebSocket(server);
  }

  @Override
  public void send(String data) {
    ws.send(data);
  }

  @Override
  public void close() {
    ws.close();
  }

  @Override
  public void onOpen(Function<Event> func) {
    ws.onopen = func;
  }

  @Override
  public void onMessage(Function<MessageEvent> func) {
    ws.onmessage = func;
  }

  @Override
  public void onClose(Function<CloseEvent> func) {
    ws.onclose = func;
  }

  @Override
  public void onError(Function<Event> func) {
    ws.onerror = func;
  }

  @Override
  public int getReadyState() {
    return ws.readyState;
  }

}
