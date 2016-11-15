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

package org.swellrt.beta.wave.transport;

import static org.waveprotocol.wave.communication.gwt.JsonHelper.getPropertyAsInteger;
import static org.waveprotocol.wave.communication.gwt.JsonHelper.getPropertyAsObject;
import static org.waveprotocol.wave.communication.gwt.JsonHelper.getPropertyAsString;
import static org.waveprotocol.wave.communication.gwt.JsonHelper.setPropertyAsInteger;
import static org.waveprotocol.wave.communication.gwt.JsonHelper.setPropertyAsObject;
import static org.waveprotocol.wave.communication.gwt.JsonHelper.setPropertyAsString;

import com.google.common.base.Preconditions;

import org.waveprotocol.box.common.comms.jso.ProtocolAuthenticateJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolOpenRequestJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolSubmitRequestJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolSubmitResponseJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolWaveletUpdateJsoImpl;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.wave.client.events.Log;
import org.waveprotocol.wave.communication.gwt.JsonMessage;
import org.waveprotocol.wave.communication.json.JsonException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IntMap;

import java.util.Queue;


/**
 * Wrapper around Atmosphere connections that handles the Wave client-server
 * protocol.
 *
 * Catch exceptions on handling server messages and provide them to client as
 * events.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WaveWebSocketClient implements WaveSocket.WaveSocketCallback {
  private static final Log LOG = Log.get(WaveWebSocketClient.class);

  public static interface StatusListener {

    public void onStateChange(String state, Throwable e);

  }

  /**
   * Envelope for delivering arbitrary messages. Each envelope has a sequence
   * number and a message. The format must match the format used in the server's
   * WebSocketChannel.
   * <p>
   * Note that this message can not be described by a protobuf, because it
   * contains an arbitrary protobuf, which breaks the protobuf typing rules.
   */
  private static final class MessageWrapper extends JsonMessage {
    static MessageWrapper create(int seqno, String type, JsonMessage message) {
      MessageWrapper wrapper = JsonMessage.createJsonMessage().cast();
      setPropertyAsInteger(wrapper, "sequenceNumber", seqno);
      setPropertyAsString(wrapper, "messageType", type);
      setPropertyAsObject(wrapper, "message", message);
      return wrapper;
    }

    @SuppressWarnings("unused") // GWT requires an explicit protected ctor
    protected MessageWrapper() {
      super();
    }

    int getSequenceNumber() {
      return getPropertyAsInteger(this, "sequenceNumber");
    }

    String getType() {
      return getPropertyAsString(this, "messageType");
    }

    <T extends JsonMessage> T getPayload() {
      return getPropertyAsObject(this, "message").<T>cast();
    }
  }

  private WaveSocket socket;
  private final IntMap<SubmitResponseCallback> submitRequestCallbacks;

  /**
   * Lifecycle of a socket is: (CONNECTING &#8594; CONNECTED &#8594;
   * DISCONNECTED)&#8727; &#8594; ERROR;
   *
   * The WaveSocket tries to keep the connection alive continuously. But under
   * some circumstances severe errors happen like server reboot or session
   * expiration.
   *
   */
  public enum ConnectState {
    CONNECTED, CONNECTING, DISCONNECTED, ERROR;

    public String toString() {

      if (this == CONNECTED) return "CONNECTED";
      if (this == CONNECTING) return "CONNECTING";
      if (this == DISCONNECTED) return "DISCONNECTED";
      if (this == ERROR) return "ERROR";

      return "UNKOWN";
    }

  }

  private ConnectState connectState = ConnectState.DISCONNECTED;
  private WaveWebSocketCallback callback;
  private StatusListener statusListener;
  private int sequenceNo;

  private final Queue<JsonMessage> messages = CollectionUtils.createQueue();

  private boolean connectedAtLeastOnce = false;

  private WaveSocket.WaveSocketStartCallback onStartCallback = null;
  
  private String sessionId = null;


  public WaveWebSocketClient(String urlBase, String clientVersion) {
    submitRequestCallbacks = CollectionUtils.createIntMap();
    socket = WaveSocketFactory.create(urlBase, clientVersion, this);
  }

  /**
   * Attaches the handler for incoming messages. Once the client's workflow has
   * been fixed, this callback attachment will become part of
   * {@link #connect()}.
   */
  public void attachHandler(WaveWebSocketCallback callback) {
    // Preconditions.checkState(this.callback == null);
    Preconditions.checkArgument(callback != null);
    this.callback = callback;
  }


  public void attachStatusListener(StatusListener listener) {
    Preconditions.checkArgument(listener != null);
    this.statusListener = listener;
  }

  /**
   * Opens this connection.
   */
  public void connect(String sessionId) {
    setState(ConnectState.CONNECTING);
    socket.connect(sessionId);
  }

  /**
   * Opens this connection with a callback to know when actually websocket is
   * opened.
   */
  public void connect(String sessionId, WaveSocket.WaveSocketStartCallback callback) {
    this.sessionId = sessionId;
    onStartCallback = callback;
    setState(ConnectState.CONNECTING);
    socket.connect(sessionId);
  }

  /**
   * Lets app to fully restart the connection.
   *
   */
  public void disconnect(boolean discardInFlightMessages) {
    setState(ConnectState.DISCONNECTED);
    socket.disconnect();
    connectedAtLeastOnce = false;
    if (discardInFlightMessages) messages.clear();

  }


  @Override
  public void onConnect() {

    setState(ConnectState.CONNECTED);

    try {
      // Sends the session cookie to the server via an RPC to work around
      // browser bugs.
      // See: http://code.google.com/p/wave-protocol/issues/detail?id=119
      if (!connectedAtLeastOnce) {
        String token = sessionId;
        if (token != null) {
          ProtocolAuthenticateJsoImpl auth = ProtocolAuthenticateJsoImpl.create();
          auth.setToken(token);
          send(MessageWrapper.create(sequenceNo++, "ProtocolAuthenticate", auth));
        }
      }
      connectedAtLeastOnce = true;
      // Flush queued messages.
      while (!messages.isEmpty() && connectState == ConnectState.CONNECTED) {
        send(messages.poll());
      }
    } catch (Exception e) {

      setState(ConnectState.ERROR);

      // Report connection error on connection started explicitly
      if (onStartCallback != null) {
        onStartCallback.onFailure();
        onStartCallback = null;
      }

      return;
    }

    // Report connection success on connection started explicitly
    if (onStartCallback != null) {
      onStartCallback.onSuccess();
      onStartCallback = null;
    }

  }

  @Override
  public void onDisconnect() {
    setState(ConnectState.DISCONNECTED);
  }


  @Override
  public void onMessage(final String message) {


    LOG.info("received JSON message " + message);
    Timer timer = Timing.start("deserialize message");
    MessageWrapper wrapper;
    try {
      wrapper = MessageWrapper.parse(message);
    } catch (JsonException e) {
      LOG.severe("invalid JSON message " + message, e);
      return;
    } finally {
      Timing.stop(timer);
    }
    String messageType = wrapper.getType();
    if ("ProtocolWaveletUpdate".equals(messageType)) {
      if (callback != null) {

        try {
          callback.onWaveletUpdate(wrapper.<ProtocolWaveletUpdateJsoImpl> getPayload());
        } catch (Exception e) {
          // TODO consider if we should drop the connection or just invalidate the broken wave
          setState(ConnectState.ERROR, e);
        }

      }
    } else if ("ProtocolSubmitResponse".equals(messageType)) {
      int seqno = wrapper.getSequenceNumber();
      SubmitResponseCallback callback = submitRequestCallbacks.get(seqno);
      if (callback != null) {

        try {
          submitRequestCallbacks.remove(seqno);
          callback.run(wrapper.<ProtocolSubmitResponseJsoImpl> getPayload());
        } catch (Exception e) {
          // TODO consider if we should drop the connection or just invalidate the broken wave
          setState(ConnectState.ERROR, e);
        }

      }
    }
  }

  public void submit(ProtocolSubmitRequestJsoImpl message, SubmitResponseCallback callback) {
    int submitId = sequenceNo++;
    submitRequestCallbacks.put(submitId, callback);
    send(MessageWrapper.create(submitId, "ProtocolSubmitRequest", message));
  }

  public void open(ProtocolOpenRequestJsoImpl message) {
    send(MessageWrapper.create(sequenceNo++, "ProtocolOpenRequest", message));
  }

  private void send(JsonMessage message) {
    switch (connectState) {
      case CONNECTED:
        Timer timing = Timing.start("serialize message");
        String json;
        try {
          json = message.toJson();
        } finally {
          Timing.stop(timing);
        }
        LOG.info("Sending JSON data " + json);
        socket.sendMessage(json);
        break;
      default:
        messages.add(message);
    }
  }

  public boolean isConnected() {
    return connectState == ConnectState.CONNECTED;
  }

  @Override
  public void onError(String errorCode) {
    // For now, we can't recover exceptions from inner layers. Just let the
    // client app to reset
    setState(ConnectState.ERROR);
  }

  protected void setState(ConnectState state) {
    setState(state, null);
  }

  protected void setState(ConnectState state, Throwable e) {
    connectState = state;
    if (statusListener != null) statusListener.onStateChange(state.toString(), e);
  }

}
