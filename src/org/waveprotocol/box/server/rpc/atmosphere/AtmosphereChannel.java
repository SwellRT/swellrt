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


import org.atmosphere.cpr.Broadcaster;
import org.waveprotocol.box.server.rpc.ProtoCallback;
import org.waveprotocol.box.server.rpc.WebSocketChannel;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;

/**
 * An atmosphere wrapper for the WebSocketChannel type.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public class AtmosphereChannel extends WebSocketChannel {

  private static final Log LOG = Log.get(AtmosphereChannel.class);

  private final Broadcaster broadcaster;


  /**
   * Constructs a new WebSocketChannel, using the callback to handle any
   * incoming messages.
   *
   * @param callback a protocallback to be called when data arrives on this
   *        channel
   */
  public AtmosphereChannel(ProtoCallback callback, Broadcaster broadcaster) {
    super(callback);
    this.broadcaster = broadcaster;


  }

  @Override
  protected void sendMessageString(String data) throws IOException {
    broadcaster.broadcast(data);
  }



  // public void sendClientNeedUpgrade() {
  //
  // broadcaster.broadcast("X-RESPONSE:UPGRADE");
  // LOG.info("Sending upgrade response to client " + resource != null ?
  // resource.uuid()
  // : "unknown.");
  // isValid = false;
  // }
  //
  // public void sendException(Exception e) {
  //
  // broadcaster.broadcast("X-RESPONSE:SERVER_ERROR:" + e.getMessage());
  // LOG.info("Sending error response to client " + resource != null ?
  // resource.uuid()
  // : "unknown.");
  // isValid = false;
  // }
  //
  // public void sendException(String e) {
  //
  // broadcaster.broadcast("X-RESPONSE:SERVER_ERROR:" + e);
  // LOG.info("Sending error response to client " + resource != null ?
  // resource.uuid() : "unknown.");
  // isValid = false;
  // }


}
