package org.swellrt.server.box.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

public abstract class BaseService {


  protected static final String RC_ACCOUNT_ALREADY_EXISTS = "ACCOUNT_ALREADY_EXISTS";
  protected static final String RC_INVALID_EMAIL_ADDRESS = "INVALID_EMAIL_ADDRESS";
  protected static final String RC_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
  protected static final String RC_INVALID_ACCOUNT_ID_SYNTAX = "INVALID_ACCOUNT_ID_SYNTAX";
  protected static final String RC_INVALID_JSON_SYNTAX = "INVALID_JSON_SYNTAX";
  protected static final String RC_ACCOUNT_NOT_FOUND = "ACCOUNT_NOT_FOUND";
  protected static final String RC_ACCOUNT_NOT_LOGGED_IN = "ACCOUNT_NOT_LOGGED_IN";
  protected static final String RC_LOGIN_FAILED = "LOGIN_FAILED";
  protected static final String RC_MISSING_PARAMETER = "MISSING_PARAMETER";
  protected static final String RC_INVALID_HTTP_METHOD = "INVALID_HTTP_METHOD";
  protected static final String RC_INVALID_OBJECT_ID = "INVALID_OBJECT_ID";
  protected static final String RC_INVALID_OBJECT_PATH = "INVALID_OBJECT_PATH";


  public static class ServiceError {

    public String error;

    public ServiceError(String error) {
      this.error = error;
    }

  }

  protected final SessionManager sessionManager;

  public BaseService(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }

  public abstract void execute(HttpServletRequest req, HttpServletResponse response)
      throws IOException;


  protected void sendResponse(HttpServletResponse response, Object responseData) throws IOException {

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store");
    Gson gson = new Gson();
    response.getWriter().append(gson.toJson(responseData));
    response.getWriter().flush(); // Commit the response
  }

  protected void sendResponse(HttpServletResponse response, JsonElement responseData) throws IOException {

	    response.setStatus(HttpServletResponse.SC_OK);
	    response.setContentType("application/json");
	    response.setCharacterEncoding("UTF-8");
	    response.setHeader("Cache-Control", "no-store");
	    response.getWriter().append(responseData.toString());
	    response.getWriter().flush(); // Commit the response
	  }

  protected void sendResponseError(HttpServletResponse response, int httpStatus,
      final String appErrorCode) throws IOException {

    response.setStatus(httpStatus);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.setHeader("Cache-Control", "no-store");
    Gson gson = new Gson();
    response.getWriter().append(gson.toJson(new ServiceError(appErrorCode)));
    response.getWriter().flush(); // Commit the response
  }


  protected String getBaseUrl(HttpServletRequest req) {
    return req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort();
  }


	protected ParticipantId getLoggedInUser(HttpServletRequest req) throws ServiceException {
		ParticipantId pid = sessionManager.getLoggedInUser(req);
		if (pid == null) {
			throw new ServiceException("Can't retrieve logged in user", HttpServletResponse.SC_FORBIDDEN, RC_ACCOUNT_NOT_LOGGED_IN);
		}
		return pid;
	}

	// Browser must have at least one window with authenticated user
	protected void checkAnySession(HttpServletRequest req) throws ServiceException {
	  if (sessionManager.getAllLoggedInUser(req).isEmpty())
	    throw new ServiceException("No active sessions found in the browser", HttpServletResponse.SC_FORBIDDEN, RC_ACCOUNT_NOT_LOGGED_IN);
	}

  /**
   * Check if a participant is in the HTTP current session. It means that at
   * least is logged in one Window session.
   *
   * @param participantId
   * @return
   */
  protected boolean isSessionParticipant(HttpServletRequest req, ParticipantId participantId) {
    return sessionManager.getAllLoggedInUser(req).contains(participantId);
  }

}
