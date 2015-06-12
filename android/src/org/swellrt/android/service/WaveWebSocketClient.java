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

package org.swellrt.android.service;

import java.util.Queue;

import org.waveprotocol.box.common.comms.gson.ProtocolAuthenticateGsonImpl;
import org.waveprotocol.box.common.comms.gson.ProtocolOpenRequestGsonImpl;
import org.waveprotocol.box.common.comms.gson.ProtocolSubmitRequestGsonImpl;
import org.waveprotocol.box.common.comms.gson.ProtocolSubmitResponseGsonImpl;
import org.waveprotocol.box.common.comms.gson.ProtocolWaveletUpdateGsonImpl;
import org.waveprotocol.wave.communication.gson.GsonException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IntMap;

import android.util.Log;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;


/**
 * Wrapper around WebSocket that handles the Wave client-server protocol.
 */
public class WaveWebSocketClient implements WaveSocket.WaveSocketCallback {

  private static final String TAG = "WaveWebSocketClient";

  public interface ConnectionListener {

    public void onConnect();

    public void onReconnect();

    public void onDisconnect();

  }

  private static final int MAX_INITIAL_FAILURES = 2;

  private static final int RECONNECT_TIME_MS = 5000;

  // Sets an specific session cookie name
  // private static final String JETTY_SESSION_TOKEN_NAME = "JSESSIONID";
  private static final String JETTY_SESSION_TOKEN_NAME = "WSESSIONID";

  /**
   * Envelope for delivering arbitrary messages. Each envelope has a sequence
   * number and a message. The format must match the format used in the server's
   * WebSocketChannel.
   * <p>
   * Note that this message can not be described by a protobuf, because it
   * contains an arbitrary protobuf, which breaks the protobuf typing rules.
   *
   * Pure-java version copied from server's WebSocketChannel
   */
  private static class MessageWrapper {
    private final static JsonParser parser = new JsonParser();

    final int sequenceNumber;
    final String messageType;
    final JsonElement message;

    public MessageWrapper(int sequenceNumber, String messageType, JsonElement message) {
      this.sequenceNumber = sequenceNumber;
      this.messageType = messageType;
      this.message = message;
    }

    public static MessageWrapper deserialize(Gson gson, String data) {
      JsonElement e = parser.parse(data);
      JsonObject obj = e.getAsJsonObject();
      String type = obj.get("messageType").getAsString();
      int seqno = obj.get("sequenceNumber").getAsInt();
      JsonElement message = obj.get("message");
      return new MessageWrapper(seqno, type, message);
    }

    public static String serialize(String type, int seqno, JsonElement message) {
      JsonObject o = new JsonObject();
      o.add("messageType", new JsonPrimitive(type));
      o.add("sequenceNumber", new JsonPrimitive(seqno));
      o.add("message", message);
      return o.toString();
    }
  }

  private WaveSocket socket = null;
  private final IntMap<SubmitResponseCallback> submitRequestCallbacks;

  /**
   * Lifecycle of a socket is:
   *   (CONNECTING &#8594; CONNECTED &#8594; DISCONNECTED)&#8727;
   */
  private enum ConnectState {
    CONNECTED, CONNECTING, DISCONNECTED
  }

  private ConnectState connected = ConnectState.DISCONNECTED;
  private WaveWebSocketCallback callback;
  private int sequenceNo;

  private final Queue<String> messages = CollectionUtils.createQueue();


  private boolean connectedAtLeastOnce = false;
  private long connectTry = 0;
  private final String urlBase;
  private final String httpSessionId;

  private final Gson gson = new Gson();

  private ConnectionListener connectionListener = null;

  public WaveWebSocketClient(String urlBase, String httpSessionId) {
    this.httpSessionId = httpSessionId;
    this.urlBase = urlBase;

    submitRequestCallbacks = CollectionUtils.createIntMap();
    socket = WaveSocketFactory.create(false, urlBase, httpSessionId, this);
  }

  /**
   * Attaches the handler for incoming messages. Once the client's workflow has
   * been fixed, this callback attachment will become part of
   * {@link #connect()}.
   */
  public void attachHandler(WaveWebSocketCallback callback) {
    Preconditions.checkState(this.callback == null);
    Preconditions.checkArgument(callback != null);
    this.callback = callback;
  }

