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

	@Inject
	public AppServicePacketTransport(IncomingPacketHandler handler, Config config) {
		this.handler = handler;
	}

	@Override
    public void run() {
    	
    }

	@Override
	public void sendPacket(JSONArray packet) {

	}


}