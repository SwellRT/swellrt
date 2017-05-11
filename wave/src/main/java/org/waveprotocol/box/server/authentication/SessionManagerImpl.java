/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.box.server.authentication;

import java.io.Serializable;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.rpc.TransientSessionFilter;
import org.waveprotocol.box.server.rpc.WindowIdFilter;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.escapers.PercentEscaper;
import org.waveprotocol.wave.util.logging.Log;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.inject.Inject;

/**
 * Utility class for managing the session's authentication status.
 *
 * It generates {@link HttpWindowSession} instances for the {@link HttpSession}
 * interface.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public final class SessionManagerImpl implements SessionManager {

  /**
   * Use to keep backwards compatibility
   */
  private static final String OLD_USER_ID_ATTR = SessionManager.USER_FIELD;

  private final AccountStore accountStore;
  private final org.eclipse.jetty.server.SessionManager jettySessionManager;

  private static final Log LOG = Log.get(SessionManagerImpl.class);

  // miliseconds
  private static final long USER_SESSION_LIFETIME = 1000L * 60L * 60L * 24L * 30L; // 30 days


  //
  // Multiple user sessions per http session management
  //

  public static class SessionUser implements Serializable {

    private static final long serialVersionUID = 1L;

    public ParticipantId participanId;
    public long lastLoginTime;
    public String transientSessionId;
    public String browserWindowId;
    public boolean rememberMe;
    public int index;
    private String properties = "";

    public SessionUser(ParticipantId participanId, long lastLoginTime, String transientSessionid, String browserWindowId,
        boolean rememberMe) {
      super();
      this.participanId = participanId;
      this.lastLoginTime = lastLoginTime;
      this.transientSessionId = transientSessionid;
      this.browserWindowId = browserWindowId;
      this.rememberMe = rememberMe;
    }

    private void propertyMapToString(Map<String, String> m) {
      String s = "";
      for (Entry<String, String> e: m.entrySet()) {
        s+= e.getKey()+"="+e.getValue() +"|";
      }
      properties = s;
    }

    private Map<String, String> propertyStringToMap() {
      String[] propertyArray = properties.split("|");
      Map<String,String> map = new HashMap<String, String>();
      for (String s: propertyArray) {
        if (s != null) {
          String[] keyValue = s.split("=");
          map.put(keyValue[0], keyValue[1]);
        }
      }

      return map;
    }

    public void setProperty(String key, String value) {
      Map<String, String> propertyMap = propertyStringToMap();
      propertyMap.put(key, value);
      propertyMapToString(propertyMap);
    }

    public String getProperty(String key) {
      Map<String, String> propertyMap = propertyStringToMap();
      return propertyMap.get(key);
    }

  }


  protected static SessionUser getSessionUser(HttpSession session, ParticipantId participantId) {
    return (SessionUser) session.getAttribute(participantId.getAddress());
  }

  protected static Map<ParticipantId, SessionUser> getSessionUsers(HttpSession session) {

    Map<ParticipantId, SessionUser> map = new HashMap<ParticipantId, SessionUser>();

    Enumeration<String> names = session.getAttributeNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      if (!name.equals(OLD_USER_ID_ATTR))
        map.put(ParticipantId.ofUnsafe(name), (SessionUser) session.getAttribute(name));
    }

    return map;
  }

  protected static Map<Integer, ParticipantId> getSessionUserIndex(HttpSession session) {
    Map<Integer, ParticipantId> map = new HashMap<Integer, ParticipantId>();

    Enumeration<String> names = session.getAttributeNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      if (!name.equals(OLD_USER_ID_ATTR)) {
        SessionUser su = (SessionUser) session.getAttribute(name);
        map.put(su.index, su.participanId);
      }
    }

    return map;
  }

  protected static int addSessionUser(HttpSession session, SessionUser sessionUser) {

    Map<ParticipantId, SessionUser> sessionUsers =  getSessionUsers(session);
    Map<Integer, ParticipantId> sessionUserIndex = getSessionUserIndex(session);

    if (!sessionUsers.containsKey(sessionUser.participanId)) {
      int index = 0;
      while (index < sessionUsers.size()) {
        if (!sessionUserIndex.containsKey(index)) {
          break;
        }
        index++;
      }
      sessionUser.index = index;
    } else {
      sessionUser.index = sessionUsers.get(sessionUser.participanId).index;
    }

    // Rewrite
    session.setAttribute(sessionUser.participanId.getAddress(), sessionUser);

    return sessionUser.index;
  }

  protected static boolean removeSessionUser(HttpSession session, ParticipantId participantId) {
    boolean exists = session.getAttribute(participantId.getAddress()) != null;
    session.removeAttribute(participantId.getAddress());
    return exists;
  }


  @Inject
  public SessionManagerImpl(
      AccountStore accountStore, org.eclipse.jetty.server.SessionManager jettySessionManager) {
    Preconditions.checkNotNull(accountStore, "Null account store");
    Preconditions.checkNotNull(jettySessionManager, "Null jetty session manager");
    this.accountStore = accountStore;
    this.jettySessionManager = jettySessionManager;
  }

  //
  // Old Wave methods
  //


  @Override
  public void login(HttpSession session, ParticipantId id) {
    Preconditions.checkNotNull(session, "Session is null");
    Preconditions.checkNotNull(id, "Participant id is null");
    session.setAttribute(OLD_USER_ID_ATTR, id.getAddress());
  }

  @Override
  public boolean logout(HttpSession session) {
    Preconditions.checkNotNull(session, "Session is null");
    boolean exists = session.getAttribute(OLD_USER_ID_ATTR) != null;
    if (exists)
      session.removeAttribute(OLD_USER_ID_ATTR);
    return exists;
  }

  @Override
  public ParticipantId getLoggedInUser(HttpSession session) {
    String address = (String) session.getAttribute(OLD_USER_ID_ATTR);
    return address != null ? ParticipantId.ofUnsafe(address) : null;
  }


  @Override
  public AccountData getLoggedInAccount(HttpServletRequest request) {
    // Consider caching the account data in the session object.
    ParticipantId user = getLoggedInUser(request);
    return getAccountData(user);
  }

  @Override
  public AccountData getAccountData(ParticipantId user) {
    if (user != null) {

      if (user.isAnonymous()) {
        // Set up a fake humman account for anonymous users
        return new HumanAccountDataImpl(user);
      }

      try {
        return accountStore.getAccount(user);
      } catch (PersistenceException e) {
        LOG.warning("Failed to retrieve account data for " + user, e);
        return null;
      }
    } else {
      return null;
    }
  }

  @Override
  public AccountData getLoggedInAccount(HttpSession session) {
    ParticipantId participandId = getLoggedInUser(session);
    return getAccountData(participandId);
  }

  @Override
  public String getLoginUrl(String redirect) {
    if (Strings.isNullOrEmpty(redirect)) {
      return SIGN_IN_URL;
    } else {
      PercentEscaper escaper =
          new PercentEscaper(PercentEscaper.SAFEQUERYSTRINGCHARS_URLENCODER, false);
      String queryStr = "?r=" + escaper.escape(redirect);
      return SIGN_IN_URL + queryStr;
    }
  }



  //
  // New SwellRT methdods
  //



  @Override
  public ParticipantId getLoggedInUser(HttpServletRequest request) {

    HttpSession session = request.getSession();

    if (session == null)
      return null;

    String transientSessionId = getTransientSessionId(request);
    String browserWindowId = getBrowserWindowId(request);

    return getLoggedInUser(session, transientSessionId, browserWindowId);
  }

  protected ParticipantId getLoggedInUser(HttpSession session, String transientSessionId, String browserWindowId) {

    SessionUser loggedSessionUser = null;
    for (SessionUser su: getSessionUsers(session).values()) {
      if (su.browserWindowId.equals(browserWindowId) && su.transientSessionId.equals(transientSessionId)) {
        if (loggedSessionUser == null) {
          loggedSessionUser = su;
        } else if (su.lastLoginTime > loggedSessionUser.lastLoginTime) {
          // pick the most recent login
          loggedSessionUser = su;
        }
      }
    }

    return loggedSessionUser != null ? loggedSessionUser.participanId : null;

  }

  @Override
  public Set<ParticipantId> getAllLoggedInUser(HttpServletRequest request) {

    final Set<ParticipantId> participants = new HashSet<ParticipantId>();
    HttpSession session = request.getSession(false);

    if (session == null)
      return participants;

    getSessionUsers(session).values().forEach(su -> {
      if (isLoginSessionUser(request, su)) {
        participants.add(su.participanId);
      }
    });

    return participants;
  }


  @Override
  public ParticipantId getLoggedInUser(String token) {
    Preconditions.checkNotNull(token, "Token is null");
    String[] parts = token.split(":");
    Preconditions.checkArgument(parts.length == 3);
    String permanentSessionId = parts[0];
    String transientSessionId = parts[1];
    String browserWindowId = parts[2];

    HttpSession session = jettySessionManager.getHttpSession(permanentSessionId);
    return getLoggedInUser(session, transientSessionId, browserWindowId);
  }


  @Override
  public int login(HttpServletRequest request, ParticipantId participantId, boolean rememberMe) {
    Preconditions.checkNotNull(request, "Request is null");
    Preconditions.checkNotNull(participantId, "Participant id is null");

    HttpSession session = request.getSession(true);

    // Remember always session data
    SessionUser su = getSessionUser(session, participantId);
    if (su != null) {
        updateSessionUser(request, su);
        return su.index;
    } else {
      SessionUser userRecord =  new SessionUser(participantId, System.currentTimeMillis(), getTransientSessionId(request), getBrowserWindowId(request), rememberMe);
      return addSessionUser(session, userRecord);
    }
  }


  protected boolean isLoginSessionUser(HttpServletRequest request, SessionUser su) {
    String transientSessionId = getTransientSessionId(request);
    String browserWindowId = getBrowserWindowId(request);
    return (su.browserWindowId.equals(browserWindowId) && su.transientSessionId.equals(transientSessionId));
  }

  protected boolean canResumeSessionUser(HttpServletRequest request, SessionUser su) {

    if (su.participanId.isAnonymous())
      return false;

    if (su.rememberMe) {
      // Remember-me sessions are valid until the defined expiration time
      long nowTime = System.currentTimeMillis();
      return nowTime - su.lastLoginTime < USER_SESSION_LIFETIME;
    } else {
      // Normal sessions are valid only during a browser session
      return (su.transientSessionId != null) && su.transientSessionId.equals(getTransientSessionId(request));
    }
  }

  protected void updateSessionUser(HttpServletRequest request, SessionUser su) {
    su.lastLoginTime = System.currentTimeMillis();
    su.browserWindowId = getBrowserWindowId(request);
    su.transientSessionId = getTransientSessionId(request);
    addSessionUser(request.getSession(), su);
  }

  @Override
  public ParticipantId resume(HttpServletRequest request, Integer userIndex) {
    Preconditions.checkNotNull(request, "Request is null");

    HttpSession session = request.getSession(true);
    Map<ParticipantId, SessionUser> sessionUserMap = getSessionUsers(session);
    Map<Integer, ParticipantId> sessionUserIndex = getSessionUserIndex(session);
    if (userIndex == null || !sessionUserIndex.containsKey(userIndex)) {

      // Return first valid user session
      ParticipantId resumeAs = null;
      for (ParticipantId p: sessionUserMap.keySet()) {
        SessionUser su = sessionUserMap.get(p);
        if (canResumeSessionUser(request, su)) {
          updateSessionUser(request, su);
          resumeAs = p;
          break;
        }
      }

      return resumeAs;

    } else {
      ParticipantId resumeAs = sessionUserIndex.get(userIndex);
      SessionUser sessionUser = sessionUserMap.get(resumeAs);
      if (canResumeSessionUser(request, sessionUser)) {
        updateSessionUser(request, sessionUser);
        return resumeAs;
      }
    }

    return null;
  }


  @Override
  public boolean logout(HttpServletRequest request, ParticipantId participantId) {
    Preconditions.checkNotNull(request, "Request is null");
    Preconditions.checkNotNull(participantId, "Participant is null");

    HttpSession session = request.getSession(false);
    return session != null ? removeSessionUser(session, participantId) : false;
  }

  @Override
  public Map<Integer, ParticipantId> getSessionUsersIndex(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    Map<Integer, ParticipantId> sessionUserIndex = session != null ?  getSessionUserIndex(session) : Collections.emptyMap();
    return sessionUserIndex;
  }

  @Override
  public Cookie getTransientSessionCookie(HttpServletRequest request) {
    return getCookie(request, SessionManager.TRASIENT_SESSION_COOKIE_NAME); // TODO replace with config prop
  }

  @Override
  public Cookie getSessionCookie(HttpServletRequest request) {
    return getCookie(request, SessionManager.SESSION_COOKIE_NAME); // TODO replace with config prop
  }

  @Override
  public String getTransientSessionId(HttpServletRequest request) {
    return (String) request.getAttribute(TransientSessionFilter.REQUEST_ATTR_TSESSION_ID);
  }

  public String getBrowserWindowId(HttpServletRequest request) {
    return (String) request.getAttribute(WindowIdFilter.REQUEST_ATTR_WINDOW_ID);
  }

  @Override
  public String getSessionId(HttpServletRequest request) {
    return request.getSession().getId();
  }


  protected static Cookie getCookie(HttpServletRequest request, String name) {

    Preconditions.checkNotNull(request, "Request can't be null");
    Preconditions.checkNotNull(name, "Cookie name can't be null");

    Cookie[] cookies = request.getCookies();
    if (cookies != null)
      for (Cookie c: cookies) {
        if (c.getName().equalsIgnoreCase(name))
          return c;
      }
    return null;

  }



}
