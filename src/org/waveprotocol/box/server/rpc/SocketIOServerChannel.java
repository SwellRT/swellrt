/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.box.server.rpc;

import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;
import com.glines.socketio.server.SocketIOFrame;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOOutbound;

import org.waveprotocol.wave.util.logging.Log;

/**
 * The server side of WebSocketChannel.
 */
public class SocketIOServerChannel extends WebSocketChannel implements SocketIOInbound {
  private static final Log LOG = Log.get(SocketIOServerChannel.class);

  private SocketIOOutbound outbound;

  /**
   * Creates a new WebSocketServerChannel using the callback for incoming messages.
   *
   * @param callback A ProtoCallback instance called with incoming messages.
   */
  public SocketIOServerChannel(ProtoCallback callback) {
    super(callback);
  }

  /**
   * Handles an incoming connection
   *
   * @param outbound The outbound direction of the new connection.
   */
  @Override
  public void onConnect(SocketIOOutbound outbound) {
    this.outbound = outbound;
  }

  /**
   * Pass on an incoming String message.
   *
   * @param data The message data itself
   */
  @Override
  public void onMessage(int messageType, String data) {
    if (SocketIOFrame.TEXT_MESSAGE_TYPE == messageType) {
      handleMessageString(data);
    } else {
      LOG.warning("Recieved message of unexpected type: " + messageType);
    }
  }

  /**
   * Handle a client disconnect.
   */
  @Override
  public void onDisconnect(DisconnectReason reason, String errorMessage) {
    if (errorMessage == null) {
      LOG.info("websocket disconnected["+reason+"]");
    } else {
      LOG.info("websocket disconnected["+reason+"]: "+errorMessage);
    }
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
  protected void sendMessageString(String data) throws SocketIOException {
    synchronized (this) {
      if (outbound == null) {
        // Just drop the message. It's rude to throw an exception since the
        // caller had no way of knowing.
        LOG.warning("Websocket is not connected");
      } else {
        outbound.sendMessage(data);
      }
    }
  }
}
