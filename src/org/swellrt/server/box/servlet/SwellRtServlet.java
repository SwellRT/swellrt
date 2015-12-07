package org.swellrt.server.box.servlet;

import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.mongodb.MongoDbProvider;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A Servlet providing SwellRT REST operations on /swell context
 *
 *
 *
 * TODO: to use a "cool" REST framework
 *
 *
 * @author pablojan@gmail.com
 *
 */
@Singleton
@SuppressWarnings("serial")
public class SwellRtServlet extends HttpServlet {

  private static final Log LOG = Log.get(SwellRtServlet.class);

  private final SessionManager sessionManager;

  // TODO pass dependencies to each Service by injection
  private MongoDbProvider mongoDbProvider;
  private WaveletProvider waveletProvider;

  @Inject
  public SwellRtServlet(SessionManager sessionManager, MongoDbProvider mongoDbProvider,
      WaveletProvider waveletProvider) {
    this.sessionManager = sessionManager;
    this.mongoDbProvider = mongoDbProvider;
    this.waveletProvider = waveletProvider;
  }


  /**
   * Create an http response to the fetch query. Main entrypoint for this class.
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {

    ParticipantId participantId = sessionManager.getLoggedInUser(req.getSession(false));

    if (participantId == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    String[] pathTokens = req.getPathInfo().split("/");
    String entity = pathTokens[1];

    if (entity.equals("model")) {

      QueryModelService.get(participantId, mongoDbProvider).execute(req, response);

    } else if (entity.equals("access")) {

      AccessModelService.get(participantId, waveletProvider).execute(req, response);

    } else if (entity.equals("notification")) {

      NotificationService.get(participantId).execute(req, response);

    } else {

      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;

    }

  }

}
