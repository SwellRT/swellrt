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

package org.waveprotocol.wave.federation.matrix;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.MapMaker;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;

import java.util.concurrent.*;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides abstraction between Federation-specific code and the backing Matrix
 * transport, including support for reliable outgoing calls (i.e. calls that are
 * guaranteed to time out) and sending error responses.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
public class MatrixPacketHandler implements IncomingPacketHandler {

  private static final Logger LOG = Logger.getLogger(MatrixPacketHandler.class.getCanonicalName());

  /**
   * Inner static class representing a single outgoing call.
   */
  private static class OutgoingCall {
    PacketCallback callback;
    ScheduledFuture<?> timeout;

    OutgoingCall(PacketCallback callback) {
      this.callback = callback;
    }

    void start(ScheduledFuture<?> timeout) {
      Preconditions.checkState(this.timeout == null);
      this.timeout = timeout;
    }
  }

  private class IncomingCallback implements PacketCallback {
    private final String domain;
    private final String eventId;
    private boolean complete = false;

    IncomingCallback(String domain, String eventId) {
      this.domain = domain.split(":")[1];
      this.eventId = eventId;
    }

    @Override
    public void error(FederationError error) {
      Preconditions.checkState(!complete,
          "Must not callback multiple times for incoming packet: %s", eventId);
      complete = true;
      //sendErrorResponse(request, error);
    }

    @Override
    public void run(JSONObject packet) {
      Preconditions.checkState(!complete,
          "Must not callback multiple times for incoming packet: %s", eventId);
      // TODO(thorogood): Check outgoing response versus stored incoming request
      // to ensure that to/from are paired correctly?
      complete = true;

      room.searchRemoteId(domain, new SuccessFailCallback<String, String>() {
        @Override
        public void onSuccess(String roomId) {
          Request response = MatrixUtil.createMessageFeedback(roomId);
          response.addBody("target_event_id", eventId);
          response.addBody("body", "");
          response.addBody("data", packet);
          transport.sendPacket(response);
        }

        @Override
        public void onFailure(String errorMessage) {
        }
      });
    }
  }

  private final MatrixFederationHost host;
  private final MatrixFederationRemote remote;
  private final MatrixRoomManager room;
  private final OutgoingPacketTransport transport;
  private final String id;
  private final String serverDomain;

  // Pending callbacks to outgoing requests.
  private final ConcurrentMap<String, OutgoingCall> callbacks = new MapMaker().makeMap();
  private final ScheduledExecutorService timeoutExecutor =
    Executors.newSingleThreadScheduledExecutor();

  @Inject
  public MatrixPacketHandler(MatrixFederationHost host, MatrixFederationRemote remote,
      MatrixRoomManager room, OutgoingPacketTransport transport, Config config) {
    this.host = host;
    this.remote = remote;
    this.room = room;
    this.transport = transport;
    this.id = config.getString("federation.matrix_id");
    this.serverDomain = config.getString("federation.matrix_server_hostname");

    // Configure all related objects with this manager. Eventually, this should
    // be replaced by better Guice interface bindings.
    host.setHandler(this);
    remote.setHandler(this);
    room.setHandler(this);
  }

  @Override
  public void testDiscovery() {
    room.searchRemoteId("localhost:20000", new SuccessFailCallback<String, String>() {
      @Override
      public void onSuccess(String remoteJid) {
        System.out.println("Success :" + remoteJid);
        
      }

      @Override
      public void onFailure(String errorMessage) {
        System.out.println("Fail : localhost:20000");
      }
    });
  }

