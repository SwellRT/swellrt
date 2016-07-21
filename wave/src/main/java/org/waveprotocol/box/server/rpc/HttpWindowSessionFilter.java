package org.waveprotocol.box.server.rpc;

import com.google.inject.Singleton;

import org.waveprotocol.box.server.authentication.HttpWindowSession;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * A Servlet filter to handle window-session ids. See {@link HttpWindowSession}
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 * 
 */
@Singleton
public class HttpWindowSessionFilter implements Filter {

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {


    HttpServletRequest req = (HttpServletRequest) request;


    if (req.getQueryString() != null
        && req.getQueryString().contains(HttpWindowSession.WINDOW_SESSION_PARAMETER_NAME + "=")) {
      String s = req.getQueryString();
      s =
          s.substring(s.indexOf(HttpWindowSession.WINDOW_SESSION_PARAMETER_NAME + "=")
          + HttpWindowSession.WINDOW_SESSION_PARAMETER_NAME.length() + 1);

      if (s.contains("&")) {
        s = s.substring(0, s.indexOf("&"));
      }

      if (s != null && !s.isEmpty())
    	  req.setAttribute(HttpWindowSession.WINDOW_SESSION_REQUEST_ATTR, s);

    }

    // The header always has priority, overwrites the query parameter
    String windowId = req.getHeader(HttpWindowSession.WINDOW_SESSION_HEADER_NAME);

    if (windowId != null) {
        req.setAttribute(HttpWindowSession.WINDOW_SESSION_REQUEST_ATTR, windowId);
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
