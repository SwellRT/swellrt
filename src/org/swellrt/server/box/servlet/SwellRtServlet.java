package org.swellrt.server.box.servlet;

import com.google.inject.Injector;

import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.persistence.mongodb.MongoDbProvider;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
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

  public final static String SERVLET_CONTEXT = "/swell";

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

    int separatorIndex = path.indexOf(";");

    if (separatorIndex >= 0) {
      return path.substring(0, separatorIndex);
    }

    return path;
  }



  /**
   * Create an http response to the fetch query. Main entrypoint for this class.
   */
  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse response) throws IOException {

    String[] pathTokens = getCleanPathInfo(req).split("/");
    String entity = pathTokens[1];

    if (entity.equals("model")) {

      injector.getInstance(QueryModelService.class).execute(req, response);

    } else if (entity.equals("account")) {

      injector.getInstance(AccountService.class).execute(req, response);

    } else if (entity.equals("auth")) {

      injector.getInstance(AuthenticationService.class).execute(req, response);

    } else {

      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;

    }

  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse response) throws IOException {


    String[] pathTokens = getCleanPathInfo(req).split("/");
    String entity = pathTokens[1];


    if (entity.equals("notification")) {

      injector.getInstance(NotificationService.class).execute(req, response);

    } else if (entity.equals("email")) {

      injector.getInstance(EmailService.class).execute(req, response);

    } else if (entity.equals("password")) {

      injector.getInstance(PasswordService.class).execute(req, response);

    } else if (entity.equals("account")) {

      injector.getInstance(AccountService.class).execute(req, response);

    } else if (entity.equals("auth")) {

      injector.getInstance(AuthenticationService.class).execute(req, response);

    } else {

      response.sendError(HttpServletResponse.SC_BAD_REQUEST);
      return;

    }


  }
}
