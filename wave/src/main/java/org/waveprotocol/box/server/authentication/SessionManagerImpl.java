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
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
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
        s+= e.getKey()+"="+e.getValue().replace(";", "") +";";
      }
      properties = s;
    }

    private Map<String, String> propertyStringToMap() {

      Map<String,String> map = new HashMap<String, String>();
      if (properties.isEmpty()) {
        return map;
      }
      String[] propertyArray = properties.split(";");
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

    public Map<String, String> getProperties() {
      return propertyStringToMap();
    }

    public void setProperties(Map<String, String> properties) {
      propertyMapToString(properties);
    }

  }


  protected static SessionUser readSessionUser(HttpSession session, ParticipantId participantId) {
    return (SessionUser) session.getAttribute(participantId.getAddress());
  }

  protected static Map<ParticipantId, SessionUser> readAllSessionUser(HttpSession session) {

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

  protected static void writeSessionUser(HttpSession session, SessionUser sessionUser) {

    Map<ParticipantId, SessionUser> sessionUsers =  readAllSessionUser(session);

    /*
     * Map<Integer, ParticipantId> sessionUserIndex =
     * getSessionUserIndex(session);
     *
     * if (!sessionUsers.containsKey(sessionUser.participanId)) { int index = 0;
     * while (index < sessionUsers.size()) { if
     * (!sessionUserIndex.containsKey(index)) { break; } index++; }
     * sessionUser.index = index; } else { sessionUser.index =
     * sessionUsers.get(sessionUser.participanId).index; }
     */

    // Rewrite
    session.setAttribute(sessionUser.participanId.getAddress(), sessionUser);

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
    session.setAttribute(OLD_USER_ID_ATTR, id);
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
    return session != null ? (ParticipantId) session.getAttribute(OLD_USER_ID_ATTR) : null;
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
    for (SessionUser su: readAllSessionUser(session).values()) {
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
  public Set<ParticipantId> listLoggedInUsers(HttpServletRequest request) {

    final Set<ParticipantId> participants = new HashSet<ParticipantId>();
    HttpSession session = request.getSession(false);

    if (session == null)
      return participants;

    readAllSessionUser(session).values().forEach(su -> {
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
  public ParticipantId login(HttpServletRequest request, ParticipantId participantId,
      boolean rememberMe) {
    Preconditions.checkNotNull(request, "Request is null");
    Preconditions.checkNotNull(participantId, "Participant id is null");

    HttpSession session = request.getSession(true);

    if (participantId.isNewAnonymous()) {
      // For first time anonymous login we must complete the participant id
      participantId = ParticipantId.anonymousOfUnsafe(session.getId(), participantId.getDomain());
    }

    // Remember always session data
    SessionUser su = readSessionUser(session, participantId);
    if (su != null) {
      updateSessionUser(request, su);
    } else {
      SessionUser userRecord =  new SessionUser(participantId, System.currentTimeMillis(), getTransientSessionId(request), getBrowserWindowId(request), rememberMe);
      writeSessionUser(session, userRecord);
    }

    return participantId;
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
    writeSessionUser(request.getSession(), su);
  }

  @Override
  public ParticipantId resume(HttpServletRequest request, String participantIdStr) {
    Preconditions.checkNotNull(request, "Request is null");

    ParticipantId participantId = null;
    try {
      participantId = ParticipantId.of(participantIdStr);
    } catch (InvalidParticipantAddress e) {
    }
    HttpSession session = request.getSession(true);
    Map<ParticipantId, SessionUser> sessionUserMap = readAllSessionUser(session);

    if (sessionUserMap.isEmpty()) {
      return null;

    } else if (participantId == null || !sessionUserMap.containsKey(participantId)) {

      long lastLoginTime = 0;
      SessionUser pickedSessionUser = null;

      for (ParticipantId p: sessionUserMap.keySet()) {
        SessionUser su = sessionUserMap.get(p);
        if (canResumeSessionUser(request, su) && lastLoginTime <= su.lastLoginTime) {
          lastLoginTime = su.lastLoginTime;
          pickedSessionUser = su;
        }
      }

      updateSessionUser(request, pickedSessionUser);
      return pickedSessionUser.participanId;

    } else if (participantId != null && sessionUserMap.containsKey(participantId)) {

      SessionUser pickedSessionUser = sessionUserMap.get(participantId);
      updateSessionUser(request, pickedSessionUser);
      return pickedSessionUser.participanId;

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
    // Don't create HTTP session if doesn't exists
    HttpSession httpSession = request.getSession(false);
    if (httpSession != null)
      return httpSession.getId();
    else
      return null;
  }

  @Override
  public Map<String, String> getSessionProperties(HttpServletRequest request) {
    ParticipantId participantId = getLoggedInUser(request);
    return readSessionUser(request.getSession(), participantId).getProperties();
  }

  @Override
  public void setSessionProperties(HttpServletRequest request, Map<String, String> properties) {
    ParticipantId participantId = getLoggedInUser(request);
    readSessionUser(request.getSession(), participantId).setProperties(properties);
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
