/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.waveprotocol.box.server.rpc;

import org.eclipse.jetty.websocket.WebSocket;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;

/**
 * The server side of WebSocketChannel.
 */
public class WebSocketServerChannel extends WebSocketChannel implements WebSocket,
    WebSocket.OnTextMessage {
  private static final Log LOG = Log.get(WebSocketServerChannel.class);

  private Connection connection;

  /**
   * Creates a new WebSocketServerChannel using the callback for incoming messages.
   *
   * @param callback A ProtoCallback instance called with incoming messages.
   */
  public WebSocketServerChannel(ProtoCallback callback) {
    super(callback);
  }

  /**
   * Called when a new websocket connection is accepted.
   *
   * @param connection The Connection object to use to send messages.
   */
  @Override
  public void onOpen(Connection connection) {
    this.connection = connection;
    connection.setMaxIdleTime(0);
  }

  /**
   * Pass on an incoming String message.
   *
   * @param data The message data itself
   */
  @Override
  public void onMessage(String data) {
    handleMessageString(data);
  }

  /**
   * Called when an established websocket connection closes
   * @param closeCode
   * @param message
   */
  @Override
  public void onClose(int closeCode, String message) {
    LOG.info("websocket disconnected (" + closeCode + " - " + message + "): " + this);
    synchronized (this) {
      connection = null;
    }
  }

  /**
   * Send the given data String
   *
   * @param data
   */
  @Override
  protected void sendMessageString(String data) throws IOException {
    synchronized (this) {
      if (connection == null) {
        // Just drop the message. It's rude to throw an exception since the
        // caller had no way of knowing.
        LOG.warning("Websocket is not connected");
      } else {
        connection.sendMessage(data);
      }
    }
  }
}
