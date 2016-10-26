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

package org.waveprotocol.box.webclient.profile;

import org.waveprotocol.box.profile.ProfileResponse;
import org.waveprotocol.box.profile.ProfileResponse.FetchedProfile;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.impl.AbstractProfileManager;
import org.waveprotocol.wave.client.account.impl.ProfileImpl;
import org.waveprotocol.wave.client.debug.logger.DomLogger;
import org.waveprotocol.wave.common.logging.LoggerBundle;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A {@link ProfileManager} that returns profiles fetched from the server.
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 */
public final class RemoteProfileManagerImpl extends AbstractProfileManager<ProfileImpl> implements
    FetchProfilesService.Callback {

  private final static LoggerBundle LOG = new DomLogger("fetchProfiles");
  private final FetchProfilesServiceImpl fetchProfilesService;

  /**
   * Deserializes {@link ProfileResponse} and updates the profiles.
   */
  static void deserializeResponseAndUpdateProfiles(RemoteProfileManagerImpl manager,
      ProfileResponse profileResponse) {
    for (FetchedProfile fetchedProfile : profileResponse.getProfiles()) {
      deserializeAndUpdateProfile(manager, fetchedProfile);
    }
  }

  static private void deserializeAndUpdateProfile(RemoteProfileManagerImpl manager,
      FetchedProfile fetchedProfile) {
    ParticipantId participantId = ParticipantId.ofUnsafe(fetchedProfile.getAddress());
    ProfileImpl profile = manager.getProfile(participantId);
    // Profiles already exist for all profiles that have been requested.
    assert profile != null;
    // Updates profiles - this also notifies listeners.
    profile.update(fetchedProfile.getName(), fetchedProfile.getImageUrl());
    manager.fireOnUpdated(profile);
  }

  public RemoteProfileManagerImpl() {
    fetchProfilesService = FetchProfilesServiceImpl.create();
  }

  @Override
  public void onFailure(String message) {
    LOG.error().log(message);
    // TODO (user) Try to re-fetch the profile.
  }

  @Override
  public void onSuccess(ProfileResponse profileResponse) {
    deserializeResponseAndUpdateProfiles(this, profileResponse);
  }

  @Override
  protected ProfileImpl requestProfile(ParticipantId participantId) {
    LOG.trace().log("Fetching profile: " + participantId.getAddress());
    fetchProfilesService.fetch(this, participantId.getAddress());    
    return new ProfileImpl(participantId, null, null);
  }

}
