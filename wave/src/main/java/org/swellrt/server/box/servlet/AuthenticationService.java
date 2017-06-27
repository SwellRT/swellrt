package org.swellrt.server.box.servlet;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.Set;

import javax.inject.Singleton;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.waveprotocol.box.server.authentication.HttpRequestBasedCallbackHandler;
import org.waveprotocol.box.server.authentication.ParticipantPrincipal;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.util.RegistrationUtil;
import org.waveprotocol.wave.model.id.WaveIdentifiers;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import com.google.common.base.Preconditions;
import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import com.typesafe.config.Config;

/**
 * A servlet for authenticating a user's password and giving them a token via a
 * cookie.
 *
 * Login
 *
 * POST /auth { id : <ParticipantId>, password : <Password>, remember : <boolean> (optional) }
 *
 * Login (Anonymous)
 *
 * POST /auth { id : "_anonymous_", password : "_anonymous_" }
 *
 * Resume
 *
 * POST /auth/{participantId}
 *
 * Listing existing users in session
 *
 * GET /auth { }
 *
 * Close session
 *
 * DELETE /auth/{participantId}
 *
 * Original code taken from {@AuthenticationServlet}.
 *
 * @author Pablo Ojanguren (pablojan@gmail.com)
 *
 */
@Singleton
public class AuthenticationService extends BaseService {



  public static class AuthenticationServiceData extends ServiceData {

    public String id;
    public String password;
    public String status;
    public boolean remember;
    public int index;

    public AuthenticationServiceData() {

    }

    public AuthenticationServiceData(String status) {
      this.status = status;
    }

  }

  // The Object ID of the PKCS #9 email address stored in the client
  // certificate.
  // Source:
  // http://www.rsa.com/products/bsafe/documentation/sslc251html/group__AD__COMMON__OIDS.html
  private static final String OID_EMAIL = "1.2.840.113549.1.9.1";

  private static final Log LOG = Log.get(AuthenticationService.class);

  private final AccountStore accountStore;
  private final Configuration configuration;
  private final String domain;
  private final boolean isClientAuthEnabled;
  private final String clientAuthCertDomain;


  private boolean failedClientAuth;

  @Inject
  public AuthenticationService(AccountStore accountStore, Configuration configuration,
      SessionManager sessionManager, Config config) {

    super(sessionManager);
    this.accountStore = accountStore;
    this.configuration = configuration;
    this.domain = config.getString("core.wave_server_domain");
    this.isClientAuthEnabled = config.getBoolean("security.enable_clientauth");
    this.clientAuthCertDomain = config.getString("security.clientauth_cert_domain").toLowerCase();

  }

