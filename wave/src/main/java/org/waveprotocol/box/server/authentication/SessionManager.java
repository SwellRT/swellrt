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

import java.util.Map;
import java.util.Set;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.wave.model.wave.ParticipantId;


/**
 * Utility class for managing the session's authentication status.
 * <p>
 * This version includes major modifications from original Wave version.
 * Old methods are kept until old code was removed.
 * <p><br>
 * Sessions are tracked using three components:
 * <li>A permanent session cookie, managed by Jetty container
 * <li>A transient session cookie, managed by {@see TransientSessionFilter}
 * <li>The current browsers window id, managed by {@see WindowIdFilter}
 * <br>
 * This approach allows to support following session features:
 * <li>Resuming sessions of previous logged in users
 * <li>Different user sessions per each browser window/tab
 * <li>Remember session after browser/tab is closed.
 * <li>Forgot session after browser/tab is closed.
 * <li>List all active users / sessions in the browser.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public interface SessionManager {

  // Old stuff
  static final String USER_FIELD = "user";
  static final String SIGN_IN_URL = "/auth/signin";

  // New stuff
  public final static String SESSION_URL_PARAM = "sid";
  public final static String SESSION_COOKIE_NAME = "WSESSIONID";
  public final static String TRASIENT_SESSION_COOKIE_NAME = "TSESSIONID";


  /**
   * Get the participant id of the currently logged in user from the user's HTTP
   * session.
   *
   * If the session is null, or if the user is not logged in, this function
   * returns null.
   *
   * @param session The user's HTTP session, usually obtained from
   *        request.getSession(false);
   * @return the user's participant id, or null if the user is not logged in.
   */
  @Deprecated
  ParticipantId getLoggedInUser(HttpSession session);

  /**
   * Bind the user's participant id to the user's session.
   *
   * This records that a user has been logged in.
   *
   * @param session The user's HTTP session, usually obtained from
   *        request.getSession(true);
   * @param id the user who has been logged in
   */
  @Deprecated
  void login(HttpSession session, ParticipantId id);

  /**
   * Log the user out.
   *
   * If session is null, this function has no effect.
   *
   * @param session The user's HTTP session, obtainable from
   *        request.getSession(false);
   */
  @Deprecated
  boolean logout(HttpSession session);

  /**
   * Get account data of the currently logged in user.
   *
   * @param current HTTP session
   * @return the user's account data, or null if the user is not logged in.
   */
  @Deprecated
  AccountData getLoggedInAccount(HttpSession session);


  /**
   * Get account data of the currently logged in user.
   *
   * @param request
   * @return
   */
  AccountData getLoggedInAccount(HttpServletRequest request);

  /**
   * Get account data of the participant
   *
   * @param the participant
   * @return
   */
  AccountData getAccountData(ParticipantId participantId);

  /**
   * Get the relative URL to redirect the user to the login page.
   *
   * @param redirect a url path to redirect the user back to once they have
   *        logged in, or null if the user should not be redirected after
   *        authenticating.
   * @return a url containing the login page.
   */
  String getLoginUrl(String redirect);

  /**
   * A convenience method to extract the logged in participant from a token
   * compound of "session ID:transient session ID:browser window ID"
   *
   * @param request
   * @return
   */
  ParticipantId getLoggedInUser(String token);

  /**
   * A convenience method to extract the logged in participant from the request
   * in only one step.
   *
   * @param request
   * @return
   */
  ParticipantId getLoggedInUser(HttpServletRequest request);


  /**
   * Get all the participants logged in with the same HTTP session.
   *
   * @param session the HTTP session object
   * @return a set of participants, maybe empty, but never null.
   */
  Set<ParticipantId> listLoggedInUsers(HttpServletRequest request);


  //
  // New logic
  //

  /**
   * Associates a participant with the current session.
   * A session is identified by the tuple (http session, transient session, window session).
   *
   * @param request the request
   * @param participantId the participant id
   * @param rememberMe Allow resume of the participant session even after browser is closed
   * @return the actual ParticipantId logged in, relevant for anonymous logins.
   */
  public ParticipantId login(HttpServletRequest request, ParticipantId participantId,
      boolean rememberMe);

  /**
   * Resume an user session, provided as argument or the last active session
   * otherwise
   * <p>
   * Trying to resume an existing user session within the HTTP session is always
   * better than rejecting the request.
   *
   * @param request
   * @param participantId
   * @return
   */
  public ParticipantId resume(HttpServletRequest request, String participantId);

  /**
   * Remove association of a participant with the current session.
   * A session is identified by the tuple (http session, transient session, window session).
   *
   * @param request
   * @param participantId
   * @return
   */
  public boolean logout(HttpServletRequest request, ParticipantId participantId);

  /**
   * Get the transient session cookie. Transient session only lives until browser is closed.
   * @param request
   * @return
   */
  public Cookie getTransientSessionCookie(HttpServletRequest request);

  /**
   * Get the session cookie
   * @param request
   * @return
   */
  public Cookie getSessionCookie(HttpServletRequest request);

  /**
   * Get the transient session id. Transient session only lives until browser is closed.
   * @param request
   * @return
   */
  public String getTransientSessionId(HttpServletRequest request);

  /**
   * Get the permanent session id.This session lives even after browser is closed.
   * @param request
   * @return
   */
  public String getSessionId(HttpServletRequest request);

  /**
   * Get map of properties stored in the logged in user's session
   * @param request
   * @return
   */
  public Map<String, String> getSessionProperties(HttpServletRequest request);

  /**
   * Store map of properties in the logged in user's session. This methods overwrite previous map.
   * @param request
   * @param properties
   */
  public void setSessionProperties(HttpServletRequest request, Map<String, String> properties);

}
