package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.SessionManager;
import org.swellrt.beta.client.rest.operations.AccountDataResponse;

import com.google.gwt.user.client.Cookies;

public class WebSessionManager implements SessionManager {

  private static final String SESSION_COOKIE_NAME = "WSESSIONID";
  private static final String TRANSIENT_SESSION_COOKIE_NAME = "TSESSIONID";

  private AccountDataResponse accountData = null;

  public static WebSessionManager create() {
    WebSessionManager sm = new WebSessionManager();
    sm.init();
    return sm;
  }


  private WebSessionManager() {

  }

  protected void init() {

  }


  @Override
  public String getSessionId() {
    return accountData.sessionId;
  }


  @Override
  public String getTransientSessionId() {
    return accountData.transientSessionId;
  }


  @Override
  public void setSession(AccountDataResponse profile) {
    this.accountData = profile;
  }


  @Override
  public void removeSession() {
    Cookies.removeCookie(SESSION_COOKIE_NAME);
    Cookies.removeCookie(TRANSIENT_SESSION_COOKIE_NAME);
    this.accountData = null;
  }

  @Override
  public boolean isSession() {
    return (this.accountData != null);
  }


  @Override
  public String getWaveDomain() {
    return this.accountData.domain;
  }

  @Override
  public String getUserId() {
    return this.accountData.id;
  }

}
