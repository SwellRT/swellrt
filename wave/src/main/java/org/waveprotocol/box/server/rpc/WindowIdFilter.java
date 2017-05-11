package org.waveprotocol.box.server.rpc;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import com.google.inject.Singleton;

/**
 * A Servlet filter to handle browser window's ids.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@Singleton
public class WindowIdFilter implements Filter {


  public static final String HEADER_NAME = "X-window-id";
  public static final String QUERY_PARAM_NAME = "wid";

  public static final String REQUEST_ATTR_WINDOW_ID =
      "org.waveprotocol.box.server.rpc.windowId";

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {


    HttpServletRequest req = (HttpServletRequest) request;


    if (req.getQueryString() != null
        && req.getQueryString().contains(QUERY_PARAM_NAME + "=")) {
      String s = req.getQueryString();
      s =
          s.substring(s.indexOf(QUERY_PARAM_NAME + "=")
          + QUERY_PARAM_NAME.length() + 1);

      if (s.contains("&")) {
        s = s.substring(0, s.indexOf("&"));
      }

      if (s != null && !s.isEmpty())
    	  req.setAttribute(REQUEST_ATTR_WINDOW_ID, s);

    }

    // The header always has priority, overwrites the query parameter
    String windowId = req.getHeader(HEADER_NAME);

    if (windowId != null) {
        req.setAttribute(REQUEST_ATTR_WINDOW_ID, windowId);
    }

    try {
      chain.doFilter(request, response);
    } finally {
    }

  }

  @Override
  public void destroy() {

  }

}
