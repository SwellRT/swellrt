package org.swellrt.server.box.servlet;

import javax.servlet.http.HttpServletRequest;
import org.waveprotocol.box.server.authentication.SessionManager;

import com.google.common.base.Preconditions;

public class ServiceUtils {

  
  public static String getSessionUrlRewrite(HttpServletRequest request) {
   if (SessionManager.hasSessionCookie(request))
     return "";
   else 
     return SessionManager.getSessionStringFromPath(request);
      
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
