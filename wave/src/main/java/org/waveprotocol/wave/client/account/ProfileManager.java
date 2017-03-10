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

import org.waveprotocol.wave.client.doodad.selection.CaretAnnotationHandler;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

/**
 * Manages profiles and sessions for participants.
 * <p>
 * Session activity changes are detected when
 * a component invokes {@link ProfileSession#trackActivity()}.
 * <p>
 * Profile data is updated when a component invokes a setter
 * in a {@link Profile} or when implementations of this class
 * do as necessary.
 * <p>
 * TODO:
 * In current version of SwellRT, online status is managed by
 * the {@link CaretAnnotationHandler} of the editor. Move this logic to
 * a separated component that doesn't rely on the document blip to
 * store presence data. 
 * <p>
 *
 * @author kalman@google.com (Benjamin Kalman)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
@JsType(namespace = "swellrt", name = "ProfileManager")
public interface ProfileManager extends SourcesEvents<ProfileListener> {
  
  @JsIgnore
  public static int USER_INACTIVE_WAIT = 240 * 1000; // ms (4 mins)
  
  /**
   * Gets the profile for a participant.
   *
   * @param participantId id of the participant
   * @return the profile for a participant
   */
  Profile getProfile(ParticipantId participantId);
  
  /**
   * Returns whether the participant should be ignored in the context of
   * accounts.
   *
   * @param participantId the participant id to check
   * @return true if the participant should be ignored, false if not
   */
  boolean shouldIgnore(ParticipantId participantId);
    
  /**
   * Gets the session for a particular participant.
   * 
   * @param participantId
   * @param sessionId
   * @return
   */
  ProfileSession getSession(String sessionId, @JsOptional ParticipantId participantId);
 
  /**
   * @return session id of the logged in user, null otherwise 
   */
  String getCurrentSessionId();
  
  /**
   * @return id of the logged in user, null otherwise
   */
  ParticipantId getCurrentParticipantId();
  
  /**
   * @return the profile of the current logged in user
   */
  Profile getCurrentProfile();

  /**
   * Start/Stop events on session status changes (online/offline).
   * Default status is false. 
   * 
   * @param enable
   */
  void enableStatusEvents(boolean enable);
  
  
}
