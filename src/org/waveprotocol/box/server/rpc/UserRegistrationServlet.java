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

import com.google.gxp.base.GxpContext;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.HttpRequestBasedCallbackHandler;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.gxp.UserRegistrationPage;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.agent.welcome.WelcomeRobot;
import org.waveprotocol.box.server.util.RegistrationUtil;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.util.Locale;

import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * The user registration servlet allows new users to register accounts.
 *
 * @author josephg@gmail.com (Joseph Gentle)
 */
@SuppressWarnings("serial")
@Singleton
public final class UserRegistrationServlet extends HttpServlet {

  private final AccountStore accountStore;
  private final String domain;
  private final WelcomeRobot welcomeBot;
  private final boolean registrationDisabled;
  private final String analyticsAccount;

  private final Log LOG = Log.get(UserRegistrationServlet.class);

  @Inject
  public UserRegistrationServlet(AccountStore accountStore,
      @Named(CoreSettings.WAVE_SERVER_DOMAIN) String domain, WelcomeRobot welcomeBot,
      @Named(CoreSettings.DISABLE_REGISTRATION) boolean registrationDisabled,
      @Named(CoreSettings.ANALYTICS_ACCOUNT) String analyticsAccount) {
    this.accountStore = accountStore;
    this.domain = domain;
    this.welcomeBot = welcomeBot;
    this.registrationDisabled = registrationDisabled;
    this.analyticsAccount = analyticsAccount;
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    writeRegistrationPage("", AuthenticationServlet.RESPONSE_STATUS_NONE, req.getLocale(), resp);
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    req.setCharacterEncoding("UTF-8");

    String message = null;
    String responseType;
    if (!registrationDisabled) {
    message = tryCreateUser(req.getParameter(HttpRequestBasedCallbackHandler.ADDRESS_FIELD),
                  req.getParameter(HttpRequestBasedCallbackHandler.PASSWORD_FIELD));
    }

    if (message != null || registrationDisabled) {
      resp.setStatus(HttpServletResponse.SC_FORBIDDEN);
      responseType = AuthenticationServlet.RESPONSE_STATUS_FAILED;
    } else {
      message = "Registration complete.";
      resp.setStatus(HttpServletResponse.SC_OK);
      responseType = AuthenticationServlet.RESPONSE_STATUS_SUCCESS;
    }

    writeRegistrationPage(message, responseType, req.getLocale(), resp);
  }

  /**
   * Try to create a user with the provided username and password. On error,
   * returns a string containing an error message. On success, returns null.
   */
  private String tryCreateUser(String username, String password) {
    ParticipantId id = null;

    try {
      // First, some cleanup on the parameters.
      if (username == null) {
        return "Username portion of address cannot be empty";
      }
      username = username.trim().toLowerCase();
      if (username.contains(ParticipantId.DOMAIN_PREFIX)) {
        id = ParticipantId.of(username);
      } else {
        id = ParticipantId.of(username + ParticipantId.DOMAIN_PREFIX + domain);
      }
      if (id.getAddress().indexOf("@") < 1) {
        return "Username portion of address cannot be empty";
      }
      String[] usernameSplit = id.getAddress().split("@");
      if (usernameSplit.length != 2 || !usernameSplit[0].matches("[\\w\\.]+")) {
        return "Only letters (a-z), numbers (0-9), and periods (.) are allowed in Username";
      }
      if (!id.getDomain().equals(domain)) {
        return "You can only create users at the " + domain + " domain";
      }
    } catch (InvalidParticipantAddress e) {
      return "Invalid username";
    }

    if(RegistrationUtil.doesAccountExist(accountStore, id)) {
        return "Account already exists";
    }

    if (password == null) {
      // Register the user with an empty password.
      password = "";
    }

    if (!RegistrationUtil.createAccountIfMissing(accountStore, id,
          new PasswordDigest(password.toCharArray()), welcomeBot)) {
      return "An unexpected error occured while trying to create the account";
    }

    return null;
  }

  private void writeRegistrationPage(String message, String responseType, Locale locale,
      HttpServletResponse dest) throws IOException {
    dest.setCharacterEncoding("UTF-8");
    dest.setContentType("text/html;charset=utf-8");
    UserRegistrationPage.write(dest.getWriter(), new GxpContext(locale), domain, message,
        responseType, registrationDisabled, analyticsAccount);
  }
}