  @Override
  public void receivePacket(JSONObject packet) {

    try {

      JSONObject rooms = packet.getJSONObject("rooms");
      JSONObject invites = rooms.getJSONObject("invite");

      Iterator<String> invite_it = invites.keys();
      while(invite_it.hasNext()) {
        String roomId = invite_it.next();

        System.out.println(roomId);

        if (LOG.isLoggable(Level.FINE)) {
          LOG.fine("Received incoming invite: " + roomId);
        }

        room.processRoomInvite(roomId);
      }

      JSONObject joined_rooms = rooms.optJSONObject("join");

      if(joined_rooms != null) {
        Iterator<String> joined_it = joined_rooms.keys();
        while(joined_it.hasNext()) {
          String roomId = joined_it.next();

          JSONObject roomInfo = joined_rooms.getJSONObject(roomId);
          
          JSONArray arr = roomInfo.getJSONObject("timeline").getJSONArray("events");

          for (int i=0; i < arr.length(); i++) {
            JSONObject message = arr.getJSONObject(i);

            message.put("room_id", roomId);

            if(!roomId.split(":", 2)[1].equals(serverDomain)) {
              if(message.getString("type").equals("m.room.message"))
                processMessage(message);
            }
            else {
              if(message.getString("type").equals("m.room.member") 
                  && !message.getString("sender").equals(id))
              processResponse(message);
            }

            if(message.getString("type").equals("m.room.message.feedback")
              && !message.getString("sender").equals(id))
              processResponse(message);
          }
        }
        
      }
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void send(Request request, final PacketCallback callback, int timeout) {

    try {
 
      JSONObject packet = transport.sendPacket(request);

      if(packet == null)
        callback.error(FederationErrors.internalServerError("UserId Not Found"));
      else {

        System.out.println("\n\n return:-\n"+packet+"\n\n");

        String temp_key = null;

        if(packet.has("event_id")) {
          temp_key = packet.getString("event_id");
        }
        else {
          temp_key = request.getBody().getString("user_id");
        }

        final String key = temp_key;
        final OutgoingCall call = new OutgoingCall(callback);

        if (callbacks.putIfAbsent(key, call) == null) {
          // Timeout runnable to be invoked on packet expiry.
          Runnable timeoutTask = new Runnable() {
            @Override
            public void run() {
              if (callbacks.remove(key, call)) {
                callback.error(
                    FederationErrors.newFederationError(FederationError.Code.REMOTE_SERVER_TIMEOUT));
              } else {
                // Likely race condition where success has actually occurred. Ignore.
              }
            }
          };
          call.start(timeoutExecutor.schedule(timeoutTask, timeout, TimeUnit.SECONDS));
        } else {
          String msg = "Could not send packet, ID already in-flight: " + key;
          LOG.warning(msg);

          // Invoke the callback with an internal error.
          callback.error(
              FederationErrors.newFederationError(FederationError.Code.UNDEFINED_CONDITION, msg));
        }

      }
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void processResponse(JSONObject packet) {
    try {

      String key = null;

      if(packet.getString("type").equals("m.room.member")) {
        key = packet.getString("sender");
      }
      else {
        key = packet.getJSONObject("content").getString("target_event_id");
      }

      OutgoingCall call = callbacks.remove(key);

      if (call == null) {
        LOG.warning("Received response packet without paired request: " + key);
      }
      else {
        // Cancel the outstanding timeout.
        call.timeout.cancel(false);

        LOG.fine("Invoking normal callback for: " + key);
        call.callback.run(packet);

        call.callback = null;
      }
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void processMessage(JSONObject packet) {
    try {
      PacketCallback responseCallback = 
          new IncomingCallback(packet.getString("sender"), packet.getString("event_id"));
      JSONObject content = packet.getJSONObject("content");
      if(content.getString("msgtype").equals("m.notice"))
        room.processPing(packet);
      else if(content.getString("msgtype").equals("m.message"))
        remote.update(packet, responseCallback);
      else if(content.getString("msgtype").equals("m.set")) {
        JSONObject pubsub = content.getJSONObject("data").getJSONObject("pubsub");
        JSONObject element = pubsub.getJSONObject("publish");

        if (element.getString("node").equals("wavelet"))
          host.processSubmitRequest(packet, responseCallback);
        else if (element.getString("node").equals("signer"))
          host.processPostSignerRequest(packet, responseCallback);
      }
      else if(content.getString("msgtype").equals("m.get")) {
        JSONObject pubsub = content.getJSONObject("data").getJSONObject("pubsub");
        JSONObject element = pubsub.getJSONObject("items");

        if (element.getString("node").equals("wavelet"))
            host.processHistoryRequest(packet, responseCallback);
        else if (element.getString("node").equals("signer"))
            host.processGetSignerRequest(packet, responseCallback);
      }
      
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  public JSONObject sendBlocking(Request packet) {
    return transport.sendPacket(packet);
  }

  public String getDomain() {
    return serverDomain;
  }
}