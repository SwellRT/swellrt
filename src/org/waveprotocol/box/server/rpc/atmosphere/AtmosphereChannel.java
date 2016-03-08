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

package org.waveprotocol.box.server.rpc.atmosphere;


import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.waveprotocol.box.server.rpc.ProtoCallback;
import org.waveprotocol.box.server.rpc.WebSocketChannel;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;

/**
 * An atmosphere wrapper for the WebSocketChannel type.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public class AtmosphereChannel extends WebSocketChannel  {


  private static final Log LOG = Log.get(AtmosphereChannel.class);

  private final Broadcaster broadcaster;
  private AtmosphereResource resource;

  private boolean isValid = true;


  /**
   * Creates a new AtmosphereChannel using the callback for incoming messages.
   *
   * @param callback A ProtoCallback instance called with incoming messages.
   */
  public AtmosphereChannel(ProtoCallback callback, String id) {
    super(callback);
    this.broadcaster = BroadcasterFactory.getDefault().get();
  }

  /**
   * A new resource connection has been associated with
   * this channel
   * @param resource the Atmosphere resource object
   */
  public void bindResource(AtmosphereResource resource) {
    this.broadcaster.removeAtmosphereResource(this.resource);
    this.broadcaster.addAtmosphereResource(resource);
    this.resource = resource;
  }


  public Broadcaster getBroadcaster() {
    return broadcaster;
  }

  /**
   * The atmosphere resource has received a new post message
   * @param message the message
   */
  public void onMessage(String message) {

    if (isValid) handleMessageString(message);
  }

  /**
   * The atmosphere resource has been closed
   */
  public void unbindResource() {
    broadcaster.removeAtmosphereResource(this.resource);
    this.resource = null;
  }


  public boolean hasResources() {
    return broadcaster.getAtmosphereResources().size() > 0;
  }

  public void sendClientNeedUpgrade() {

    broadcaster.broadcast("X-RESPONSE:UPGRADE");
    LOG.info("Sending upgrade response to client " + resource != null ? resource.uuid()
        : "unknown.");
    isValid = false;
  }

  public void sendException(Exception e) {

    broadcaster.broadcast("X-RESPONSE:SERVER_ERROR:" + e.getMessage());
    LOG.info("Sending error response to client " + resource != null ? resource.uuid()
        : "unknown.");
    isValid = false;
  }

  public void sendException(String e) {

    broadcaster.broadcast("X-RESPONSE:SERVER_ERROR:" + e);
    LOG.info("Sending error response to client " + resource != null ? resource.uuid() : "unknown.");
    isValid = false;
  }

  /**
   * Send the given data String
   *
   * @param data
   * @throws IOException
   */
  @Override
  protected void sendMessageString(String data) throws IOException {

    if (!isValid) {
      LOG.warning("Channel is no longer valid. Does client need upgrade? Old RPCs hasn't been disposed?");
      return;
    }

    if (broadcaster == null || broadcaster.isDestroyed()) {
      // Just drop the message. It's rude to throw an exception since the
      // caller had no way of knowing.
      LOG.warning("Atmosphere Channel is not connected");
    } else {

      LOG.fine("BROADCAST " + data);
      broadcaster.broadcast(data);
    }
  }




}
