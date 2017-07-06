package org.swellrt.server.box.servlet;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.waveprotocol.box.server.account.AccountData;
import org.waveprotocol.box.server.account.HumanAccountData;
import org.waveprotocol.box.server.account.SecretToken;
import org.waveprotocol.box.server.authentication.PasswordDigest;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.inject.Inject;

/**
 * Service to change passwords
 *
 * POST
 *
 * /password?id={id}&token-or-password={tokenOrPassword}&new-password={newPassword}
 *
 *
 */
public class PasswordService extends BaseService {

  public static final String ID = "id";

  public static final String TOKEN_OR_PASSWORD = "token-or-password";

  public static final String NEW_PASSWORD = "new-password";


  private final AccountStore accountStore;

  @Inject
  public PasswordService(SessionManager sessionManager, AccountStore accountStore) {
    super(sessionManager);
    this.accountStore = accountStore;
  }

  @Override
  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {

    ParticipantId participantId = sessionManager.getLoggedInUser(req);

    if (participantId == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }

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

      ParticipantId pId = ParticipantId.of(id);

      if (pId.isAnonymous()) {
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User is anonymous");
        return;
      }


      AccountData a = accountStore.getAccount(pId);
      if (a != null) {
        HumanAccountData account = a.asHuman();
        SecretToken storedToken = account.getRecoveryToken();

        if ((storedToken != null && storedToken.isActive() && storedToken.getToken().equals(
            tokenOrPassword))
            || account.getPasswordDigest().verify(tokenOrPassword.toCharArray())) {

          // Change the original account object to preserve all acount data
          // during DB update.
          account.setPasswordDigest(new PasswordDigest(newPassword.toCharArray()));
          // Reset the token anyway
          account.setRecoveryToken((String) null);

          accountStore.putAccount(account);
          response.setStatus(HttpServletResponse.SC_OK);
        } else {
          response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
      }

    } catch (PersistenceException e) {
      response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } catch (InvalidParticipantAddress e) {
    	response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "User id is not valid");
	}

  }
}
