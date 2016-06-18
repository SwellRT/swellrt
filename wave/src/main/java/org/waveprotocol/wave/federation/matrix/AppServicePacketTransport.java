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

import com.google.inject.Inject;
import com.typesafe.config.Config;
import org.json.JSONArray;

import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

/**
 * Talks to a Matrix server using the Client-Server API.
 *
 * Implements {@link OutgoingPacketTransport} allowing users to send packets,
 * and accepts an {@link IncomingPacketHandler} which can process incoming
 * packets.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
public class AppServicePacketTransport implements Runnable, OutgoingPacketTransport {
	
  private static final Logger LOG = 
      Logger.getLogger(AppServicePacketTransport.class.getCanonicalName());

  private final IncomingPacketHandler handler;
  private final String appServiceName;
  private final String appServiceToken;
  private final String serverDomain;
  private final String serverAddress;
  private final int serverPort;

  // Contains packets queued but not sent (while offline).
  private final Queue<Request> queuedPackets;

  // Object used to lock around online/offline state changes.
  private final Object connectionLock = new Object();

  private ExternalComponentManager componentManager = null;
  private boolean connected = false;

  @Inject
  public AppServicePacketTransport(IncomingPacketHandler handler, Config config) {
    this.handler = handler;
    this.appServiceName = config.getString("federation.matrix_appservice_name");
    this.appServiceToken = config.getString("federation.matrix_appservice_token");
    this.serverDomain = config.getString("federation.matrix_server_hostname");
    this.serverAddress = config.getString("federation.matrix_server_ip");
    this.serverPort = config.getInt("federation.xmpp_server_component_port");

    queuedPackets = new LinkedList<>();
  }

  @Override
  public void run() {
  	
  }

  @Override
  public void sendPacket(Request packet) {

  }

  public void setUp() {

  }

}