package org.swellrt.server.box.servlet;

import java.io.IOException;

import javax.inject.Singleton;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.waveprotocol.box.server.authentication.HttpWindowSession;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.wave.util.logging.Log;

import com.google.inject.Inject;

/**
 * A echo service to acknowledge if session cookie is sent by
 * the browser. 
 * 
 * A false response means that third party cookies are disabled.
 * Pass a session id token to cache the request
 * 
 * GET /echo/{sessionId}
 * 
 * 200 OK
 * {
 *    sessionCookie: true,
 *    sessionId: ... 
 *    windowId: ...
 * }
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@Singleton
public class EchoService extends BaseService {

  private static final Log LOG = Log.get(EchoService.class);
  
  
  public static class EchoServiceData extends ServiceData {

    protected final boolean sessionCookie;
    protected final String sessionId;
    protected final String windowId;
    

    public EchoServiceData(boolean sessionCookie, String sessionId, String windowId) {
      this.sessionCookie = sessionCookie;
      this.sessionId = sessionId;
      this.windowId = windowId;
    }

  }
  
  @Inject
  public EchoService(SessionManager sessionManager) {
    super(sessionManager);
  }

  @Override
  public void execute(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    try {

      if (request.getMethod().equals("GET"))
        doGet(request, response);


    } catch (RuntimeException e) {
      sendResponseError(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
          RC_INTERNAL_SERVER_ERROR);
      LOG.warning(e.getMessage(), e);
    }
    
  }
  
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    boolean hasSessionCookie = SessionManager.hasSessionCookie(request);
    HttpSession httpSession = sessionManager.getSession(request);
    HttpWindowSession httpWindowSession = httpSession instanceof HttpWindowSession ? (HttpWindowSession) httpSession : null;
    
    EchoServiceData responseData = new EchoServiceData(hasSessionCookie, httpSession.getId(), httpWindowSession != null ? httpWindowSession.getWindowId() : "");
    
    sendResponse(response, responseData);
  }

}
