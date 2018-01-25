package org.swellrt.beta.client;

import org.swellrt.beta.client.rest.operations.params.Account;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Tracks HTTP session info for the current logged in participant.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface ServiceSession {

  public interface Factory {

    ServiceSession create(Account accountData);

    String getWindowId();

  }

  /**
   * Creates a session object for an user that is logged in.
   *
   * @param accountData
   * @return
   */
  public static ServiceSession create(Account account) {
    return ServiceDeps.serviceSessionFactory.create(account);
  }

  /**
   * Returns an id identifying the current browser tab. This is a static method
   * as long as it doesn't change during execution.
   */
  public static String getWindowId() {
    return ServiceDeps.serviceSessionFactory.getWindowId();
  }

  /** Clean up session info, e.g. cookies */
  public void destroy();

  /** Set if there is a session cookie */
  public void setSessionCookie(boolean inPlace);

  /** Check if there is a session cookie */
  public boolean isSessionCookie();

  /**
   * Returns the HTTP session id. If cookies are available, this session is kept
   * in the browser indefinitely.
   */
  public String getHttpSessionId();

  /**
   * Returns a transient session id. This id lives as long as the browser is
   * open.
   */
  public String getTransientSessionId();

  /**
   * The session token puts together all session id's (http, transient, window)
   * in a single string.
   */
  public String getSessionToken();

  /** Returns the participant id of this session */
  public ParticipantId getParticipantId();

  /** Returns wave domain of the current server */
  public String getWaveDomain();

}
