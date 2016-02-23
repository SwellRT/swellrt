package org.swellrt.server.box.servlet;

import com.google.gson.JsonParseException;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.io.IOUtils;
import org.apache.commons.validator.routines.EmailValidator;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountAttachmentStore;
import org.waveprotocol.box.server.persistence.AccountAttachmentStore.Attachment;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.AttachmentUtil;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Calendar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Service for creating and editing accounts.
 * To retrie multiple public accounts data see {@}
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 * Create new account
 *
 * POST /account { id : <ParticipantId>, password : <String>, ... }
 *
 *
 * Edit accound profile (empty values are deleted)
 *
 * POST /account/{ParticipantId.name} { ... }
 *

 * Get account profile
 *
 * GET /account/{ParticipantId.name}
 */
public class AccountService extends SwellRTService {


  public static class AccountServiceData extends ServiceData {

    public String id;
    public String name; // For future use
    public String password;
    public String email;
    public String avatarData;
    public String avatarUrl;
    public String locale;
    public String sessionId;
    public String domain;

    public AccountServiceData() {

    }

    public AccountServiceData(String id) {
      this.id = id;
    }

  }



  private static final Log LOG = Log.get(AccountService.class);

  private final SessionManager sessionManager;

  private final AccountStore accountStore;

  private final AccountAttachmentStore attachmentAccountStore;

  private final String domain;

  @Inject
  public AccountService(SessionManager sessionManager, AccountStore accountStore,
      AccountAttachmentStore attachmentAccountStore,
      @Named(CoreSettings.WAVE_SERVER_DOMAIN) String domain) {

    this.sessionManager = sessionManager;
    this.accountStore = accountStore;
    this.attachmentAccountStore = attachmentAccountStore;
    this.domain = domain;
  }


