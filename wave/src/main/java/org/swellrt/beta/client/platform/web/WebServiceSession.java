package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.ServiceSession;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.wave.WaveDeps;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gwt.user.client.Cookies;

public class WebServiceSession implements ServiceSession {

  private static final String TOKEN_SEPARATOR = ":";

  /** Generate only one window id for this web session */
  protected static String WINDOW_ID = WaveDeps.getRandomBase64(4);

  static {
    String now = String.valueOf(System.currentTimeMillis());
    WINDOW_ID += now.substring(now.length() - 4, now.length());
  }

  private static final String SESSION_COOKIE_NAME = "WSESSIONID";
  private static final String TRANSIENT_SESSION_COOKIE_NAME = "TSESSIONID";

  private Account account;
  private boolean existsSessionCookie = false;
  private final ParticipantId participantId;


  protected WebServiceSession(Account account) {
    this.account = account;
    this.participantId = ParticipantId.ofUnsafe(account.getId());
  }

  protected void check() {
    if (account == null)
      throw new IllegalStateException("Session is destroyed");
  }

  @Override
  public void destroy() {
    Cookies.removeCookie(SESSION_COOKIE_NAME);
    Cookies.removeCookie(TRANSIENT_SESSION_COOKIE_NAME);
    account = null;
  }

  @Override
  public void setSessionCookie(boolean existsSessionCookie) {
    check();
    this.existsSessionCookie = existsSessionCookie;
  }

  @Override
  public boolean isSessionCookie() {
    check();
    return existsSessionCookie;
  }

  @Override
  public String getHttpSessionId() {
    check();
    return account.getSessionId();
  }

  @Override
  public String getTransientSessionId() {
    check();
    return account.getTransientSessionId();
  }


  @Override
  public String getSessionToken() {
    check();
    return account.getSessionId() + TOKEN_SEPARATOR + account.getTransientSessionId() + TOKEN_SEPARATOR + WINDOW_ID;
  }

  @Override
  public ParticipantId getParticipantId() {
    check();
    return participantId;
  }

  @Override
  public String getWaveDomain() {
    check();
    return account.getDomain();
  }

}
