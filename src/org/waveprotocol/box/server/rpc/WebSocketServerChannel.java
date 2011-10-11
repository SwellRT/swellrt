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
public class WebSocketServerChannel extends WebSocketChannel implements WebSocket {
  private static final Log LOG = Log.get(WebSocketServerChannel.class);

  private Outbound outbound;

  /**
   * Creates a new WebSocketServerChannel using the callback for incoming messages.
   *
   * @param callback A ProtoCallback instance called with incoming messages.
   */
  public WebSocketServerChannel(ProtoCallback callback) {
    super(callback);
  }

  /**
   * Handles an incoming connection
   *
   * @param outbound The outbound direction of the new connection.
   */
  @Override
  public void onConnect(Outbound outbound) {
    this.outbound = outbound;
  }

  /**
   * Does nothing, as this only understands String messages, not byte ones.
   */
  @Override
  public void onMessage(byte frame, byte[] data, int offset, int length) {
    // do nothing. we don't expect this type of message.
  }

  /**
   * Pass on an incoming String message.
   *
   * @param frame Which framing byte was used
   * @param data The message data itself
   */
  @Override
  public void onMessage(byte frame, String data) {
    handleMessageString(data);
  }

  /**
   * Handle a client disconnect.
   */
  @Override
  public void onDisconnect() {
    LOG.info("websocket disconnected: "+this);
    synchronized (this) {
      outbound = null;
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
      if (outbound == null) {
        // Just drop the message. It's rude to throw an exception since the
        // caller had no way of knowing.
        LOG.warning("Websocket is not connected");
      } else {
        // we always use null to frame our UTF-8 strings.
        outbound.sendMessage((byte) 0x00, data);
      }
    }
  }
}
