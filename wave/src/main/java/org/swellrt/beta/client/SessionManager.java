package org.swellrt.beta.client;

import org.swellrt.beta.client.rest.operations.AccountDataResponse;

/**
 * Manage session data. Implementations are platform dependent: Web, Android...
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface SessionManager {

  public class ParticipantId {

  }

  public void setSession(AccountDataResponse profile);

  public String getSessionId();

  public String getTransientSessionId();


   /**
   * Remove the session. Session Cookie should be
   * delete here.
   */
  public void removeSession();

  boolean isSession();

  public String getWaveDomain();

  String getUserId();
}