  @Override
  public void execute(HttpServletRequest request, HttpServletResponse response) throws IOException {

    try {

      if (request.getMethod().equals("POST"))
        doPost(request, response);
      else if (request.getMethod().equals("GET"))
        doGet(request, response);
      else if (request.getMethod().equals("DELETE"))
        doDelete(request, response);

    } catch (PersistenceException e) {
      sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          RC_INTERNAL_SERVER_ERROR);
      LOG.warning(e.getMessage(), e);
    }

  }


  private LoginContext login(String address, String password) throws IOException, LoginException {
    Subject subject = new Subject();

    CallbackHandler callbackHandler = new HttpRequestBasedCallbackHandler(address, password);

    LoginContext context = new LoginContext("Wave", subject, callbackHandler, configuration);

    // If authentication fails, login() will throw a LoginException.
    context.login();
    return context;

  }

  /**
   * The POST request should have all the fields required for authentication.
   *
   * @throws PersistenceException
   */
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException,
      PersistenceException {
    req.setCharacterEncoding("UTF-8");
    LoginContext context = null;
    Subject subject;
    ParticipantId loggedInAddress = null;

    if (isClientAuthEnabled) {
      boolean skipClientAuth = false;
      try {
        X509Certificate[] certs =
            (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");

        if (certs == null) {
            failedClientAuth = true;
            skipClientAuth = true;
        }

        if (!skipClientAuth) {
          failedClientAuth = false;
          subject = new Subject();
          for (X509Certificate cert : certs) {
            X500Principal principal = cert.getSubjectX500Principal();
            subject.getPrincipals().add(principal);
          }
          loggedInAddress = getLoggedInUser(subject);
          doLogin(req, resp, loggedInAddress, false);
          return;
        }
      } catch (InvalidParticipantAddress e1) {
        sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, RC_INVALID_ACCOUNT_ID_SYNTAX);
        return;
      }
    }

    AuthenticationServiceData authData = new AuthenticationServiceData();

    try {
      authData = getRequestServiceData(req);
    } catch (JsonParseException e) {
      sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_JSON_SYNTAX);
      return;
    }


    if (authData != null && authData.has("id") && authData.id != null) {

      if (!ParticipantId.isAnonymousName(authData.id)) {

        try {

          String password = (authData.has("password")  && authData.password != null ? authData.password : "");
          context = login(authData.id, password);
          subject = context.getSubject();
          loggedInAddress = getLoggedInUser(subject);
          doLogin(req, resp, loggedInAddress, authData.has("remember") ?  authData.remember : false);

        } catch (LoginException e) {

          sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, RC_LOGIN_FAILED);
          return;

        } catch (InvalidParticipantAddress e1) {

          sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, RC_INVALID_ACCOUNT_ID_SYNTAX);
          return;

        }

      } else {
        doLogin(req, resp, ParticipantId.anonymousOfUnsafe(domain), false);

      }

    } else {
      doResume(req, resp);

    }
  }

  protected AccountService.AccountServiceData getAccountData(HttpServletRequest req, ParticipantId participantId) throws PersistenceException {
    AccountService.AccountServiceData accountData;

    if (!participantId.isAnonymous())
      accountData =
          AccountService.toServiceData(ServiceUtils.getUrlBuilder(req),
              accountStore.getAccount(participantId).asHuman());
    else
      accountData = new AccountService.AccountServiceData(participantId.getAddress());

    accountData.sessionId = sessionManager.getSessionId(req);
    accountData.transientSessionId = sessionManager.getTransientSessionId(req);
    accountData.domain = domain;

    return accountData;
  }

  protected void doLogin(HttpServletRequest req, HttpServletResponse resp, ParticipantId participantId, boolean keepLogin) throws IOException, PersistenceException {
    if (participantId.isAnonymous()) {
      participantId = ParticipantId.anonymousOfUnsafe(sessionManager.getSessionId(req), domain);
      keepLogin = false;
    }
    sessionManager.login(req, participantId, keepLogin);
    LOG.info("Authenticated user " + participantId);
    sendResponse(resp, getAccountData(req, participantId));
  }

  protected void doResume(HttpServletRequest req, HttpServletResponse resp)
      throws IOException, PersistenceException {

    String[] pathTokens = SwellRtServlet.getCleanPathInfo(req).split("/");
    String participantIdStr = pathTokens.length > 2 ? pathTokens[2] : null;

    ParticipantId participantId = sessionManager.resume(req, participantIdStr);
    if (participantId == null) {
      sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, RC_LOGIN_FAILED);
    } else {
      LOG.info("Authenticated user " + participantId);
      sendResponse(resp, getAccountData(req, participantId));
    }
  }


  /**
   * DELETE a session
   *
   * @param req
   * @param resp
   * @throws IOException
   */
  protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {

    String[] pathTokens = SwellRtServlet.getCleanPathInfo(req).split("/");
    String participantToken =  pathTokens.length > 2 ? pathTokens[2] : null;

    boolean wasDelete = false;

    if (participantToken != null && !participantToken.isEmpty()) {
      ParticipantId participantId;
      try {
        participantId = ParticipantId.of(participantToken);
      } catch (InvalidParticipantAddress e) {
        sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_ACCOUNT_ID_SYNTAX);
        return;
      }
      wasDelete = sessionManager.logout(req, participantId);
    }

    if (wasDelete) {
      sendResponse(resp, new AuthenticationServiceData("SESSION_CLOSED"));
      return;
    } else {
      sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, RC_ACCOUNT_NOT_LOGGED_IN);
      return;
    }

}

  /**
   * Get the participant id of the given subject.
   *
   * The subject is searched for compatible principals. When other
   * authentication types are added, this method will need to be updated to
   * support their principal types.
   *
   * @throws InvalidParticipantAddress The subject's address is invalid
   */
  private ParticipantId getLoggedInUser(Subject subject) throws InvalidParticipantAddress {
    String address = null;

    for (Principal p : subject.getPrincipals()) {
      // TODO(josephg): When we support other authentication types (LDAP, etc),
      // this method will need to read the address portion out of the other
      // principal types.
      if (p instanceof ParticipantPrincipal) {
        address = ((ParticipantPrincipal) p).getName();
        break;
      } else if (p instanceof X500Principal) {
        return attemptClientCertificateLogin((X500Principal) p);
      }
    }

    return address == null ? null : ParticipantId.of(address);
  }

  /**
   * Attempts to authenticate the user using their client certificate.
   *
   * Retrieves the email from their certificate, using it as the wave username.
   * If the user doesn't exist and registration is enabled, it will
   * automatically create an account before continuing. Otherwise it will simply
   * check if the account exists and authenticate based on that.
   *
   * @throws RuntimeException The encoding of the email is unsupported on this
   *         system
   * @throws InvalidParticipantAddress The email address doesn't correspond to
   *         an account
   */
  private ParticipantId attemptClientCertificateLogin(X500Principal p) throws RuntimeException,
      InvalidParticipantAddress {
    String distinguishedName = p.getName();
    try {
      LdapName ldapName = new LdapName(distinguishedName);
      for (Rdn rdn : ldapName.getRdns()) {
        if (rdn.getType().equals(OID_EMAIL)) {
          String email = decodeEmailFromCertificate((byte[]) rdn.getValue());
          if (email.endsWith("@" + clientAuthCertDomain)) {
            // Check we decoded the string correctly.
            Preconditions.checkState(WaveIdentifiers.isValidIdentifier(email),
                "The decoded email is not a valid wave identifier");
            ParticipantId id = ParticipantId.of(email);
            if (!RegistrationUtil.doesAccountExist(accountStore, id)) {
//              if (!isRegistrationDisabled) {
//                if (!RegistrationUtil.createAccountIfMissing(accountStore, id, null, welcomeBot)) {
//                  return null;
//                }
//              } else {
//                throw new InvalidNameException(
//                    "User doesn't already exist, and registration disabled by administrator");
//              }

              throw new InvalidNameException(
                  "User doesn't already exist");
            }
            return id;
          }
        }
      }
    } catch (UnsupportedEncodingException ex) {
      throw new RuntimeException(ex);
    } catch (InvalidNameException ex) {
      throw new InvalidParticipantAddress(distinguishedName,
          "Certificate does not contain a valid distinguished name");
    }
    return null;
  }

  /**
   * Decodes the user email from the X.509 certificate.
   *
   * Email address is assumed to be valid in ASCII, and less than 128 characters
   * long
   *
   * @param encoded Output from rdn.getValue(). 1st byte is the tag, second is
   *        the length.
   * @return The decoded email in ASCII
   * @throws UnsupportedEncodingException The email address wasn't in ASCII
   */
  private String decodeEmailFromCertificate(byte[] encoded) throws UnsupportedEncodingException {
    // Check for < 130, since first 2 bytes are taken up as stated above.
    Preconditions.checkState(encoded.length < 130, "The email address is longer than expected");
    return new String(encoded, 2, encoded.length - 2, "ascii");
  }

  /**
   * GET, return a list of active users within the http session
   *
   * @throws PersistenceException
   */
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException,
      PersistenceException {
    Set<ParticipantId> sessions = sessionManager.listLoggedInUsers(req);
    String[] sessionsArray = new String[sessions.size()];
    Iterator<ParticipantId> it = sessions.iterator();
    for (int i = 0; i < sessions.size(); i++) {
      sessionsArray[i] = it.next().getAddress();
    }
    sendResponse(resp, sessionsArray);
  }

  protected AuthenticationServiceData getRequestServiceData(HttpServletRequest request)
      throws IOException, JsonParseException {

    StringWriter writer = new StringWriter();
    IOUtils.copy(request.getInputStream(), writer, Charset.forName("UTF-8"));

    String json = writer.toString();

    if (json == null)
       throw new JsonParseException("Null JSON message");

    return (AuthenticationServiceData) ServiceData.fromJson(json,
        AuthenticationServiceData.class);

  }


}
