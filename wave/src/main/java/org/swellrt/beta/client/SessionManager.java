package org.swellrt.beta.client;

import org.swellrt.beta.client.operation.data.ProfileData;

/**
 * Manage session data. Implementations are platform dependent: Web, Android...
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface SessionManager {

  public class ParticipantId {
    
  }
    
  public void setSession(ProfileData profile);
     
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
