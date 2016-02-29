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
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Service for creating and editing accounts.
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
 *
 * Get account profile
 *
 * GET /account/{ParticipantId.name}
 *
 *
 * Get multiple account profiles
 *
 * GET /account?p=user1@domain;user2@domain
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

  private final AccountStore accountStore;

  private final AccountAttachmentStore attachmentAccountStore;

  private final String domain;

  @Inject
  public AccountService(SessionManager sessionManager, AccountStore accountStore,
      AccountAttachmentStore attachmentAccountStore,
      @Named(CoreSettings.WAVE_SERVER_DOMAIN) String domain) {

    super(sessionManager);
    this.accountStore = accountStore;
    this.attachmentAccountStore = attachmentAccountStore;
    this.domain = domain;
  }

  protected ParticipantId getParticipantFromRequest(HttpServletRequest req)
      throws InvalidParticipantAddress {

    String[] pathTokens = SwellRtServlet.getCleanPathInfo(req).split("/");
    String participantToken = pathTokens.length > 2 ? pathTokens[2] : null;

    if (participantToken == null) throw new InvalidParticipantAddress("null", "Address is null");

    ParticipantId participantId =
        participantToken.contains("@") ? ParticipantId.of(participantToken) : ParticipantId
            .of(participantToken + "@" + domain);

    return participantId;

  }

  protected String getAvatarFileFromRequest(HttpServletRequest req)
      throws InvalidParticipantAddress {

    String[] pathTokens = SwellRtServlet.getCleanPathInfo(req).split("/");
    String avatarFileName = pathTokens.length > 4 ? pathTokens[4] : null;

    return avatarFileName;
  }

  protected Collection<ParticipantId> getParticipantsFromRequestQuery(HttpServletRequest req) {
    try {
      String query = URLDecoder.decode(req.getParameter("p"), "UTF-8");
      String[] participantAddresses = query.split(";");

      List<ParticipantId> participantIds= new ArrayList<ParticipantId>();

      for (String address : participantAddresses) {
        try {
          participantIds.add(ParticipantId.ofUnsafe(address));
        } catch (IllegalArgumentException e) {
          // Ignore
        }
      }

      return participantIds;

    } catch (Exception e) {
      return null;
    }

  }

  protected void createAccount(HttpServletRequest req, HttpServletResponse response)
      throws IOException {

    // POST /account create user's profile

    try {

      AccountServiceData userData = getRequestServiceData(req);

      if (userData.id == null) {
        sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_MISSING_PARAMETER);
        return;
      }

      ParticipantId participantId =
          userData.id.contains("@") ? ParticipantId.of(userData.id) : ParticipantId.of(userData.id
              + "@" + domain);

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
          sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_EMAIL_ADDRESS);
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
      sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_ACCOUNT_ID_SYNTAX);
      return;
    } catch (JsonParseException e) {
      sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_JSON_SYNTAX);
      return;
    }

  }


  protected void updateAccount(HttpServletRequest req, HttpServletResponse response)
      throws IOException {

    // POST /account/joe update user's account

    ParticipantId loggedInUser = sessionManager.getLoggedInUser(req.getSession(false));


    try {

      ParticipantId participantId = getParticipantFromRequest(req);

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
          sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_EMAIL_ADDRESS);
          return;
        }
      }

      if (userData.isParsedField("locale")) account.setLocale(userData.locale);


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

      sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_ACCOUNT_ID_SYNTAX);
      return;

    } catch (JsonParseException e) {
      sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_JSON_SYNTAX);
      return;
    }

  }

  protected void getAvatar(HttpServletRequest req, HttpServletResponse response) throws IOException {

    // GET /account/joe/avatar/[filename]
    try {


      ParticipantId participantId = getParticipantFromRequest(req);
      AccountData accountData = accountStore.getAccount(participantId);

      if (accountData == null) {
        sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, RC_ACCOUNT_NOT_FOUND);
        return;
      }

      String fileName = getAvatarFileFromRequest(req);

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

    } catch (PersistenceException e) {

      sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          RC_INTERNAL_SERVER_ERROR);
      LOG.warning(e.getMessage(), e);
      return;

    } catch (InvalidParticipantAddress e) {
      sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_ACCOUNT_ID_SYNTAX);
      return;
    }

  }


  protected void getParticipantAccount(HttpServletRequest req, HttpServletResponse response)
      throws IOException {

    try {

      ParticipantId participantId = getParticipantFromRequest(req);

      ParticipantId loggedInUser = sessionManager.getLoggedInUser(req.getSession(false));
      AccountData accountData = accountStore.getAccount(participantId);

      if (accountData == null) {
        sendResponseError(response, HttpServletResponse.SC_NOT_FOUND, RC_ACCOUNT_NOT_FOUND);
      }

      if (!participantId.equals(loggedInUser)) {

        // GET /account/joe retrieve user's account data only public fields


        sendResponse(response,
            toPublicServiceData(req.getRequestURL().toString(), accountData.asHuman()));
        return;

      } else {


        // GET /account/joe retrieve user's account data including private
        // fields

        sendResponse(response, toServiceData(req.getRequestURL().toString(), accountData.asHuman()));
        return;
      }

    } catch (PersistenceException e) {

      sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          RC_INTERNAL_SERVER_ERROR);
      LOG.warning(e.getMessage(), e);
      return;

    } catch (InvalidParticipantAddress e) {
      sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_INVALID_ACCOUNT_ID_SYNTAX);
      return;
    }
  }


  protected void queryParticipantAccount(HttpServletRequest req, HttpServletResponse response)
      throws IOException {


    // GET /account?p=joe@local.net;tom@local.net

    Collection<ParticipantId> participantsQuery = getParticipantsFromRequestQuery(req);

    if (participantsQuery == null) {
      sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, RC_MISSING_PARAMETER);
      return;
    }

    sendResponse(response,
        toPublicServiceData(req.getRequestURL().toString(), participantsQuery, accountStore));


  }


  @Override
  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {

    String[] pathTokens = SwellRtServlet.getCleanPathInfo(req).split("/");
    String participantToken = pathTokens.length > 2 ? pathTokens[2] : null;

    // All operations required a logged in user


    if (req.getMethod().equals("POST") && participantToken == null) {

      createAccount(req, response);

    } else if (req.getMethod().equals("POST") && participantToken != null) {

      if (checkForLoggedInUser(req, response) == null) return;
      updateAccount(req, response);

    } else if (req.getMethod().equals("GET")) {

      if (checkForLoggedInUser(req, response) == null) return;

      if (participantToken != null) {

        getParticipantAccount(req, response);

      } else {

        queryParticipantAccount(req, response);

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



  protected Collection<AccountServiceData> toPublicServiceData(String serverBaseUrl,
      Collection<ParticipantId> participants, AccountStore accountStore) {

    List<AccountServiceData> accountServiceDataList = new ArrayList<AccountServiceData>();

    for (ParticipantId p : participants) {

      try {
        AccountData accountData = accountStore.getAccount(p);

        if (accountData != null && accountData.isHuman()) {
          accountServiceDataList.add(toPublicServiceData(serverBaseUrl, accountData.asHuman()));
        }

      } catch (PersistenceException e) {
        // Ignore missing accounts
      }

    }

    return accountServiceDataList;
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

  protected static AccountServiceData toPublicServiceData(String serverBaseUrl,
      HumanAccountData account) {

    AccountServiceData data = new AccountServiceData();

    data.id = account.getId().getAddress();
    String avatarUrl = getAvatarUrl(serverBaseUrl, account);
    data.avatarUrl = avatarUrl == null ? "" : avatarUrl;
    data.locale = account.getLocale() == null ? "" : account.getLocale();

    return data;

  }


}
