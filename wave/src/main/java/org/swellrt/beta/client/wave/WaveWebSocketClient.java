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

import static org.waveprotocol.wave.communication.gwt.JsonHelper.getPropertyAsInteger;
import static org.waveprotocol.wave.communication.gwt.JsonHelper.getPropertyAsObject;
import static org.waveprotocol.wave.communication.gwt.JsonHelper.getPropertyAsString;
import static org.waveprotocol.wave.communication.gwt.JsonHelper.setPropertyAsInteger;
import static org.waveprotocol.wave.communication.gwt.JsonHelper.setPropertyAsObject;
import static org.waveprotocol.wave.communication.gwt.JsonHelper.setPropertyAsString;

import com.google.common.base.Preconditions;

import org.swellrt.beta.client.js.Console;
import org.swellrt.beta.client.wave.WaveWebSocketCallback.RpcFinished;
import org.waveprotocol.box.common.comms.jso.ProtocolAuthenticateJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolOpenRequestJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolSubmitRequestJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolSubmitResponseJsoImpl;
import org.waveprotocol.box.common.comms.jso.ProtocolWaveletUpdateJsoImpl;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.wave.client.events.Log;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.communication.gwt.JsonMessage;
import org.waveprotocol.wave.communication.json.JsonException;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IntMap;

import java.util.Queue;


/**
 * Handle raw Wave protocol's messages from/to underlying transport protocol (usually WebSockets)
 * <p>
 * Exposes connection status to the app. This status don't need to match WebSocket's status.  
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WaveWebSocketClient implements WaveSocket.WaveSocketCallback {
  private static final Log LOG = Log.get(WaveWebSocketClient.class);

  public static interface StatusListener {

    public void onStateChange(ConnectState state, String error);

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
  
  private final Scheduler.Task reconnectTask = new Scheduler.Task() {
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

  private final Queue<JsonMessage> messages = CollectionUtils.createQueue();

  private boolean connectedAtLeastOnce = false;
  /** Workaround, I can't make scheduler.cancel() work! */
  private boolean reconnectionDisabled = false;
  
  private final String sessionToken;
  private final String serverUrl;

  
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
  public void start() {    
    try {
      setState(ConnectState.CONNECTING);
      connectedAtLeastOnce = false;
      sequenceNo = 0;
      socket = new WaveSocketWS(serverUrl, sessionToken, this);
      socket.connect();
    } catch (Exception e) {
      // Report exception if it's first attempt to connect the websocket
      Console.log("WaveWebSocketClient.start() error: "+e.getMessage());
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
      Console.log("WaveWebSocketClient.connect() error: "+e.getMessage());
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
        String token = sessionToken;
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
      Console.log("WaveWebSocketClient.onConnect() error: "+e.getMessage());
      setState(ConnectState.ERROR);
      return;
    }
  }

  @Override
  public void onDisconnect() {
    setState(ConnectState.DISCONNECTED);
    SchedulerInstance.getLowPriorityTimer().scheduleDelayed(reconnectTask, 5000);    
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
      Console.log("WaveWebSocketClient.onMessage() error: "+e.getMessage());
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
          Console.log("WaveWebSocketClient.onMessage() error: "+e.getMessage());
          setState(ConnectState.ERROR, e.getMessage());
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
          Console.log("WaveWebSocketClient.onMessage() error: "+e.getMessage());
          setState(ConnectState.ERROR, e.getMessage());
        }

      }
    } else if ("RpcFinished".equals(messageType)) {
        if (callback != null) {
           callback.onFinished(wrapper.<RpcFinished>getPayload());
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

  public ConnectState getState() {
    return this.connectState;
  }
  
  @Override
  public void onError(String reason) {
    SchedulerInstance.getLowPriorityTimer().cancel(reconnectTask);
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
