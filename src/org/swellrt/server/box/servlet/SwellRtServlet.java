package org.swellrt.server.box.servlet;

import com.google.inject.Injector;

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
  private Injector injector;


  @Inject
  public SwellRtServlet(SessionManager sessionManager, MongoDbProvider mongoDbProvider,
      WaveletProvider waveletProvider, Injector injector) {
    this.sessionManager = sessionManager;
    this.mongoDbProvider = mongoDbProvider;
    this.waveletProvider = waveletProvider;
    this.injector = injector;
  }


  /**
   * Do some clean up in query string to remove URL session param. This is a
   * workaround for for the jetty's session URL rewriting.
   * 
   * @param req HttpServletRequest the servlet request
   * @return the Path info without jetty's extra session parameter
   */
  public static String getCleanPathInfo(HttpServletRequest req) {

    String path = req.getPathInfo();

    if (path == null) return "";

    // The ';sid=' syntax is jetty specific.
    int indexOfSessionParam = path.indexOf(";sid=");

    if (indexOfSessionParam >= 0) {
      return path.substring(0, indexOfSessionParam);
    }

    return path;
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

    String[] pathTokens = getCleanPathInfo(req).split("/");
    String entity = pathTokens[1];

    if (entity.equals("model")) {

      QueryModelService.get(participantId, mongoDbProvider).execute(req, response);

    } else if (entity.equals("access")) {

      AccessModelService.get(participantId, waveletProvider).execute(req, response);

    } else {

      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;

    }

  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse response) throws IOException {

    ParticipantId participantId = sessionManager.getLoggedInUser(req.getSession(false));

    if (participantId == null) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    String[] pathTokens = getCleanPathInfo(req).split("/");
    String entity = pathTokens[1];

    if (entity.equals("notification")) {

      injector.getInstance(NotificationService.class).execute(req, response);

    } else if (entity.equals("email")) {

      injector.getInstance(EmailServlet.class).execute(req, response);

    } else if (entity.equals("password")) {

      injector.getInstance(PasswordServlet.class).execute(req, response);

    } else {

      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;

    }
  }
}
