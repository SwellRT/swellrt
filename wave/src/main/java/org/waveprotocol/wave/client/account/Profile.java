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

import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsType;

/**
 * Profile information for a participant.
 * <p>
 * A profile keeps the activity state of the user
 * in the current wave as long as the color representing her/him.
 * <p>
 * This class is intended to be shared across platforms. Not UI
 * components must be kept here.
 *
 * @author kalman@google.com (Benjamin Kalman)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public interface Profile {

  /**
   * @return the participant id for this profile
   */
  ParticipantId getParticipantId();
  
  void update(RawProfileData rawData);

  /**
   * @return the address for this profile, same as {@link #getParticipantId()}
   */
  String getAddress();

  /**
   * @return the participant's full name
   */
  String getName();

  /**
   * @return the participant's short name
   */
  String getShortName();

  /**
   * @return the URL of a participant's avatar image
   */
  String getImageUrl();
  
  /**
   * @return the color associated to this profile
   */
  RgbColor getColor();
  
  /**
   * @return true if the user is online in the current wave
   */
  boolean isOnline();
  
  void setOnline();
    
  void setOffline();
}
