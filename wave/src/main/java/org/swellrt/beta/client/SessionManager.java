package org.swellrt.beta.client;

import org.waveprotocol.wave.client.account.RawProfileData;

/**
 * Manage session data. Implementations are platform dependent: Web, Android...
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface SessionManager {

  public class ParticipantId {
    
  }
    
  public void setSession(RawProfileData profile);
     
  public String getWindowId();
  
  public String getSessionId();
  
  public String getSessionToken();
 
  /**
   * Remove the session. Session Cookie should be
   * delete here.
   */
  public void removeSession();

  boolean isSession();
  
  public String getWaveDomain();

  String getUserId();
} 
