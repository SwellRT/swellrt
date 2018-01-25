package org.swellrt.beta.client.platform.java;

import org.swellrt.beta.client.ServiceSession;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.waveprotocol.wave.model.wave.ParticipantId;

public class JavaServiceSession implements ServiceSession {

  protected static final String WINDOW_ID = "0";

  private static final String TOKEN_SEPARATOR = ":";

  private Account account;
  private boolean existsSessionCookie = false;
  private ParticipantId participanId;

  protected JavaServiceSession(Account account) {
    this.account = account;
    this.participanId = ParticipantId.ofUnsafe(account.getId());
  }

  protected void check() {
    if (account == null)
      throw new IllegalStateException("Session is destroyed");
  }

  @Override
  public void destroy() {
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
    return account.getSessionId() + TOKEN_SEPARATOR + account.getTransientSessionId()
        + TOKEN_SEPARATOR + WINDOW_ID;
  }

  @Override
  public ParticipantId getParticipantId() {
    check();
    return participanId;
  }

  @Override
  public String getWaveDomain() {
    check();
    return account.getDomain();
  }

}
