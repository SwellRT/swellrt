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

package org.waveprotocol.wave.client.account;

import jsinterop.annotations.JsType;

/**
 * Listen to changes in a profile.
 *
 * @author kalman@google.com (Benjamin Kalman)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
@JsType(isNative = true)
public interface ProfileListener {  
  
  /**
   * A profile's data is updated, both from server or from
   * a remote session.
   * 
   * @param profile
   */
  void onUpdated(Profile profile);
  
  /**
   * A session object is loaded in the current editor context.
   * <p>
   * Before editor takes control of the
   * document, this event is launched for all previous sessions
   * in the document.
   * <p>
   * After editor takes control of the document,
   * this events is launched for every new session connected
   * to the document.
   * 
   * @param profile
   */
  void onLoaded(ProfileSession profile);
   
  /**
   * A session gets offline in the current editor.
   * 
   * @param profile
   */
  void onOffline(ProfileSession profile);
  
  /**
   * A session gets online in the current editor.
   * 
   * @param profile
   */
  void onOnline(ProfileSession profile);
  
}
