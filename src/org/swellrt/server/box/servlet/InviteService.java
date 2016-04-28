package org.swellrt.server.box.servlet;

import com.google.inject.Inject;

import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class InviteService extends SwellRTService {

  private static final String EMAIL = "email";
  private static final String INVITATION_EMAIL_BUNDLE = "InvitationEmailMessages";
  private static final String INVITATION_TEMPLATE = "Invitation.vm";
  private final AccountStore accountStore;
  private final EmailSender emailSender;

  @Inject
  public InviteService(SessionManager sessionManager, AccountStore accountStore,
      EmailSender emailSender) {
    super(sessionManager);
    this.accountStore = accountStore;
    this.emailSender = emailSender;
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

    HashMap<String, String> params = new HashMap<String, String>();

    params.put("inviter", participantId.getAddress());

      try {
      emailSender.send(email, INVITATION_TEMPLATE, INVITATION_EMAIL_BUNDLE, params, locale);
    } catch (AddressException e) {
        // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (MessagingException e) {
        // TODO Auto-generated catch block
      e.printStackTrace();
      }
    }


}
