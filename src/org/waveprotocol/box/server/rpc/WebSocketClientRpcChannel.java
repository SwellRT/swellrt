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

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.RpcController;
import com.google.protobuf.UnknownFieldSet;

import com.sixfire.websocket.WebSocket;

import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of {@link ClientRpcChannel} based on a
 * {@link WebSocketClientChannel}.
 */
public class WebSocketClientRpcChannel implements ClientRpcChannel {
  private static final Log LOG = Log.get(WebSocketClientRpcChannel.class);

  private final MessageExpectingChannel protoChannel;
  private final AtomicInteger lastSequenceNumber = new AtomicInteger();
  private final BiMap<Integer, ClientRpcController> activeMethodMap = HashBiMap.create();

  /**
   * Set up a new WebSocketClientRpcChannel pointing at the given server
   * address.
   *
   * @param serverAddress the target server address
   * @param threadPool threadpool for performing async reads.
   */
  public WebSocketClientRpcChannel(SocketAddress serverAddress, ExecutorService threadPool)
      throws IOException {
    Preconditions.checkNotNull(serverAddress, "null serverAddress");

    ProtoCallback callback = new ProtoCallback() {
      @Override
      public void message(int sequenceNo, Message message) {
        final ClientRpcController controller;
        synchronized (activeMethodMap) {
          controller = activeMethodMap.get(sequenceNo);
          // TODO: remove controller from activeMethodMap
        }
        if (message instanceof Rpc.RpcFinished) {
          Rpc.RpcFinished finished = (Rpc.RpcFinished) message;
          if (finished.getFailed()) {
            controller.failure(finished.getErrorText());
          } else {
            controller.response(null);
          }
        } else {
          controller.response(message);
        }
      }

      private void unknown(long sequenceNo, String messageType) {
        final ClientRpcController controller;
        synchronized (activeMethodMap) {
          controller = activeMethodMap.get(sequenceNo);
        }
        controller.failure("Client RPC got unknown message: " + messageType);
      }

      @Override
      public void unknown(int sequenceNo, String messageType, UnknownFieldSet message) {
        unknown(sequenceNo, messageType);
      }

      @Override
      public void unknown(int sequenceNo, String messageType, String message) {
        unknown(sequenceNo, messageType);
      }
    };

    WebSocket websocket = openWebSocket((InetSocketAddress) serverAddress);
    protoChannel = new WebSocketClientChannel(websocket, callback, threadPool);
    protoChannel.expectMessage(Rpc.RpcFinished.getDefaultInstance());
    protoChannel.startAsyncRead();
    LOG.fine("Opened a new WebSocketClientRpcChannel to " + serverAddress);
  }

  /**
   * Create a new WebSocketClientRpcChannel backed onto a new single thread
   * executor.
   */
  public WebSocketClientRpcChannel(SocketAddress serverAddress) throws IOException {
    this(serverAddress, Executors.newSingleThreadExecutor());
  }

  @Override
  public RpcController newRpcController() {
    return new ClientRpcController(this);
  }

  @Override
  public void callMethod(MethodDescriptor method, RpcController genericRpcController,
      Message request, Message responsePrototype, RpcCallback<Message> callback) {
    // Cast the given generic controller to a ClientRpcController.
    final ClientRpcController controller;
    if (genericRpcController instanceof ClientRpcController) {
      controller = (ClientRpcController) genericRpcController;
    } else {
      throw new IllegalArgumentException("Expected ClientRpcController, got: "
          + genericRpcController.getClass());
    }

    // Generate a new sequence number, and configure the controller - notably,
    // this throws an IllegalStateException if it is *already* configured.
    final int sequenceNo = lastSequenceNumber.incrementAndGet();
    final ClientRpcController.RpcState rpcStatus =
        new ClientRpcController.RpcState(this, method.getOptions()
            .getExtension(Rpc.isStreamingRpc), callback, new Runnable() {
          @Override
          public void run() {
            protoChannel.sendMessage(sequenceNo, Rpc.CancelRpc.getDefaultInstance());
          }
        });
    controller.configure(rpcStatus);
    synchronized (activeMethodMap) {
      activeMethodMap.put(sequenceNo, controller);
    }
    LOG.fine("Calling a new RPC (seq " + sequenceNo + "), method " + method.getFullName() + " for "
        + protoChannel);

    // Kick off the RPC by sending the request to the server end-point.
    protoChannel.sendMessage(sequenceNo, request, responsePrototype);
  }

  private WebSocket openWebSocket(InetSocketAddress inetAddress) throws IOException {
    URI uri;
    try {
      uri = new URI("ws", null, inetAddress.getHostName(), inetAddress.getPort(), "/socket",
          null, null);
    } catch (URISyntaxException e) {
      LOG.severe("Unable to create ws:// uri from given address (" + inetAddress + ")", e);
      throw new IllegalStateException(e);
    }
    WebSocket websocket = new WebSocket(uri);
    websocket.connect();
    return websocket;
  }
}
