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

/**
 * This interface serves as a proxy interface wrapper for concrete socket implementations
 * like {@link com.google.gwt.websockets.client.WebSocket}.
 * <p>
 * Note(pablojan): onError() callback is intend to signal fatal errors that can't be recovered. They must force to
 * start a fresh new SwellRT/Wave infrastructure (see ServiceContext) 
 *
 * @author tad.glines@gmail.com (Tad Glines)
 */
public interface WaveSocket {


  interface WaveSocketCallback {
    void onConnect();
    void onDisconnect();
    /** Use to signal fatal errors that force app to reload */
    void onError(String reason); 
    void onMessage(String message);
  }
  
  void connect();
  void disconnect();
  void sendMessage(String message);

}
