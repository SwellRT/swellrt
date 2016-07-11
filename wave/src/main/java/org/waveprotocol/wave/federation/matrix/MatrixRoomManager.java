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
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.dom4j.Element;
import org.json.JSONObject;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides public methods to accept invite and join a room for receiving packets
 * (via {@link MatrixPacketHandler}), as well as managing room creation/search for
 * communication with remote Server via {{@link #getRoomForRemoteId}.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
public class MatrixRoomManager {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(MatrixRoomManager.class.getCanonicalName());

  // This tracks the number of room search attempts started.
  public static final LoadingCache<String, AtomicLong> statSearchStarted =
      CacheBuilder.newBuilder().build(new CacheLoader<String, AtomicLong>() {
            @Override
            public AtomicLong load(@SuppressWarnings("NullableProblems") String id) {
              return new AtomicLong();
            }
  });

  // private final LoadingCache<String, RemoteRoom> roomRequests;
  private final String serverDescription;

  private MatrixPacketHandler handler = null;

  final long roomExpirationHours;

  @Inject
  public MatrixRoomManager(Config config) {
    this.serverDescription = config.getString("federation.matrix_server_description");

    this.roomExpirationHours = config.getDuration("federation.room_search_expiration", TimeUnit.HOURS);

    //noinspection NullableProblems
    roomRequests =
        CacheBuilder.newBuilder().expireAfterWrite(
                roomExpirationHours, TimeUnit.HOURS).build(
        new CacheLoader<String, RemoteId>() {

          @Override
          public RemoteRoom load(String id) throws Exception {
            statDiscoStarted.get(id).incrementAndGet();
            return new RemoteRoom(manager, id);
          }
        });
  }

  public void setPacketHandler(MatrixPacketHandler handler) {
    this.handler = handler;
  }

  void processRoomInvite(JSONObject room, PacketCallback responseCallback) {
    
    Request response = MatrixUtil.joinRoom(roomId);

    responseCallback.run(response);
  }

  public void searchRemoteId(String remoteId, SuccessFailCallback<String, String> callback) {
    Preconditions.checkNotNull("Must call setManager first", manager);
    RemoteRoom search = roomRequests.getIfPresent(remoteId);
    if (search != null) {
      // This is a race condition, but we don't care if we lose it, because the ttl timestamp
      // won't be exceeded in that case.
      if (search.ttlExceeded()) {
        if (LOG.isLoggable(Level.FINE)) {
          LOG.info("searchRoom for " + remoteId + ": result ttl exceeded.");
        }
        // TODO(arb): should we expose the disco cache somehow for debugging?
        roomRequests.invalidate(remoteId);
      }
    }
    try {
      roomRequests.get(remoteId).searchRemoteRoom(callback);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
}
}