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

package org.swellrt.beta.client.wave;

import java.util.Queue;

import org.swellrt.beta.client.wave.ProtocolMessageUtils.MessageWrapper;
import org.swellrt.beta.client.wave.ProtocolMessageUtils.ParseException;
import org.waveprotocol.box.common.comms.ProtocolAuthenticate;
import org.waveprotocol.box.common.comms.ProtocolOpenRequest;
import org.waveprotocol.box.common.comms.ProtocolSubmitRequest;
import org.waveprotocol.box.common.comms.impl.ProtocolAuthenticateImpl;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IntMap;

import com.google.common.base.Preconditions;


/**
 * Handle raw Wave protocol's messages from/to underlying transport protocol (WebSockets)
 * <p>
 * This class will reconnect to the server transparently creating new websockets if it is necessary.
 * The server will identify the connection despite of active websocket using the connection token (see constructor).
 * <p>
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WaveWebSocketClient implements WaveSocket.WaveSocketCallback {
  private static final Log LOG = Log.get(WaveWebSocketClient.class);

  public static interface StatusListener {
    public void onStateChange(ConnectState state, String error);
  }

  public static interface StartCallback {

    public void onStart();

    public void onFailure(String e);

  }



  private final Scheduler.Task reconnectTask = new Scheduler.Task() {
    @Override
    public void execute() {
      if (reconnectionDisabled)
        return;

      connect();
    }
  };


  private WaveSocket socket;
  private final IntMap<SubmitResponseCallback> submitRequestCallbacks;

  /**
   * Lifecycle of a socket is: (CONNECTING &#8594; CONNECTED &#8594; (TURBULENCE |
   * DISCONNECTED))&#8727; &#8594; ERROR;
   * <p><br>
   * The WaveSocket tries to keep the connection alive continuously. But under
   * some circumstances severe errors happen like server reboot or session
   * expiration.
   * <p><br>
   * A turbulence happens when server ACK's for heart beat messages are not received
   * before timeout. This means a slow network connection, server down, a ghost connection.
   * Consider here which is the right response to this situation.
   *
   */
  public enum ConnectState {
    CONNECTED, CONNECTING, DISCONNECTED, ERROR, TURBULENCE;

    @Override
    public String toString() {

      if (this == CONNECTED) return "CONNECTED";
      if (this == CONNECTING) return "CONNECTING";
      if (this == DISCONNECTED) return "DISCONNECTED";
      if (this == ERROR) return "ERROR";
      if (this == TURBULENCE) return "TURBULENCE";

      return "UNKOWN";
    }

  }

  private ConnectState connectState = null;
  private WaveWebSocketCallback callback;
  private StatusListener statusListener;
  private int sequenceNo = 0;

  private final Queue<MessageWrapper> messages = CollectionUtils.createQueue();

  private boolean connectedAtLeastOnce = false;
  /** Workaround, I can't make scheduler.cancel() work! */
  private boolean reconnectionDisabled = false;

  private final String sessionToken;
  private final String serverUrl;

  private StartCallback startCallback;

  private final ProtocolMessageUtils messageUtils = WaveDeps.protocolMessageUtils;

  /**
   * Create a new connection handler.
   *
   * @param connectionToken random token to identify same client connection despite which websocket is active
   * @param sessionToken HTTP session token
   * @param serverUrl server URL ws://, wss://
   */
  public WaveWebSocketClient(String sessionToken, String serverUrl) {
    this.sessionToken = sessionToken;
    this.serverUrl = serverUrl;
    submitRequestCallbacks = CollectionUtils.createIntMap();
  }

  /**
   * Attaches the handler for incoming messages. Once the client's workflow has
   * been fixed, this callback attachment will become part of
   * {@link #connect()}.
   */
  public void attachHandler(WaveWebSocketCallback callback) {
    Preconditions.checkArgument(callback != null);
    this.callback = callback;
  }


  public void attachStatusListener(StatusListener listener) {
    Preconditions.checkArgument(listener != null);
    this.statusListener = listener;
  }

  /**
   * Opens this connection first time.
   */
  public void start(StartCallback startCallback) {
    try {
      this.startCallback = startCallback;
      setState(ConnectState.CONNECTING);
      connectedAtLeastOnce = false;
      sequenceNo = 0;
      socket = new WaveSocketWS(serverUrl, sessionToken, this);
      socket.connect();
    } catch (Exception e) {
      // Report exception if it's first attempt to connect the websocket
      LOG.severe("start() exception ", e);
      startCallback.onFailure(e.getMessage());
      startCallback = null;
      setState(ConnectState.ERROR, e.getMessage());
    }
  }

  /**
   * Connect again an already started websocket
   */
  protected void connect() {
    try {
      setState(ConnectState.CONNECTING);
      socket.connect();
    } catch (Exception e) {
      // On reconnection attempts allow errors
      LOG.severe("onConnect() exception: ", e);
      onDisconnect();
    }
  }

  /**
   * Lets app to fully restart the connection.
   *
   */
  public void stop(boolean discardInFlightMessages) {
    setState(ConnectState.DISCONNECTED);
    socket.disconnect();
    sequenceNo = 0;
    connectedAtLeastOnce = false;
    reconnectionDisabled = true;
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
        String token = sessionToken;
        if (token != null) {
          ProtocolAuthenticate auth = new ProtocolAuthenticateImpl();
          auth.setToken(token);
          send(messageUtils.wrap(sequenceNo++, auth));
        }
      }

      connectedAtLeastOnce = true;
      // Flush queued messages.
      while (!messages.isEmpty() && connectState == ConnectState.CONNECTED) {
        send(messages.poll());
      }
    } catch (Exception e) {
      LOG.severe("onConnect() exception: ", e);
      setState(ConnectState.ERROR);
      return;
    }
  }

  @Override
  public void onDisconnect() {
    setState(ConnectState.DISCONNECTED);
    WaveDeps.lowPriorityTimer.scheduleDelayed(reconnectTask, 5000);
  }


  @Override
  public void onMessage(final String message) {


    LOG.debug("received JSON message " + message);
    Timer timer = Timing.start("deserialize message");
    MessageWrapper wrapper;
    try {
      wrapper = messageUtils.parseWrapper(message);
    } catch (ParseException e) {
      LOG.severe("invalid JSON message " + message, e);
      return;
    } finally {
      Timing.stop(timer);
    }

    if (wrapper.isProtocolWaveletUpdate()) {
      if (callback != null) {

        try {
          callback.onWaveletUpdate(messageUtils.unwrapWaveletUpdate(wrapper));
        } catch (Exception e) {
          // TODO consider if we should drop the connection or just invalidate the broken wave
          LOG.severe("onMessage() exception: ", e);
          setState(ConnectState.ERROR, e.getMessage());
        }

      }
    } else if (wrapper.isProtocolSubmitResponse()) {
      int seqno = wrapper.getSequenceNumber();
      SubmitResponseCallback callback = submitRequestCallbacks.get(seqno);

      if (callback != null) {

        try {
          submitRequestCallbacks.remove(seqno);
          callback.run(messageUtils.unwrapSubmitResponse(wrapper));
        } catch (Exception e) {
          // TODO consider if we should drop the connection or just invalidate the broken wave
          LOG.severe("onMessage() exception ", e);
          setState(ConnectState.ERROR, e.getMessage());
        }


      } else {
        LOG.debug("Submit response received (" + seqno + ") but not callback found of "
            + submitRequestCallbacks.countEntries() + " entries");
      }
    } else if (wrapper.isRpcFinished()) {
      ProtocolMessageUtils.RpcFinished m = messageUtils.unwrapRpcFinished(wrapper);
        if (callback != null) {
        callback.onFinished(m);
        }
      if (m.hasFailed()) {
        setState(ConnectState.ERROR,
            "A server error has closed the RPC connection: "
                + (m.hasErrorText() ? m.getErrorText() : ""));
      }
    } else if (wrapper.isProtocolAuthenticationResult()) {
      if (startCallback != null) {
        startCallback.onStart();
        startCallback = null;
      }
    }
  }

  public void submit(ProtocolSubmitRequest message, SubmitResponseCallback callback) {
    int submitId = sequenceNo++;
    submitRequestCallbacks.put(submitId, callback);
    send(messageUtils.wrap(submitId, message));
  }

  public void open(ProtocolOpenRequest message) {
    send(messageUtils.wrap(sequenceNo++, message));
  }

  private void send(MessageWrapper message) {
    switch (connectState) {
      case CONNECTED:
        Timer timing = Timing.start("serialize message");
        String json;
        try {
          json = messageUtils.toJson(message);
        } finally {
          Timing.stop(timing);
        }
      LOG.debug("Sending JSON data " + json);
        socket.sendMessage(json);
        break;
      default:
        messages.add(message);
    }
  }

  public boolean isConnected() {
    return connectState == ConnectState.CONNECTED;
  }

  public ConnectState getState() {
    return this.connectState;
  }

  @Override
  public void onError(String reason) {
    if (connectedAtLeastOnce)
      WaveDeps.lowPriorityTimer.cancel(reconnectTask);

    if (startCallback != null) {
      startCallback.onFailure(reason);
      startCallback = null;
    }

    reconnectionDisabled = true;
    setState(ConnectState.ERROR, reason);
  }

  @Override
  public void onTurbulence(boolean finished) {
    if (!finished) {
      // this will cache messages in pending queue
      setState(ConnectState.DISCONNECTED);
      // Don't start here a reconnection process, just wait.
    } else {
      onConnect(); // flush pending messages
    }
  }

  protected void setState(ConnectState state) {
    setState(state, null);
  }

  protected void setState(ConnectState state, String error) {
    connectState = state;
    if (statusListener != null) statusListener.onStateChange(state, error);
  }

}
