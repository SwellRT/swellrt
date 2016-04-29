package org.swellrt.server.box.servlet;

import com.google.inject.Inject;

import org.apache.velocity.Template;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.wave.model.wave.ParticipantId;

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

public class InviteService extends SwellRTService {

  private static final String EMAIL = "email";
  private static final String INVITATION_EMAIL_BUNDLE = "InvitationEmailMessages";
  private static final String INVITATION_TEMPLATE = "Invitation.vm";
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

    ParticipantId participantId = sessionManager.getLoggedInUser(req.getSession(false));

    HumanAccountData hum = sessionManager.getLoggedInAccount(req.getSession(false)).asHuman();

    Locale locale;

    if (hum != null) {

      if (hum.getLocale() != null) {

        locale = new Locale(hum.getLocale());

      }

      locale = Locale.getDefault();

    } else {

      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;

    }

    if (participantId == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

    String email = req.getParameter(EMAIL);

    HashMap<String, Object> params = new HashMap<String, Object>();

    params.put("inviter", participantId.getAddress());

    try {

      Template t = decTemplates.getTemplateFromName(INVITATION_TEMPLATE);
      ResourceBundle b = decTemplates.getBundleFromName(INVITATION_EMAIL_BUNDLE, locale);

      String subject = MessageFormat.format(b.getString("emailSubject"), null);

      String body = decTemplates.getTemplateMessage(t, b, params, locale);

      emailSender.send(new InternetAddress(email), subject, body);
    } catch (AddressException e) {
        // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (MessagingException e) {
        // TODO Auto-generated catch block
      e.printStackTrace();
      }
    }


}
