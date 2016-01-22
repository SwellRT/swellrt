package org.swellrt.server.box.servlet;

import com.google.inject.Inject;

import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.HumanAccountDataImpl;
import org.waveprotocol.box.server.account.SecretToken;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;
import java.util.Date;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class PasswordServlet implements SwellRTService {

  public static final String ID = "id";

  public static final String TOKEN_OR_PASSWORD = "token-or-password";

  public static final String NEW_PASSWORD = "new-password";

  @Inject
  private SessionManager sessionManager;
  @Inject
  private AccountStore accountStore;

  @Override
  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {

    Enumeration<String> paramNames = req.getParameterNames();

    if (!paramNames.hasMoreElements()) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No parameters found!");
      return;
    }

    String id = req.getParameter(ID);
    String tokenOrPassword = req.getParameter(TOKEN_OR_PASSWORD);
    String newPassword = req.getParameter(NEW_PASSWORD);

    if (id == null || tokenOrPassword == null || newPassword == null) {
      response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing required parameters");
    }

    try {

      ParticipantId pId = new ParticipantId(id);
      HumanAccountData account = accountStore.getAccount(pId).asHuman();

      SecretToken storedToken = account.getRecoveryToken();

      if (storedToken.getToken().equals(tokenOrPassword)
          && storedToken.getExpirationDate().after(new Date())) {
        HumanAccountDataImpl newAccount =
            new HumanAccountDataImpl(pId, new PasswordDigest(newPassword.toCharArray()));

        if (account.getEmail() != null) {
          newAccount.setEmail(account.getEmail());
        }

        accountStore.putAccount(account);
        response.setStatus(HttpServletResponse.SC_OK);

      } else {
        response.sendError(HttpServletResponse.SC_FORBIDDEN);
      }

    } catch (PersistenceException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

  }
}
