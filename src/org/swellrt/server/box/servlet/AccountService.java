package org.swellrt.server.box.servlet;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.apache.commons.io.IOUtils;
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
 */
public class AccountService implements SwellRTService {


  public static class ServiceData {

    public String id;
    public String name; // For future use
    public String password;
    public String email;
    public String avatar_data;
    public String avatar_url;
    public String locale;

    public static ServiceData fromJson(String json) {
      Gson gson = new Gson();
      return gson.fromJson(json, ServiceData.class);
    }

    public String toJson() {
      Gson gson = new Gson();
      return gson.toJson(this);
    }

  }

  public static class ServiceError {

    public String error;

    public ServiceError(String error) {
      this.error = error;
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

      ServiceData userData = getRequestUserData(req);

      ParticipantId participantId = null;

      try {
        participantId = ParticipantId.of(userData.id + "@" + domain);
      } catch (InvalidParticipantAddress e) {
        sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, "ACCOUNT_ID_WRONG_SYNTAX");
        return;
      }

      AccountData accountData = null;

      try {
        accountData = accountStore.getAccount(participantId);
      } catch (PersistenceException e) {
        sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR");
        LOG.warning(e.getMessage(), e);
        return;
      }

      if (accountData != null) {
        sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_ALREADY_EXISTS");
        return;
      }

      if (userData.password == null) {
        userData.password = "";
      }

      HumanAccountDataImpl account =
          new HumanAccountDataImpl(participantId, new PasswordDigest(
              userData.password.toCharArray()));

      // TODO validate email

      if (userData.email != null) account.setEmail(userData.email);

      if (userData.locale != null) account.setLocale(userData.locale);

      if (userData.avatar_data != null) {
        try {
          String avatarFileId = storeAvatar(participantId, userData.avatar_data,
              null);
          account.setAvatarFileId(avatarFileId);
        } catch (IOException e) {
          sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "INTERNAL_SERVER_ERROR");
          LOG.warning(e.getMessage(), e);
          return;
        }
      }

      try {
        accountStore.putAccount(account);
      } catch (PersistenceException e) {
        sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR");
        LOG.warning(e.getMessage(), e);
        return;
      }

      sendResponse(response, toServiceData(account));
      return;


    } else if (req.getMethod().equals("POST") && participantToken != null) {

      // POST /account/joe update user's account


      ParticipantId participantId = null;

      try {
        participantId = ParticipantId.of(participantToken + "@" + domain);
      } catch (InvalidParticipantAddress e) {
        sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, "ACCOUNT_ID_WRONG_SYNTAX");
        return;
      }

      AccountData accountData = null;

      try {
        accountData = accountStore.getAccount(participantId);
      } catch (PersistenceException e) {
        sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR");
        LOG.warning(e.getMessage(), e);
        return;
      }

      if (accountData == null) {
        sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_NOT_FOUND");
        return;
      }

      // if the account exists, only the user can modify the profile
      if (!participantId.equals(loggedInUser)) {
        sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_NOT_LOGGED_ID");
        return;
      }

      // Modify

      ServiceData userData = getRequestUserData(req);

      HumanAccountData account = accountData.asHuman();

      if (userData.email != null)
        account.setEmail(userData.email);

      if (userData.locale != null) account.setLocale(userData.locale);

      if (userData.avatar_data != null) {
        try {
          String avatarFileId =
              storeAvatar(participantId, userData.avatar_data, account.getAvatarFileName());
          account.setAvatarFileId(avatarFileId);
        } catch (IOException e) {
          sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
              "INTERNAL_SERVER_ERROR");
          LOG.warning(e.getMessage(), e);
          return;
        }
      }

      try {
        accountStore.putAccount(account);
      } catch (PersistenceException e) {
        sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR");
        LOG.warning(e.getMessage(), e);
        return;
      }


      sendResponse(response, toServiceData(account));
      return;


    } else if (req.getMethod().equals("GET")) {

      ParticipantId participantId = null;

      try {
        participantId = ParticipantId.of(participantToken + "@" + domain);
      } catch (InvalidParticipantAddress e) {
        sendResponseError(response, HttpServletResponse.SC_BAD_REQUEST, "ACCOUNT_ID_WRONG_SYNTAX");
        return;
      }


      AccountData accountData = null;

      try {
        accountData = accountStore.getAccount(participantId);
      } catch (PersistenceException e) {
        sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR");
        LOG.warning(e.getMessage(), e);
        return;
      }

      if (accountData == null) {
        sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_NOT_FOUND");
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
        sendResponseError(response, HttpServletResponse.SC_FORBIDDEN, "ACCOUNT_NOT_LOGGED_ID");
        return;
      }


      // GET /account/joe retrieve user's account data
      sendResponse(response, toServiceData(accountData.asHuman()));
      return;

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

  protected ServiceData getRequestUserData(HttpServletRequest request) throws IOException {

    StringWriter writer = new StringWriter();
    IOUtils.copy(request.getInputStream(), writer, Charset.forName("UTF-8"));

    // Deserialize data
    Gson gson = new Gson();
    return gson.fromJson(writer.toString(), ServiceData.class);
  }


  protected void sendResponse(HttpServletResponse response, Object responseData) throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store");
    Gson gson = new Gson();
    response.getWriter().append(gson.toJson(responseData));
    response.getWriter().flush(); // Commit the response
  }


  protected void sendResponseError(HttpServletResponse response, int httpStatus,
      final String appErrorCode) throws IOException {

    response.setStatus(httpStatus);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store");
    Gson gson = new Gson();
    response.getWriter().append(gson.toJson(new ServiceError(appErrorCode)));
    response.getWriter().flush(); // Commit the response
  }



  protected static String getAvatarUrl(HumanAccountData account) {
    return SwellRtServlet.SERVLET_CONTEXT + "/account/" + account.getId().getName() + "/avatar/"
        + account.getAvatarFileName();
  }

  protected ServiceData toServiceData(HumanAccountData account) {

    ServiceData data = new ServiceData();

    data.email = account.getEmail();
    data.avatar_url = getAvatarUrl(account);
    data.locale = account.getLocale();

    return data;

  }


}
