package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.SessionManager;
import org.waveprotocol.wave.client.account.ServerAccountData;

import com.google.gwt.user.client.Cookies;

public class WebSessionManager implements SessionManager {

  private static final String SESSION_COOKIE_NAME = "WSESSIONID";
  private static final String TRANSIENT_SESSION_COOKIE_NAME = "TSESSIONID";

  private final ServerAccountData emptyAccountData = new ServerAccountData() {

    @Override
    public String getId() {
      return null;
    }

    @Override
    public String getEmail() {
      return null;
    }

    @Override
    public String getLocale() {
      return "en";
    }

    @Override
    public String getAvatarUrl() {
      return null;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public String getSessionId() {
      return null;
    }

    @Override
    public String getDomain() {
      return null;
    }

    @Override
    public String getTransientSessionId() {
      return null;
    }

  };


  private ServerAccountData accountData = emptyAccountData;


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
    return accountData.getSessionId();
  }


  @Override
  public String getTransientSessionId() {
    return accountData.getTransientSessionId();
  }


  @Override
  public void setSession(ServerAccountData profile) {
    this.accountData = profile;
  }


  @Override
  public void removeSession() {
    Cookies.removeCookie(SESSION_COOKIE_NAME);
    Cookies.removeCookie(TRANSIENT_SESSION_COOKIE_NAME);
    this.accountData = emptyAccountData;
  }

  @Override
  public boolean isSession() {
    return (this.accountData != emptyAccountData);
  }


  @Override
  public String getWaveDomain() {
    return this.accountData.getDomain();
  }

  @Override
  public String getUserId() {
    return this.accountData.getId();
  }

}
