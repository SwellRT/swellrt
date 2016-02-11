package org.swellrt.server.box.servlet;


import com.google.inject.Inject;
import com.google.inject.name.Named;

import net.lightoze.gwt.i18n.client.LocaleFactory;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.NotImplementedException;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.authentication.i18n.PasswordRestoreMessages;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class EmailService implements SwellRTService {

  public static final String EMAIL = "email";

  private static final String METHOD = "method";

  private static final String SET = "set";

  private static final String PASSWORD_RESET = "password-reset";

  private static final String RECOVER_URL = "recover-url";

  @Inject
  private SessionManager sessionManager;
  @Inject
  private AccountStore accountStore;

  @Inject
  @Named(CoreSettings.EMAIL_HOST)
  String host;

  @Inject
  @Named(CoreSettings.EMAIL_FROM_ADDRESS)
  String from;

  @Override
  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {

    Enumeration<String> paramNames = req.getParameterNames();

    if (!paramNames.hasMoreElements()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No parameters found!");
      return;
    } else {

      String method = req.getParameter(METHOD);
      String email = req.getParameter(EMAIL);

      switch (method) {

        case SET:
          HttpSession session = req.getSession(false);
          HumanAccountData account = sessionManager.getLoggedInAccount(session).asHuman();

          if (account != null && account.getId().isAnonymous()) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User is anonymous");
            return;
          }

          try {

            account.setEmail(email);
            accountStore.putAccount(account);
            response.setStatus(HttpServletResponse.SC_OK);

          } catch (IllegalArgumentException t) {

            response.sendError(HttpServletResponse.SC_BAD_REQUEST, t.getMessage());

          } catch (PersistenceException e) {

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

          }

          break;

        case PASSWORD_RESET:

          String recoverUrl = URLDecoder.decode(req.getParameter(RECOVER_URL), "UTF-8");
          String idOrEmail = URLDecoder.decode(req.getParameter("id-or-email"), "UTF-8");

          try {

            List<AccountData> accounts = null;
            try {
              accounts = accountStore.getAccountByEmail(idOrEmail);
            } catch (NotImplementedException e) {
              response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            }

            // try to find by username if not found by email
            if (accounts == null || accounts.isEmpty()) {
              AccountData acc = accountStore.getAccount(new ParticipantId(idOrEmail));

              if (acc != null && !acc.getId().isAnonymous()) {
                accounts.add(acc);
              }
            }

            PasswordRestoreMessages messages = LocaleFactory.get(PasswordRestoreMessages.class);

            if (accounts != null && !accounts.isEmpty()) {

              for (AccountData a : accounts) {

                String userAddress = a.getId().getAddress();

                double random = Math.random();

                String token =
                    Base64.encodeBase64URLSafeString((String.valueOf(random)).getBytes());

                a.asHuman().setRecoveryToken(token);
                accountStore.putAccount(a);


                if (recoverUrl.contains("$user-id")) {
                  recoverUrl = recoverUrl.replaceAll("\\$user-id", userAddress);
                }

                if (recoverUrl.contains("$token")) {
                  recoverUrl = recoverUrl.replaceAll("\\$token", token);

                } else {
                  recoverUrl = recoverUrl + token;
                }

                Properties properties = new Properties();


                // Get the default Session object.
                Session mailSession = Session.getDefaultInstance(properties, null);

                // Setup mail server
                properties.setProperty("mail.smtp.host", host);


                MimeMessage message = new MimeMessage(mailSession);

                message.setFrom(new InternetAddress(from));

                message.addRecipient(Message.RecipientType.TO, new InternetAddress(idOrEmail));
                message.setSubject(messages.emailSubject(userAddress));
                message.setText(messages.restoreEmailBody(userAddress, recoverUrl));

                // Send message
                Transport.send(message);

              }
            }


            response.setStatus(HttpServletResponse.SC_OK);

          } catch (MessagingException mex) {

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, mex.getMessage());

          } catch (PersistenceException e) {

            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());

          }

          break;
      }
    }
  }
}
