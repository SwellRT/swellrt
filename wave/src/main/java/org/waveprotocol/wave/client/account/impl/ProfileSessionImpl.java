package org.waveprotocol.wave.client.account.impl;

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileSession;
import org.waveprotocol.wave.client.common.util.RgbColor;

public class ProfileSessionImpl implements ProfileSession {

  private static long USER_INACTIVE_WAIT = 60 * 1000; // ms
  
  private final String id;
  private final RgbColor color;
  private final Profile profile;
  private final AbstractProfileManager manager;
  
  private long lastOnlineTime = 0;
  
  public ProfileSessionImpl(Profile profile, AbstractProfileManager manager, String sessionId, RgbColor color) {
    this.color = color;
    this.id = sessionId;
    this.profile = profile;
    this.manager = manager;
  }
  
  @Override
  public String getSessionId() {
    return id;
  }

  @Override
  public RgbColor getColor() {
    return color;
  }

  @Override
  public void setOnline() {
    this.lastOnlineTime = System.currentTimeMillis(); 
    this.manager.fireOnOnline(this);
  }

  @Override
  public void setOffline() {
    this.lastOnlineTime = 0; 
    this.manager.fireOnOffline(this);
  }
  
  @Override
  public boolean isOnline() {
    return USER_INACTIVE_WAIT < (System.currentTimeMillis() - lastOnlineTime);    
  }

  @Override
  public Profile getProfile() {
    return profile;
  }

}