  @Override
  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {

    String[] pathTokens = SwellRtServlet.getCleanPathInfo(req).split("/");
    String participantToken = pathTokens.length > 2 ? pathTokens[2] : null;
    String opToken = pathTokens.length > 3 ? pathTokens[3] : null;
    String paramToken = pathTokens.length > 4 ? pathTokens[4] : null;

    ParticipantId loggedInUser = sessionManager.getLoggedInUser(req.getSession(false));

    if (req.getMethod().equals("POST") && participantToken == null) {

      // POST /account create user's profile

      try {

        AccountServiceData userData = getRequestServiceData(req);

        ParticipantId participantId = ParticipantId.of(userData.id + "@" + domain);

        AccountData accountData = accountStore.getAccount(participantId);


        if (accountData != null) {
          sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, RC_ACCOUNT_ALREADY_EXISTS);
          return;
        }

        if (userData.password == null) {
          userData.password = "";
        }

        HumanAccountDataImpl account =
            new HumanAccountDataImpl(participantId, new PasswordDigest(
                userData.password.toCharArray()));


        if (userData.email != null) {
          if (!EmailValidator.getInstance().isValid(userData.email)) {
            sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST,
                RC_INVALID_EMAIL_ADDRESS);
            return;
          }

          account.setEmail(userData.email);
        }

        if (userData.locale != null) account.setLocale(userData.locale);

        if (userData.avatarData != null) {
            // Store avatar
            String avatarFileId = storeAvatar(participantId, userData.avatarData, null);
            account.setAvatarFileId(avatarFileId);
        }


        accountStore.putAccount(account);


        sendResponse(response, toServiceData(getBaseUrl(req), account));
        return;


      } catch (IOException e) {

        sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            RC_INTERNAL_SERVER_ERROR);
        LOG.warning(e.getMessage(), e);
        return;

      } catch (PersistenceException e) {

        sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            RC_INTERNAL_SERVER_ERROR);
        LOG.warning(e.getMessage(), e);
        return;

      } catch (InvalidParticipantAddress e) {
        sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST,
            RC_INVALID_ACCOUNT_ID_SYNTAX);
        return;
      } catch (JsonParseException e) {
        sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_JSON_SYNTAX);
        return;
      }

    } else if (req.getMethod().equals("POST") && participantToken != null) {

      // POST /account/joe update user's account

      try {

        ParticipantId participantId = ParticipantId.of(participantToken + "@" + domain);
        AccountData accountData = accountStore.getAccount(participantId);

        if (accountData == null) {
          sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, RC_ACCOUNT_NOT_FOUND);
          return;
        }

        // if the account exists, only the user can modify the profile
        if (!participantId.equals(loggedInUser)) {
          sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, RC_ACCOUNT_NOT_LOGGED_IN);
          return;
        }

        // Modify

        AccountServiceData userData = getRequestServiceData(req);

        HumanAccountData account = accountData.asHuman();


        if (userData.isParsedField("email")) {
          try {
            if (userData.email.isEmpty())
              account.setEmail(null);
            else
              account.setEmail(userData.email);
          } catch (IllegalArgumentException e) {
            sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST,
                RC_INVALID_EMAIL_ADDRESS);
            return;
          }
        }

        if (userData.isParsedField("locale"))
          account.setLocale(userData.locale);


        if (userData.isParsedField("avatarData")) {
          if (userData.avatarData == null || userData.avatarData.isEmpty()
              || "data:".equals(userData.avatarData)) {
            // Delete avatar
            deleteAvatar(account.getAvatarFileId());
            account.setAvatarFileId(null);
          } else {
            String avatarFileId =
                storeAvatar(participantId, userData.avatarData, account.getAvatarFileName());
            account.setAvatarFileId(avatarFileId);
          }
        }

        accountStore.putAccount(account);


        sendResponse(response, toServiceData(req.getRequestURL().toString(), account));
        return;


      } catch (PersistenceException e) {

        sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            RC_INTERNAL_SERVER_ERROR);
        LOG.warning(e.getMessage(), e);
        return;

      } catch (IOException e) {

        sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            RC_INTERNAL_SERVER_ERROR);
        LOG.warning(e.getMessage(), e);
        return;

      } catch (InvalidParticipantAddress e) {

        sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST,
            RC_INVALID_ACCOUNT_ID_SYNTAX);
        return;

      } catch (JsonParseException e) {
        sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_JSON_SYNTAX);
        return;
      }


    } else if (req.getMethod().equals("GET")) {

      try {

        ParticipantId participantId = ParticipantId.of(participantToken + "@" + domain);
        AccountData accountData = accountStore.getAccount(participantId);

        if (accountData == null) {
          sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, RC_ACCOUNT_NOT_FOUND);
          return;
        }

        if (opToken != null && opToken.equalsIgnoreCase("avatar")) {

          // GET /account/joe/avatar/[filename]

          String fileName = paramToken;

          if (fileName == null || !accountData.asHuman().getAvatarFileName().equals(fileName)) {
            sendResponseError(response, HttpServletResponse.SC_NOT_FOUND,
                "ACCOUNT_ATTACHMENT_NOT_FOUND");
            return;
          }

          Attachment avatar = attachmentAccountStore.getAvatar(fileName);

          response.setContentType(accountData.asHuman().getAvatarMimeType());
          response.setContentLength((int) avatar.getSize());
          response.setStatus(HttpServletResponse.SC_OK);
          response.setDateHeader("Last-Modified", Calendar.getInstance().getTimeInMillis());
          AttachmentUtil.writeTo(avatar.getInputStream(), response.getOutputStream());

          return;

        }


        if (!participantId.equals(loggedInUser)) {
          sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, RC_ACCOUNT_NOT_LOGGED_IN);
          return;
        }


        // GET /account/joe retrieve user's account data
        sendResponse(response, toServiceData(req.getRequestURL().toString(), accountData.asHuman()));
        return;

      } catch (PersistenceException e) {

        sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            RC_INTERNAL_SERVER_ERROR);
        LOG.warning(e.getMessage(), e);
        return;

      } catch (InvalidParticipantAddress e) {
        sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST,
            RC_INVALID_ACCOUNT_ID_SYNTAX);
        return;
      }


    }



  }


  protected String storeAvatar(ParticipantId participantId, String avatarData,
      String currentAvatarFileName) throws IOException {

    if (!avatarData.startsWith("data:")) {
      throw new IOException("Avatar data syntax is not a valida RFC 2397 data URI");
    }

    // Store avatar first and get the storage's file name
    int dataUriSeparatorIndex = avatarData.indexOf(";");
    String mimeType = avatarData.substring("data:".length(), dataUriSeparatorIndex);
    // Remove the base64, prefix
    String base64Data = avatarData.substring(dataUriSeparatorIndex + 8, avatarData.length());

    return attachmentAccountStore.storeAvatar(participantId, mimeType, base64Data,
        currentAvatarFileName);

  }

  protected void deleteAvatar(String avatarFileId) {


  }

  protected AccountServiceData getRequestServiceData(HttpServletRequest request)
      throws JsonParseException, IOException {
    StringWriter writer = new StringWriter();
    IOUtils.copy(request.getInputStream(), writer, Charset.forName("UTF-8"));

    String json = writer.toString();

    if (json == null) throw new JsonParseException("Null JSON message");

    return (AccountServiceData) ServiceData.fromJson(json, AccountServiceData.class);
  }






  protected static String getAvatarUrl(String serverBaseUrl, HumanAccountData account) {
    if (account.getAvatarFileName() == null) return null;

    return serverBaseUrl + SwellRtServlet.SERVLET_CONTEXT + "/account/" + account.getId().getName()
        + "/avatar/"
        + account.getAvatarFileName();
  }

  protected static AccountServiceData toServiceData(String serverBaseUrl, HumanAccountData account) {

    AccountServiceData data = new AccountServiceData();

    data.id = account.getId().getAddress();
    data.email = account.getEmail() == null ? "" : account.getEmail();
    String avatarUrl = getAvatarUrl(serverBaseUrl, account);
    data.avatarUrl = avatarUrl == null ? "" : avatarUrl;
    data.locale = account.getLocale() == null ? "" : account.getLocale();

    return data;

  }


}
