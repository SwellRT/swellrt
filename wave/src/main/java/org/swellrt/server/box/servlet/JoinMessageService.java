package org.swellrt.server.box.servlet;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.velocity.Template;
import org.apache.velocity.tools.ConversionUtils;
import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.inject.Inject;

public class JoinMessageService extends BaseService {

  private static final String ID_OR_EMAIL = "id-or-email";
  private static final String ADMIN = "admin";
  private static final String JOIN_MESSAGE_BUNDLE = "EmailMessages";
  private static final String JOIN_MESSAGE_TEMPLATE = "Join.vm";
  public static final String URL = "url";
  public static final String URL_TEXT = "url_text";
  public static final String MESSAGE = "message";
  private final AccountStore accountStore;
  private final EmailSender emailSender;
  private DecoupledTemplates decTemplates;

  @Inject
  public JoinMessageService(SessionManager sessionManager, AccountStore accountStore,
      EmailSender emailSender, DecoupledTemplates decTemplates) {
    super(sessionManager);
    this.accountStore = accountStore;
    this.emailSender = emailSender;
    this.decTemplates = decTemplates;
  }

  @Override
  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {

    ParticipantId participantId = sessionManager.getLoggedInUser(req);

    HttpSession session = sessionManager.getSession(req);
    HumanAccountData hum = sessionManager.getLoggedInAccount(session).asHuman();

    Locale locale;

    if (hum != null && !hum.getId().isAnonymous()) {

      if (hum.getLocale() != null) {

        locale = ConversionUtils.toLocale(hum.getLocale());

      }

      locale = Locale.getDefault();

    } else {

      locale = Locale.getDefault();

    }

    String url = req.getParameter(URL);

    String urlText = req.getParameter(URL_TEXT);

    HashMap<String, Object> params = new HashMap<String, Object>();

    String idOrEmail = req.getParameter(ID_OR_EMAIL);

    String admin = req.getParameter(ADMIN);

    String message = req.getParameter(MESSAGE);

    String joinerEmail = idOrEmail;

    String joinerNickOrEmail = idOrEmail;

    String adminNick = null;

    String adminEmail = null;


    // Get admin email information
    try {
      AccountData acc = accountStore.getAccount(new ParticipantId(admin));
      if (acc != null && !acc.getId().isAnonymous()) {
        adminEmail = acc.asHuman().getEmail();
        adminNick = acc.getId().getName();
      }
    } catch (PersistenceException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    // Get joiner information if it is an user
    try {
      AccountData acc = accountStore.getAccount(new ParticipantId(idOrEmail));
      if (acc != null && !acc.getId().isAnonymous()) {
        joinerEmail = acc.asHuman().getEmail();
        joinerNickOrEmail = acc.getId().getName();
      }
    } catch (PersistenceException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }


    params.put("admin", adminNick);
    params.put("joiner", joinerNickOrEmail);
    params.put("joinerEmail", joinerEmail);
    params.put("url", url);
    params.put("url_text", urlText);
    params.put("message", message);

    try {

      Template t = decTemplates.getTemplateFromName(JOIN_MESSAGE_TEMPLATE);
      ResourceBundle b = decTemplates.getBundleFromName(JOIN_MESSAGE_BUNDLE, locale);

      String subject;

      if (participantId != null){
        String inviter = participantId.getAddress().split("@")[0];
        subject = MessageFormat.format(b.getString("joinNamedEmailSubject"), inviter, urlText);
      } else {
        subject = MessageFormat.format(b.getString("unnamedEmailSubject"), urlText);
      }


      String body = decTemplates.getTemplateMessage(t, JOIN_MESSAGE_BUNDLE, params, locale);

      emailSender.send(new InternetAddress(adminEmail), subject, body);

    } catch (AddressException e) {
        // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (MessagingException e) {
        // TODO Auto-generated catch block
      e.printStackTrace();
      }
    }


}
