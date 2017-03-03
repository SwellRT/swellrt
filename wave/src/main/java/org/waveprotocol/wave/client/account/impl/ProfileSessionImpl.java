package org.waveprotocol.wave.client.account.impl;

import org.swellrt.beta.client.js.Console;
import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileSession;
import org.waveprotocol.wave.client.common.util.RgbColor;

import com.google.gwt.core.client.Duration;
import com.google.gwt.core.client.GWT;

public class ProfileSessionImpl implements ProfileSession {

  private static long USER_INACTIVE_WAIT = 60 * 1000; // ms
  
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
  public String getSessionId() {
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
    if ((timestamp - lastActivityTime) > USER_INACTIVE_WAIT) {
      this.manager.fireOnOnline(this);
    }
    this.lastActivityTime = timestamp;
  }

  @Override
  public void setOffline() {
    this.lastActivityTime = 0; 
    this.manager.fireOnOffline(this);
  }
  
  @Override
  public boolean isOnline() {
    return USER_INACTIVE_WAIT < (getCurrentTime() - lastActivityTime);    
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
}
