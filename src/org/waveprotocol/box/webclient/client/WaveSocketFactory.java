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

package org.waveprotocol.box.webclient.client;

import com.google.gwt.websockets.client.WebSocket;
import com.google.gwt.websockets.client.WebSocketCallback;

import com.glines.socketio.client.common.SocketIOConnection;
import com.glines.socketio.client.common.SocketIOConnectionListener;
import com.glines.socketio.client.gwt.GWTSocketIOConnectionFactory;
import com.glines.socketio.common.DisconnectReason;
import com.glines.socketio.common.SocketIOException;

/**
 * Factory to create proxy wrappers around either {@link com.google.gwt.websockets.client.WebSocket}
 * or {@link com.glines.socketio.client.SocketIOConnection}.
 * 
 * @author tad.glines@gmail.com (Tad Glines)
 */
public class WaveSocketFactory {
  
  /**
   * Create a WaveSocket instance that wraps a concrete socket implementation.
   * If useSocketIO is true an instance of {@link com.glines.socketio.client.SocketIOConnection}
   * is wrapped, otherwise an instance of {@link com.google.gwt.websockets.client.WebSocket} is
   * wrapped.
   */
  public static WaveSocket create(boolean useSocketIO, final String urlBase,
      final WaveSocket.WaveSocketCallback callback) {
    if (useSocketIO) {
      return new WaveSocket() {
        /*
         *  TODO: The urlBase is ignored for now. The default is identical to what is currently
         *  provided in urlBase. When the GWTSocketIOConnectionFactory.create() is updated,
         *  parse the urlBAse and pass the host and port.
         */
        private final SocketIOConnection socket = GWTSocketIOConnectionFactory.INSTANCE.create(
            new SocketIOConnectionListener() {
          @Override
          public void onConnect() {
            callback.onConnect();
          }

          @Override
          public void onDisconnect(DisconnectReason reason, String errorMEssage) {
            callback.onDisconnect();
          }

          @Override
          public void onMessage(int messageType, String message) {
            callback.onMessage(message);
          }
        }, null, (short)0);

        @Override
        public void connect() {
          socket.connect();
        }

        @Override
        public void disconnect() {
          socket.disconnect();
        }

        @Override
        public void sendMessage(String message) {
          try {
            socket.sendMessage(message);
          } catch (SocketIOException e) {
            // Ignore because the GWT implementation doesn't throw this Exception.
          }
        }
        
      };
    } else {
      return new WaveSocket() {
        final WebSocket socket = new WebSocket(new WebSocketCallback() {
          @Override
          public void onConnect() {
            callback.onConnect();
          }

          @Override
          public void onDisconnect() {
            callback.onDisconnect();
          }

          @Override
          public void onMessage(String message) {
            callback.onMessage(message);
          }
        });

        @Override
        public void connect() {
          socket.connect(urlBase + "socket");
        }

        @Override
        public void disconnect() {
          socket.close();
        }

        @Override
        public void sendMessage(String message) {
          socket.send(message);
        }
      };
    }
  }
}
