package org.swellrt.server.box.servlet;

import com.google.common.base.Preconditions;
import com.google.gson.JsonSyntaxException;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.io.IOUtils;
import org.waveprotocol.box.server.CoreSettings;
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

import java.io.IOException;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.Principal;
import java.security.cert.X509Certificate;

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
import javax.servlet.http.HttpSession;

/**
 * A servlet for authenticating a user's password and giving them a token via a
 * cookie.
 *
 * Login
 *
 * POST /auth { id : <ParticipantId> password : <Password> }
 *
 * Login (Anonymous)
 *
 * POST /auth { id : "_anonymous_" password : "_anonymous_" }
 *
 * Resume existing session
 *
 * GET /auth { }
 *
 * Close session
 *
 * POST /auth { }
 *
 * Original code taken from {@AuthenticationServlet}.
 *
 * @author Pablo Ojanguren (pablojan@gmail.com)
 *
 */
@Singleton
public class AuthenticationService extends SwellRTService {



  public static class AuthenticationServiceData extends ServiceData {

    public String id;
    public String password;
    public String status;
    public String sessionId;

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
  private final SessionManager sessionManager;
  private final String domain;
  private final boolean isClientAuthEnabled;
  private final String clientAuthCertDomain;


  private boolean failedClientAuth;

  @Inject
  public AuthenticationService(AccountStore accountStore, Configuration configuration,
      SessionManager sessionManager, @Named(CoreSettings.WAVE_SERVER_DOMAIN) String domain,
      @Named(CoreSettings.ENABLE_CLIENTAUTH) boolean isClientAuthEnabled,
      @Named(CoreSettings.CLIENTAUTH_CERT_DOMAIN) String clientAuthCertDomain) {

    this.accountStore = accountStore;
    this.configuration = configuration;
    this.sessionManager = sessionManager;
    this.domain = domain.toLowerCase();
    this.isClientAuthEnabled = isClientAuthEnabled;
    this.clientAuthCertDomain = clientAuthCertDomain.toLowerCase();

  }

  @Override
  public void execute(HttpServletRequest request, HttpServletResponse response) throws IOException {

    try {

      if (request.getMethod().equals("POST"))
        doPost(request, response);
      else if (request.getMethod().equals("GET"))
        doGet(request, response);

    } catch (PersistenceException e) {
      sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          RC_INTERNAL_SERVER_ERROR);
      LOG.warning(e.getMessage(), e);
    }

  }


  @SuppressWarnings("unchecked")
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
        }
      } catch (InvalidParticipantAddress e1) {
        sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, RC_INVALID_ACCOUNT_ID_SYNTAX);
        return;
      }
    }

    HttpSession session = null;

    if (loggedInAddress == null) {

      AuthenticationServiceData authData = null;

      try {
        authData = getRequestServiceData(req);
      } catch (JsonSyntaxException e) {
        sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_JSON_SYNTAX);
        return;
      }


      if (authData.isParsedField("id") && authData.id != null && authData.isParsedField("password")
          && authData.password != null) {

        if (!ParticipantId.isAnonymousName(authData.id)) {

          try {

            context = login(authData.id, authData.password);
            subject = context.getSubject();
            loggedInAddress = getLoggedInUser(subject);
            session = req.getSession(true);

          } catch (LoginException e) {

            sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, RC_LOGIN_FAILED);
            return;

          } catch (InvalidParticipantAddress e1) {

            sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, RC_INVALID_ACCOUNT_ID_SYNTAX);
            return;

          }

        } else if (authData.id != null) {

          session = req.getSession(true);
          loggedInAddress = ParticipantId.anonymousOfUnsafe(session.getId(), domain);

        }


      } else if (!authData.isParsedField("id") || !authData.isParsedField("password")) {
        // Don't throw error, close the current session if it exists
      } else {
        sendResponseError(resp, HttpServletResponse.SC_BAD_REQUEST, RC_MISSING_PARAMETER);
        return;
      }


    }

    // If we have reach this point with a no login, close current session
    if (loggedInAddress == null) {

      try {
        session = req.getSession(false);
        LOG.info("Closing session " + (session != null ? session.getId() : ""));
        sessionManager.logout(session);
        if (context != null)
          context.logout();
      } catch (LoginException e) {
        // Logout failed. Absorb the error, since we're about to throw an
        // illegal state exception anyway.
      }

      sendResponse(resp, new AuthenticationServiceData("SESSION_CLOSED"));
      return;

    }

    sessionManager.setLoggedInUser(session, loggedInAddress);
    LOG.info("Authenticated user " + loggedInAddress);

    AccountService.AccountServiceData accountData;

    if (!loggedInAddress.isAnonymous())
      accountData =
          AccountService.toServiceData(req.getRequestURL().toString(),
              accountStore.getAccount(loggedInAddress).asHuman());
    else
      accountData = new AccountService.AccountServiceData(loggedInAddress.getAddress());

    accountData.sessionId = req.getSession().getId();

    sendResponse(resp, accountData);

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
   * On GET, try to resume an existing session matching the session cookie
   *
   * @throws PersistenceException
   */
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException,
      PersistenceException {
    // If the user is already logged in, we'll try to redirect them immediately.
    resp.setCharacterEncoding("UTF-8");
    req.setCharacterEncoding("UTF-8");
    HttpSession session = req.getSession(false);
    ParticipantId user = sessionManager.getLoggedInUser(session);

    if (user != null) {

      AccountService.AccountServiceData accountData;

      if (!user.isAnonymous())
        accountData =
            AccountService.toServiceData(req.getRequestURL().toString(),
                accountStore.getAccount(user).asHuman());
      else
        accountData = new AccountService.AccountServiceData(user.getAddress());

      accountData.sessionId = req.getSession().getId();

      // Resuming the session
      LOG.info("Resuming Authenticated user " + user);
      sendResponse(resp, accountData);
      return;

    } else {
      if (isClientAuthEnabled && !failedClientAuth) {
        X509Certificate[] certs =
            (X509Certificate[]) req.getAttribute("javax.servlet.request.X509Certificate");
        if (certs != null) {
          doPost(req, resp);
        }
      }

      // Login is required
      sendResponseError(resp, HttpServletResponse.SC_FORBIDDEN, RC_LOGIN_FAILED);
    }
  }

  protected AuthenticationServiceData getRequestServiceData(HttpServletRequest request)
      throws IOException, JsonSyntaxException {

    StringWriter writer = new StringWriter();
    IOUtils.copy(request.getInputStream(), writer, Charset.forName("UTF-8"));

    return (AuthenticationServiceData) ServiceData.fromJson(writer.toString(),
        AuthenticationServiceData.class);

  }


}
