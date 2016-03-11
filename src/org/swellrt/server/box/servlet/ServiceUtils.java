package org.swellrt.server.box.servlet;

import com.google.common.base.Preconditions;

import javax.servlet.http.HttpServletRequest;

public class ServiceUtils {


  public static String getSessionUrlRewrite(HttpServletRequest request) {
    Preconditions.checkNotNull(request, "Request can't be null");

    if (request.getPathInfo() == null || request.getPathInfo().isEmpty()) return "";

    // The ';sid=' syntax is jetty specific.
    int indexSid = request.getPathInfo().indexOf(";sid=");

    if (indexSid >= 0) {
      return request.getPathInfo().substring(indexSid, request.getPathInfo().length());
    }

    return "";
  }


  public static UrlBuilder getUrlBuilder(final HttpServletRequest request) {
    return getUrlBuilder(request, SwellRtServlet.SERVLET_CONTEXT);
  }

  public static UrlBuilder getUrlBuilder(final HttpServletRequest request, final String context) {

    return new UrlBuilder() {

      @Override
      public String build(String relativePath, String queryString) {
        Preconditions.checkNotNull(relativePath, "Path can't be null");

        String base =
            request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort();

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

    int st = sb.indexOf("\"" + urlFieldName, 0);
    while (st >= 0) {

      int su = sb.indexOf("\"", st + urlFieldName.length() + 2);
      int eu = sb.indexOf("\"", su + 1);

      String u = sb.substring(su + 1, eu);

      String nu = urlBuilder.build(u, null);

      sb.replace(su, eu + 1, "\"" + nu + "\"");

      st = sb.indexOf(urlFieldName, su + nu.length() + 2);
    }

  }

}
