package org.swellrt.server.box.servlet;

import com.google.inject.Inject;

import org.swellrt.server.box.events.gcm.GCMSubscriptionStore;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class NotificationService extends SwellRTService {

  private static final Log LOG = Log.get(NotificationService.class);

  private final GCMSubscriptionStore subscriptionStore;

  @Inject
  public NotificationService(SessionManager sessionManager, GCMSubscriptionStore subscriptionStore) {
    super(sessionManager);
    this.subscriptionStore = subscriptionStore;
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
    } else {

      String name = paramNames.nextElement();
      String value = req.getParameter(name);

      HttpSession session = req.getSession(false);

      System.out.println(session);
      System.out.println(sessionManager);
      String account = sessionManager.getLoggedInAccount(session).getId().getAddress();

      switch (name) {

        case "registerDevice":
          subscriptionStore.register(account, value);
          LOG.info("Device <" + value + "> registered for account " + account);
          response.setStatus(HttpServletResponse.SC_NO_CONTENT);
          break;

        case "unregisterDevice":
          subscriptionStore.unregister(account, value);
          LOG.info("Device <" + value + "> unregistered for account " + account);
          response.setStatus(HttpServletResponse.SC_NO_CONTENT);
          break;

        case "subscribe":
          subscriptionStore.addSubscriptor(value, account);
          LOG.info("Account " + account + " subscribed to wave id " + value);
          response.setStatus(HttpServletResponse.SC_NO_CONTENT);
          break;

        case "unsubscribe":
          subscriptionStore.removeSubscriptor(value, account);
          LOG.info("Account " + account + " unsubscribed to wave id " + value);
          response.setStatus(HttpServletResponse.SC_NO_CONTENT);
          break;

        default:
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown operation: " + name);
      }
    }
  }
}
