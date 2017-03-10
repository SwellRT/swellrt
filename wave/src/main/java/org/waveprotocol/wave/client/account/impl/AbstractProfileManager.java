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

package org.waveprotocol.wave.client.account.impl;

import org.swellrt.beta.client.js.Console;
import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileListener;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.ProfileSession;
import org.waveprotocol.wave.client.account.RawProfileData;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.common.util.RgbColorPalette;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsOptional;

/**
 * Manage user profiles and their sessions.
 * <p><br>
 * TODO refresh online status automatically
 * TODO refresh cached profiles data automatically 
 * 
 * @author yurize@apache.org (Yuri Zelikov)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public abstract class AbstractProfileManager implements ProfileManager {
      
  public interface RequestProfileCallback {    
    void onCompleted(RawProfileData rawData);    
  }


  private int currentColourIndex = 0;
  private int refreshInterval = ProfileManager.USER_INACTIVE_WAIT;
    
  protected final StringMap<Profile> profiles = CollectionUtils.createStringMap();
  protected final StringMap<ProfileSession> sessions = CollectionUtils.createStringMap();  
  protected final CopyOnWriteSet<ProfileListener> listeners = CopyOnWriteSet.create();
  
  private final Scheduler.IncrementalTask checkStatusTask = new Scheduler.IncrementalTask() {
    
    @Override
    public boolean execute() {
      Console.log("Refreshing sessions status ");

      sessions.each(new ProcV<ProfileSession>() {

        @Override
        public void apply(String key, ProfileSession value) {          
          if (!value.isOnline()) {
            fireOnOffline(value);
          }
        }
        
      });
      
      return true;
      
    }
  };

  protected AbstractProfileManager() {
    
  }
  
  protected AbstractProfileManager(int refreshInterval) {
    this.refreshInterval = refreshInterval;
  }
  
  @Override
  public void enableStatusEvents(boolean enable) {
    
    if (enable) {
    
      if (!SchedulerInstance.getLowPriorityTimer().isScheduled(checkStatusTask)) {
        // Refresh connection status each minute
        SchedulerInstance.getLowPriorityTimer().scheduleRepeating(checkStatusTask, refreshInterval, refreshInterval + 1000);
      }
    
    } else {
      
      if (SchedulerInstance.getLowPriorityTimer().isScheduled(checkStatusTask)) {
        SchedulerInstance.getLowPriorityTimer().cancel(checkStatusTask);
      }
      
    }
  }
 

      
  /** Internal helper that rotates through the colours. */
  private RgbColor getNextColour(String id) {
    int colorIndex = id.hashCode() % RgbColorPalette.PALETTE.length;
    RgbColor colour = RgbColorPalette.PALETTE[colorIndex].get("400");
    return colour;  
  }
  
    
  @Override
  public final Profile getProfile(ParticipantId participantId) {
    
    if (!profiles.containsKey(participantId.getAddress())) {
      
      final Profile profile = createBareProfile(participantId);
      
      requestProfile(participantId, new RequestProfileCallback() {
        
        @Override
        public void onCompleted(RawProfileData rawData) {
          profile.update(rawData);
          fireOnUpdated(profile);
        }
      });       
      
      profiles.put(participantId.getAddress(), profile);     
    } 
    
    return profiles.get(participantId.getAddress());
  }
  
  
  
  @Override
  public final ProfileSession getSession(String sessionId, @JsOptional ParticipantId participantId) {
  
    if (participantId != null) {    
      Profile profile = getProfile(participantId);  
      
      if (!sessions.containsKey(sessionId)) {
        sessions.put(sessionId, new ProfileSessionImpl(profile, this, sessionId, getNextColour(sessionId)));
        fireOnLoaded(sessions.get(sessionId));
      }    
    }
    
    return sessions.get(sessionId);
  }
  
  
  private Profile createBareProfile(ParticipantId participantId) {
    return new ProfileImpl(participantId, this);
  }
  
  
  @Override
  public boolean shouldIgnore(ParticipantId participant) {
    return false;
  }
  
  @Override
  public final Profile getCurrentProfile() {
    return getProfile(getCurrentParticipantId());     
  }
  

  @Override
  public void addListener(ProfileListener listener) {
    listeners.add(listener);
  }

  @Override
  public void removeListener(ProfileListener listener) {
    listeners.remove(listener);
  }
  
  protected void fireOnUpdated(Profile profile) {
    for (ProfileListener listener : listeners) {
      listener.onUpdated(profile);
    }
  }
  
  protected void fireOnOffline(ProfileSession profile) {
    for (ProfileListener listener : listeners) {
      listener.onOffline(profile);
    }
  }
   
  protected void fireOnOnline(ProfileSession profile) {
    for (ProfileListener listener : listeners) {
      listener.onOnline(profile);
    }
  }  
  
  protected void fireOnLoaded(ProfileSession profile) {
    for (ProfileListener listener : listeners) {
      listener.onLoaded(profile);
    }
  }  
  
  /**
   * Asynchronous method to retrieve profile data.
   * 
   * @param participantId
   * @param callback
   */
  protected abstract void requestProfile(ParticipantId participantId, RequestProfileCallback callback);
  
}
