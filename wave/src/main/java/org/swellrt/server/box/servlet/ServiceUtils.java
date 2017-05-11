package org.swellrt.server.box.servlet;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.waveprotocol.box.server.authentication.SessionManager;

import com.google.common.base.Preconditions;

public class ServiceUtils {


  /**
   * Extracts the session id string from the URL's path if present.
   * For example: ";sid=fds342534sdf"
   *
   * This function assumes that ";sid=..." string is always at the
   * end of the URL's path part.
   *
   * @param request the HTTP request
   * @return the session string or empty string
   */
  public static String getSessionIdFromPath(HttpServletRequest request) {
    Preconditions.checkNotNull(request, "Request can't be null");

    if (request.getPathInfo() == null || request.getPathInfo().isEmpty()) return "";

    // The ';sid=' syntax is jetty specific.
    int indexSid = request.getPathInfo().indexOf(";"+SessionManager.SESSION_URL_PARAM+"=");

    if (indexSid >= 0) {
      return request.getPathInfo().substring(indexSid, request.getPathInfo().length());
    }

    return "";
  }

  public static Cookie getCookie(HttpServletRequest request, String name) {

    Preconditions.checkNotNull(request, "Request can't be null");
    Preconditions.checkNotNull(name, "Cookie name can't be null");

    Cookie[] cookies = request.getCookies();
    if (cookies != null)
      for (Cookie c: cookies) {
        if (c.getName().equalsIgnoreCase(name))
          return c;
      }
    return null;

  }

  public static String getSessionUrlRewrite(HttpServletRequest request) {
    return getCookie(request, SessionManager.SESSION_COOKIE_NAME) != null ? "" : getSessionIdFromPath(request);
  }


  public static UrlBuilder getUrlBuilder(final HttpServletRequest request) {
    return getUrlBuilder(request, SwellRtServlet.SERVLET_CONTEXT);
  }

  public static UrlBuilder getUrlBuilder(final HttpServletRequest request, final String context) {

    return new UrlBuilder() {

      @Override
      public String build(String relativePath, String queryString) {
        Preconditions.checkNotNull(relativePath, "Path can't be null");

        String scheme;

        if (request.getHeader("X-Forwarded-Proto") != null) {
          scheme = request.getHeader("X-Forwarded-Proto");
        } else {
          scheme = request.getScheme();
        }

        String port =
            request.getServerPort() == 80 || request.getServerPort() == 443 ? "" : ":"
                + request.getServerPort();

        String base = scheme + "://" + request.getServerName() + port;

        String sessionRewrite = getSessionUrlRewrite(request);

        if (!relativePath.startsWith("/")) relativePath = "/" + relativePath;

        if (queryString == null) queryString = "";

        String absolute =
            base + context + relativePath + sessionRewrite + queryString;
        return absolute;
      }

    };
  }


  public static void completeRelativeUrls(StringBuilder sb, String urlFieldName,
      UrlBuilder urlBuilder) {

    int st = sb.indexOf("\"" + urlFieldName + "\"", 0);
    while (st >= 0) {

      int su = sb.indexOf("\"", st + urlFieldName.length() + 2);
      int eu = sb.indexOf("\"", su + 1);

      String u = sb.substring(su + 1, eu);

      String nu = urlBuilder.build(u, null);

      sb.replace(su, eu + 1, "\"" + nu + "\"");

      st = sb.indexOf("\"" + urlFieldName + "\"", su + nu.length() + 2);
    }

  }

}
