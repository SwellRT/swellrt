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

package org.waveprotocol.box.server.rpc;

import com.google.common.collect.Maps;
import com.google.gxp.base.GxpContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.waveprotocol.box.common.SessionConstants;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.gxp.TopBar;
import org.waveprotocol.box.server.gxp.WaveClientPage;
import org.waveprotocol.box.server.util.RandomBase64Generator;
import org.waveprotocol.wave.client.util.ClientFlagsBase;
import org.waveprotocol.wave.common.bootstrap.FlagConstants;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * The HTTP servlet for serving a wave client along with content generated on
 * the server.
 *
 * @author kalman@google.com (Benjamin Kalman)
 */
@SuppressWarnings("serial")
@Singleton
public class WaveClientServlet extends HttpServlet {

  private static final Log LOG = Log.get(WaveClientServlet.class);

  private static final HashMap<String, String> FLAG_MAP = Maps.newHashMap();
  static {
    // __NAME_MAPPING__ is a map of name to obfuscated id
    for (int i = 0; i < FlagConstants.__NAME_MAPPING__.length; i += 2) {
      FLAG_MAP.put(FlagConstants.__NAME_MAPPING__[i], FlagConstants.__NAME_MAPPING__[i + 1]);
    }
  }

  private final String domain;
  private final String analyticsAccount;
  private final SessionManager sessionManager;
  private final String websocketAddress;
  private final String websocketPresentedAddress;

  /**
   * Creates a servlet for the wave client.
   */
  @Inject
  public WaveClientServlet(
      @Named(CoreSettings.WAVE_SERVER_DOMAIN) String domain,
      @Named(CoreSettings.HTTP_FRONTEND_ADDRESSES) List<String> httpAddresses,
      @Named(CoreSettings.HTTP_WEBSOCKET_PUBLIC_ADDRESS) String websocketAddress,
      @Named(CoreSettings.HTTP_WEBSOCKET_PRESENTED_ADDRESS) String websocketPresentedAddress,
      @Named(CoreSettings.ANALYTICS_ACCOUNT) String analyticsAccount,
      SessionManager sessionManager) {
    this.domain = domain;
    this.websocketAddress = StringUtils.isEmpty(websocketAddress) ?
        httpAddresses.get(0) : websocketAddress;
    this.websocketPresentedAddress = StringUtils.isEmpty(websocketPresentedAddress) ?
        this.websocketAddress : websocketPresentedAddress;
    this.analyticsAccount = analyticsAccount;
    this.sessionManager = sessionManager;
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws IOException {
    ParticipantId id = sessionManager.getLoggedInUser(request.getSession(false));

    // Eventually, it would be nice to show users who aren't logged in the public waves.
    // However, public waves aren't implemented yet. For now, we'll just redirect users
    // who haven't signed in to the sign in page.
    if (id == null) {
      response.sendRedirect(sessionManager.getLoginUrl("/"));
      return;
    }

    String username = null;
    String userDomain = null;
    if (id != null) {
      String[] parts = id.getAddress().split("@");
      username = parts[0];
      userDomain = id.getDomain();
    }

    try {
      WaveClientPage.write(response.getWriter(), new GxpContext(request.getLocale()),
          getSessionJson(request.getSession(false)), getClientFlags(request), websocketPresentedAddress,
          TopBar.getGxpClosure(username, userDomain), analyticsAccount);
    } catch (IOException e) {
      LOG.warning("Failed to write GXP for request " + request, e);
      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      return;
    }

    response.setContentType("text/html");
    response.setStatus(HttpServletResponse.SC_OK);
  }

  private JSONObject getClientFlags(HttpServletRequest request) {
    try {
      JSONObject ret = new JSONObject();

      Enumeration<?> iter = request.getParameterNames();
      while (iter.hasMoreElements()) {
        String name = (String) iter.nextElement();
        String value = request.getParameter(name);

        if (FLAG_MAP.containsKey(name)) {
          // Set using the correct type of data in the json using reflection
          try {
            Method getter = ClientFlagsBase.class.getMethod(name);
            Class<?> retType = getter.getReturnType();

            if (retType.equals(String.class)) {
              ret.put(FLAG_MAP.get(name), value);
            } else if (retType.equals(Integer.class)) {
              ret.put(FLAG_MAP.get(name), Integer.parseInt(value));
            } else if (retType.equals(Boolean.class)) {
              ret.put(FLAG_MAP.get(name), Boolean.parseBoolean(value));
            } else if (retType.equals(Float.class)) {
              ret.put(FLAG_MAP.get(name), Float.parseFloat(value));
            } else if (retType.equals(Double.class)) {
              ret.put(FLAG_MAP.get(name), Double.parseDouble(value));
            } else {
              // Flag exists, but its type is unknown, so it can not be
              // properly encoded in JSON.
              LOG.warning("Ignoring flag [" + name + "] with unknown return type: " + retType);
            }

            // Ignore the flag on any exception
          } catch (SecurityException ex) {
          } catch (NoSuchMethodException ex) {
            LOG.warning("Failed to find the flag [" + name + "] in ClientFlagsBase.");
          } catch (NumberFormatException ex) {
          }
        }
      }

      return ret;
    } catch (JSONException ex) {
      LOG.severe("Failed to create flags JSON");
      return new JSONObject();
    }
  }

  private JSONObject getSessionJson(HttpSession session) {
    try {
      ParticipantId user = sessionManager.getLoggedInUser(session);
      String address = (user != null) ? user.getAddress() : null;

      // TODO(zdwang): Figure out a proper session id rather than generating a
      // random number
      String sessionId = (new RandomBase64Generator()).next(10);

      return new JSONObject()
          .put(SessionConstants.DOMAIN, domain)
          .putOpt(SessionConstants.ADDRESS, address)
          .putOpt(SessionConstants.ID_SEED, sessionId);
    } catch (JSONException e) {
      LOG.severe("Failed to create session JSON");
      return new JSONObject();
    }
  }
}
