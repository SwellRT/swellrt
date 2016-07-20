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
import org.json.JSONObject;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.xmpp.packet.IQ;
import org.xmpp.packet.Message;
import org.xmpp.packet.Packet;
import org.xmpp.packet.PacketError;

import java.util.concurrent.*;
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

  private static final Logger LOG = Logger.getLogger(XmppManager.class.getCanonicalName());

  /**
   * Inner non-static class representing a single incoming call. These are not
   * cancellable and do not time out; this is just a helper class so success and
   * failure responses may be more cleanly invoked.
   */
  private class IncomingCallback implements PacketCallback {
    private final JSONObject request;
    private boolean complete = false;

    IncomingCallback(JSONObject request) {
      this.request = request;
    }

    @Override
    public void error(FederationError error) {
      Preconditions.checkState(!complete,
          "Must not callback multiple times for incoming packet: %s", request);
      complete = true;
      //sendErrorResponse(request, error);
    }

    @Override
    public void run(Request response) {
      Preconditions.checkState(!complete,
          "Must not callback multiple times for incoming packet: %s", request);

      complete = true;
      transport.sendPacket(response);
    }
  }

  private final MatrixFederationHost host;
  private final MatrixFederationRemote remote;
  private final MatrixRoomManager room;
  private final OutgoingPacketTransport transport;
  private final String id;
  private final String serverDomain;

  // Pending callbacks to outgoing requests.
  private final ConcurrentMap<String, PacketCallback> callbacks = new MapMaker().makeMap();

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
    //host.setManager(this);
    //remote.setManager(this);
    room.setPacketHandler(this);
  }

  @Override
  public void receivePacket(JSONObject packet) {

    JSONObject rooms = sync.getJSONObject("rooms");
    JSONObject invites = rooms.getJSONObject("invite");

    Iterator<String> invite_it = invites.keys();
    while(invite_it.hasNext()) {
      String roomId = invite_it.next();

      if (LOG.isLoggable(Level.FINE)) {
        LOG.fine("Received incoming invite: " + roomId);
      }

      room.processRoomInvite(roomId, new IncomingCallback(rooms.getJSONObject(roomId)));
    }

    JSONObject joined_rooms = rooms.getJSONObject("join");

    Iterator<String> joined_it = joined_rooms.keys();
    while(joined_it.hasNext()) {
      String roomId = joined_it.next();

      if(!roomId.split(":", 2)[1].equals(serverDomain)) {
        JSONObject roomInfo = rooms.getJSONObject(roomId);
        
        JSONArray arr = roomInfo.getJSONObject("timeline").getJSONArray("events");

        for (int i=0; i < arr.length(); i++) {
          JSONObject message = arr.getJSONObject(i);

          message.put("room_id", room_id);

          if(message.getString("type").equals("m.room.message"))
            processMessage(message);
          else if(message.getString("type").equals("m.room.message.feedback")
            || (message.getString("type").equals("m.room.member") 
              && !message.getString("sender").equals(id)) ) {
            processResponse(message);
          }
        }
    }
    
  }

  public void send(Request request, final PacketCallback callback) {

    JSONObject packet = transport.sendPacket(request);

    if(packet == null)
      callback.error(null);
    else {

      String key = null;

      if(packet.has("event_id")) {
        key = packet.getString("room_id");
      }
      else {
        key = packet.getString("event_id");
      }

      callbacks.putIfAbsent(key, call);
    }
  }

  private void processResponse(JSONObject packet) {
    String key = null;

    if(packet.getString("type").equals("m.room.member")) {
      key = packet.getString("room_id");
    }
    else {
      key = packet.getJSONObject("content").getString("target_event_id");
    }

    PacketCallback callback = callbacks.remove(key);

    if (callback == null) {
      LOG.warning("Received response packet without paired request: " + key);
    } else {

    
      LOG.fine("Invoking normal callback for: " + key);
      call.callback.run(packet);
        
      // Clear call's reference to callback, otherwise callback only
      // becomes eligible for GC once the timeout expires, because
      // timeoutExecutor holds on to the call object till then, even
      // though we cancelled the timeout.
      call.callback = null;
    }
  }

  private void processMessage(JSONObject packet) {

  }

  public JSONObject sendBlocking(Request packet) {
    return transport.sendPacket(packet);
  }

  public String getDomain() {
    return serverDomain;
  }
}