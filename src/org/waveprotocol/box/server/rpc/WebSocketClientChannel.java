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

import com.sixfire.websocket.WebSocket;

import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

/**
 * The client side of a WebSocketChannel.
 */
class WebSocketClientChannel extends WebSocketChannel {
  private static final Log LOG = Log.get(WebSocketClientChannel.class);

  private final WebSocket websocket;
  private final ExecutorService threadPool;
  private final Runnable asyncRead = new Runnable() {
        @Override
        public void run() {
          try {
            String data;
            while ((data = websocket.recv()) != null) {
              handleMessageString(data);
            }
          } catch (IOException e) {
            LOG.severe("WebSocket async data read failed, aborting connection.", e);
          }
        }
      };

  private boolean isReading = false;

  /**
   * Constructs a WebSocketClientChannel.
   *
   * @param websocket connected websocket
   * @param callback ProtoCallback handler for incoming messages
   * @param threadPool threadpool for thread that performs async read.
   */
  public WebSocketClientChannel(WebSocket websocket, ProtoCallback callback,
      ExecutorService threadPool) {
    super(callback);
    this.websocket = websocket;
    this.threadPool = threadPool;
  }

  /**
   * Start reading asynchronously from the websocket client.
   */
  @Override
  public void startAsyncRead() {
    if (isReading) {
      throw new IllegalStateException("This websocket is already reading asynchronously.");
    }
    threadPool.execute(asyncRead);
    isReading = true;
  }

  /**
   * Propagate a String message to the websocket client.
   */
  @Override
  public void sendMessageString(String data) throws IOException {
    websocket.send(data);
  }
}
