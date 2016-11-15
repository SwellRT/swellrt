package org.swellrt.beta.client.js;

import org.swellrt.beta.client.SessionManager;
import org.swellrt.beta.client.operation.data.ProfileData;

import com.google.gwt.user.client.Cookies;

public class SessionManagerJs implements SessionManager {

  private static final String SESSION_COOKIE_NAME = "WSESSIONID";
  private static final String WINDOW_ID_COUNTER_ITEM = "swellrt_wid_counter";
  
  
  private final ProfileData emptyProfile = new ProfileData() {

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
    
  };
 
  
  private ProfileData profile = emptyProfile;
  private String windowId = null;
  
  
  public static SessionManagerJs create() {
    SessionManagerJs sm = new SessionManagerJs();
    sm.init();
    return sm;
  }
  
  
  private SessionManagerJs() {
    
  }
 
  protected void init() {
    
    try {
      
      if (LocalStorage.getItem(WINDOW_ID_COUNTER_ITEM) == null) {
        LocalStorage.setItem(WINDOW_ID_COUNTER_ITEM, 0);
      }
      
      String counterStr = (String) LocalStorage.getItem(WINDOW_ID_COUNTER_ITEM);
      int counter = Integer.parseInt(counterStr);
      counter++;      
      LocalStorage.setItem(WINDOW_ID_COUNTER_ITEM, counter);
      windowId = Integer.toString(counter);
      
    } catch (Exception e) {
      // I don't know which exception would be thrown by JSInterop    
    }
    
  }


  @Override
  public String getWindowId() {        
    return windowId;
  }

  @Override
  public String getSessionId() {
    return profile.getSessionId();
  }

  @Override
  public String getSessionToken() {
    return profile.getSessionId()+ (windowId != null ? ":"+windowId : "");
  }


  @Override
  public void setSession(ProfileData profile) {
    this.profile = profile;    
  }


  @Override
  public void removeSession() {          
    Cookies.removeCookie(SESSION_COOKIE_NAME);
    this.profile = emptyProfile;
  }
  
  @Override
  public boolean isSession() {
    return (this.profile != emptyProfile);
  }


  @Override
  public String getWaveDomain() {
    return this.profile.getDomain();
  }
  
  @Override 
  public String getUserId() {
    return this.profile.getId();
  }

}
