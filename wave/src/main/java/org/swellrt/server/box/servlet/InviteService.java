package org.swellrt.server.box.servlet;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.NotImplementedException;
import org.apache.velocity.Template;
import org.apache.velocity.tools.ConversionUtils;
import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.inject.Inject;

public class InviteService extends BaseService {

  private static final String ID_OR_EMAIL = "id-or-email";
  private static final String INVITATION_EMAIL_BUNDLE = "EmailMessages";
  private static final String INVITATION_TEMPLATE = "Invitation.vm";
  public static final String URL = "url";
  public static final String URL_TEXT = "url_text";
  private final AccountStore accountStore;
  private final EmailSender emailSender;
  private DecoupledTemplates decTemplates;

  @Inject
  public InviteService(SessionManager sessionManager, AccountStore accountStore,
      EmailSender emailSender, DecoupledTemplates decTemplates) {
    super(sessionManager);
    this.accountStore = accountStore;
    this.emailSender = emailSender;
    this.decTemplates = decTemplates;
  }

  @Override
  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {

    ParticipantId participantId = sessionManager.getLoggedInUser(req);

    if (participantId == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    HttpSession session = sessionManager.getSession(req);
    HumanAccountData hum = sessionManager.getLoggedInAccount(session).asHuman();

    Locale locale;

    if (hum != null && !hum.getId().isAnonymous()) {

      if (hum.getLocale() != null) {

        locale = ConversionUtils.toLocale(hum.getLocale());

      }

      locale = Locale.getDefault();

    } else {

      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;

    }

    String url = req.getParameter(URL);

    String urlText = req.getParameter(URL_TEXT);

    HashMap<String, Object> params = new HashMap<String, Object>();

    String idOrEmail = req.getParameter(ID_OR_EMAIL);

    String emailAddress = idOrEmail;

    String nickOrEmail = idOrEmail;

    List<AccountData> accounts = null;

    try {

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
          emailAddress = acc.asHuman().getEmail();
          nickOrEmail = acc.getId().getName();
        }
      }
    } catch (PersistenceException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    String inviter = participantId.getAddress().split("@")[0];
    params.put("inviter", inviter);
    params.put("url", url);
    params.put("nick_or_email", nickOrEmail);
    params.put("url_text", urlText);

    try {

      Template t = decTemplates.getTemplateFromName(INVITATION_TEMPLATE);
      ResourceBundle b = decTemplates.getBundleFromName(INVITATION_EMAIL_BUNDLE, locale);

      String subject = MessageFormat.format(b.getString("invitationEmailSubject"), inviter);

      String body = decTemplates.getTemplateMessage(t, INVITATION_EMAIL_BUNDLE, params, locale);

      emailSender.send(new InternetAddress(emailAddress), subject, body);
    } catch (AddressException e) {
        // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (MessagingException e) {
        // TODO Auto-generated catch block
      e.printStackTrace();
      }
    }


}
