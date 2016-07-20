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
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Sets;

import org.dom4j.Attribute;
import org.dom4j.Element;
import org.joda.time.DateTimeUtils;
import org.json.JSONObject;
import org.json.JSONArray;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;

import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Represents Matrix room status for a specific remote domain. This class only
 * exposes one public method; {@link #getRoomForRemoteId}.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
public class RemoteRoom {
  private static final Logger LOG = Logger.getLogger(RemoteDisco.class.getCanonicalName());

  enum Status {
    INIT, PENDING, COMPLETE
  }


  private final Random random = new SecureRandom();
  private MatrixPacketHandler handler; 
  private final String remoteId;
  private final AtomicReference<Status> status;
  private final Queue<SuccessFailCallback<String, String>> pending;

  // Result JID field that will be available on COMPLETE status.
  private String remoteRoom;

  // Error field that will be available on COMPLETE status.
  private FederationError error;


  // These two values are used for tracking success and failure counts.
  // Not yet exposed in the fedone waveserver.
  public static final LoadingCache<String, AtomicLong> statDiscoSuccess =
      CacheBuilder.newBuilder().build(new CacheLoader<String, AtomicLong>() {
            @Override
            public AtomicLong load(String remoteId) {
              return new AtomicLong();
            }
          });

  public static final LoadingCache<String, AtomicLong> statDiscoFailed =
      CacheBuilder.newBuilder().build(new CacheLoader<String, AtomicLong>() {
            @Override
            public AtomicLong load(String remoteId) {
              return new AtomicLong();
            }
          });

  public RemoteDisco(MatrixPacketHandler handler, String remoteId, long failExpirySecs,
                   long successExpirySecs) {
    this.handler = handler;
    status = new AtomicReference<Status>(Status.INIT);
    pending = new ConcurrentLinkedQueue<SuccessFailCallback<String, String>>();
    this.remoteId = remoteId;
    this.creationTimeMillis = DateTimeUtils.currentTimeMillis();
    this.failExpirySecs = failExpirySecs;
    this.successExpirySecs = successExpirySecs;
  }

  public void searchRemoteRoom(SuccessFailCallback<String, String> callback) {
    if (status.get().equals(Status.COMPLETE)) {
      complete(callback);
    } else if (status.compareAndSet(Status.INIT, Status.PENDING)) {
      pending.add(callback);
      startDisco();
    } else {
      pending.add(callback);

      // If we've become complete since the start of this method, complete
      // all possible callbacks.
      if (status.get().equals(Status.COMPLETE)) {
        SuccessFailCallback<String, String> item;
        while ((item = pending.poll()) != null) {
          complete(item);
        }
      }
    }
  }

  public boolean ttlExceeded() {
    if (status.get() == Status.COMPLETE) {
      if (remoteId == null) {
        // Failed disco case
        if (DateTimeUtils.currentTimeMillis() >
            (creationTimeMillis + (1000 * failExpirySecs))) {
          return true;
        }
      } else {
        // Successful disco case
        if (DateTimeUtils.currentTimeMillis() >
            (creationTimeMillis + (1000 * successExpirySecs))) {
          return true;
        }
      }
    }
    return false;
  }

  private void complete(SuccessFailCallback<String, String> callback) {
    Preconditions.checkState(status.get().equals(Status.COMPLETE));
    if (remoteId != null) {
      callback.onSuccess(remoteId);
    } else {
      // TODO(thorogood): better toString, or change failure type to FederationError
      callback.onFailure(error.toString());
    }
  }

  private void startDisco() {

    final Runnable requester = new Runnable() {

      final PacketCallback callback = new PacketCallback() {
        @Override
        public void run(JSONObject result) {
          String roomID = result.getString("room_id");
          finish(roomID, null);
        }

        @Override
        public void error(FederationError error) {
          LOG.info("Remote server " + RemoteId + " failed on disco items: "
              + error.getErrorCode());
          finish(null, error);
        }
      };

      @Override
      public void run() {

        //Checking if room exists
        LOG.info("Checking room existance for: " + remoteId);

        String roomAlias = "%23" + MatrixUtil.encodeDomain(remoteId) + ":" + handler.getDomain();
        JSONObject packet = handler.sendBlocking(MatrixUtil.getRoom(roomAlias);

        Request request = null;

        if(packet) {

          roomId = packet.getString("room_id");

          JSONObject packet = handler.sendBlocking(MatrixUtil.getMembers(room);
          JSONArray members = packet.getJSONArray("chunk");

          if(members.length() == 2) {
            //Sending Ping
            LOG.info("Pinging room  for: " + remoteId);

            request = MatrixUtil.createMessage(roomId);
            request.addBody("msgtype", "m.notice");
            request.addBody("body", "ping");
          }
          else {
            request = MatrixUtil.inviteRoom(roomId);
            request.addBody("user_id","@wave:" + remoteId);
          }
        }
        else {
          //Creating new room
          LOG.info("Creating room  for: " + remoteId);

          request = MatrixUtil.createRoom();
          request.addBody("room_alias_name", MatrixUtil.encodeDomain(remoteId));
          packet = handler.sendBlocking(request);

          roomId = packet.getString("room_id");

          request = MatrixUtil.inviteRoom(roomId);
          request.addBody("user_id","@wave:" + remoteId);
        }

        handler.send(request, callback);
      }
    };

    // Kick off requester!
    requester.run();
  }

  boolean finish(String roomId, FederationError error) {
    Preconditions.checkArgument((roomId != null)^(error != null));
    if (!status.compareAndSet(Status.PENDING, Status.COMPLETE)) {
      return false;
    }

    // Set either the result room or error state.

    try {
      if (roomId != null) {
        this.remoteRoom = roomId;
        LOG.info("Discovered remote Room: " + remoteRoom + " for " + remoteId);
          statDiscoSuccess.get(remoteId).incrementAndGet();
      } else if (error != null) {
        this.error = error;
        LOG.info("Could not discover remote Room: " + error + " for " + remoteId);
        statDiscoFailed.get(remoteId).incrementAndGet();
      } else {
        throw new IllegalArgumentException("At least one of id/error must be set");
      }
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }

    // Complete all available callbacks.
    SuccessFailCallback<String, String> item;
    while ((item = pending.poll()) != null) {
      complete(item);
    }
    return true;
  }

}