  /**
   * Opens this connection.
   */
  public void connect(ConnectionListener listener) {

    connectionListener = listener;

    if (socket == null) {
      socket = WaveSocketFactory.create(true, urlBase, httpSessionId, WaveWebSocketClient.this);
    }

    connectTry++;
    if (connected == ConnectState.DISCONNECTED) {
      Log.i(TAG, "Attemping to reconnect");
      connected = ConnectState.CONNECTING;
      socket.connect();
    }

    // reconnectCommand.execute();
    // Scheduler.get().scheduleFixedDelay(reconnectCommand, RECONNECT_TIME_MS);
  }

  @Override
  public void onConnect() {

    connected = ConnectState.CONNECTED;

    // Sends the session cookie to the server via an RPC to work around browser bugs.
    // See: http://code.google.com/p/wave-protocol/issues/detail?id=119

    if (httpSessionId != null && !connectedAtLeastOnce) {
      ProtocolAuthenticateGsonImpl auth = new ProtocolAuthenticateGsonImpl();
      auth.setToken(httpSessionId);
      sendMessage(sequenceNo++, "ProtocolAuthenticate", auth.toGson(null, null));
    }

    // Flush queued messages.
    while (!messages.isEmpty() && connected == ConnectState.CONNECTED) {
      send(messages.poll());
    }

    if (connectionListener != null)
      if (!connectedAtLeastOnce)
        connectionListener.onConnect();
      else
        connectionListener.onReconnect();

    connectedAtLeastOnce = true;

  }

  @Override
  public void onDisconnect() {
    connected = ConnectState.DISCONNECTED;

    if (connectionListener != null)
      connectionListener.onDisconnect();
  }

  @Override
  public void onMessage(final String message) {
    Log.i(TAG, "Received JSON message " + message);
    MessageWrapper wrapper;
    wrapper = MessageWrapper.deserialize(gson, message);

    String messageType = wrapper.messageType;
    if ("ProtocolWaveletUpdate".equals(messageType)) {
      if (callback != null) {
        ProtocolWaveletUpdateGsonImpl waveletUpdate = new ProtocolWaveletUpdateGsonImpl();

        try {
          waveletUpdate.fromGson(wrapper.message, gson, null);
        } catch (GsonException e) {
          Log.i(TAG, "Error parsing WaveletUpdate JSON message", e);
          return;
        }
        callback.onWaveletUpdate(waveletUpdate);
      }
    } else if ("ProtocolSubmitResponse".equals(messageType)) {
      int seqno = wrapper.sequenceNumber;
      SubmitResponseCallback callback = submitRequestCallbacks.get(seqno);
      if (callback != null) {
        submitRequestCallbacks.remove(seqno);
        ProtocolSubmitResponseGsonImpl submitResponse = new ProtocolSubmitResponseGsonImpl();
        try {
          submitResponse.fromGson(wrapper.message, gson, null);
        } catch (GsonException e) {
          Log.e(TAG, "Error parsing SubmitResponse JSON message", e);
          return;
        }
        callback.run(submitResponse);
      }
    }
  }

  public void submit(ProtocolSubmitRequestGsonImpl message, SubmitResponseCallback callback) {
    int submitId = sequenceNo++;
    submitRequestCallbacks.put(submitId, callback);
    sendMessage(submitId, "ProtocolSubmitRequest", message.toGson(null, null));
  }

  public void open(ProtocolOpenRequestGsonImpl message) {
    sendMessage(sequenceNo++, "ProtocolOpenRequest", message.toGson(null, null));
  }


  private void sendMessage(int sequenceNo, String type, JsonElement message) {

    String json = "";
    try {
      json = MessageWrapper.serialize(type, sequenceNo, message);
    } catch (Exception e) {
      Log.e(TAG, "Error serializing message ", e);
    } finally {

    }
    switch (connected) {
    case CONNECTED:
      send(json);
      break;
    default:
      messages.add(json);
    }
  }

  private void send(String json) {
    Log.i(TAG, "Sending JSON data " + json);
    socket.sendMessage(json);
  }

}
