package org.waveprotocol.wave.client.account.impl;

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.ProfileSession;
import org.waveprotocol.wave.client.common.util.RgbColor;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;

public class ProfileSessionImpl implements ProfileSession {
 
  private final String id;
  private final RgbColor color;
  private final Profile profile;
  private final AbstractProfileManager manager;
  
  private double lastActivityTime = 0;
  
  public ProfileSessionImpl(Profile profile, AbstractProfileManager manager, String sessionId, RgbColor color) {
    this.color = color;
    this.id = sessionId;
    this.profile = profile;
    this.manager = manager;
  }
  
  @Override
  public String getId() {
    return id;
  }

  @Override
  public RgbColor getColor() {
    return color;
  }

  @Override
  public void trackActivity() {
    this.trackActivity(getCurrentTime());
  }
  
  @Override
  public void trackActivity(double timestamp) {
    boolean fireEvent = false;
    
    if (lastActivityTime == 0) {
      fireEvent = (getCurrentTime() - timestamp) < ProfileManager.USER_INACTIVE_WAIT;  
      if (fireEvent)
        lastActivityTime = getCurrentTime();
      else
        lastActivityTime = timestamp;
      
    } else {
      fireEvent = (timestamp - lastActivityTime) > ProfileManager.USER_INACTIVE_WAIT;
      lastActivityTime = timestamp;
    }
 
       
    if (fireEvent) {
      this.manager.fireOnOnline(this);
    }
    
    
  }

  @Override
  public void setOffline() {
    this.lastActivityTime = 0; 
    this.manager.fireOnOffline(this);
  }
  
  @Override
  public boolean isOnline() {
    double timeSpan = (getCurrentTime() - lastActivityTime);
    return timeSpan < ProfileManager.USER_INACTIVE_WAIT;  
  }

  @Override
  public Profile getProfile() {
    return profile;
  }
  
  private double getCurrentTime() {
    return GWT.isClient()
        ? Duration.currentTimeMillis()
        : System.currentTimeMillis();
  }

  @Override
  public double getLastActivityTime() {
    return this.lastActivityTime;
  }
}
