package org.waveprotocol.box.server.rpc;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.inject.Singleton;

@Singleton
public class TransientSessionFilter  implements Filter {

  public static final String REQUEST_ATTR_TSESSION_ID = "org.waveprotocol.box.server.rpc.TransientSessionId";

  public static final String DEFAULT_COOKIE_NAME = "TSESSIONID";
  public static final String DEFAULT_COOKIE_DOMAIN = "";

  public static final String PARAM_COOKIE_DOMAIN = "CookieDomain";
  public static final String PARAM_COOKIE_NAME = "CookieName";

  private static final String QUERY_PARAM_NAME = "tid";

  private String confCookieName = DEFAULT_COOKIE_NAME;
  private String confCookieDomain = "";

  private Random _random;
  private boolean _weakRandom;


  public TransientSessionFilter() {
    super();
  }

  /*
   * Taken from Jetty
   */
  private void initRandom() {
    if (_random == null) {
      try {
        _random = new SecureRandom();
      } catch (Exception e) {
        _random = new Random();
        _weakRandom = true;
      }
    } else
      _random.setSeed(_random.nextLong() ^ System.currentTimeMillis() ^ hashCode()
          ^ Runtime.getRuntime().freeMemory());

  }

  private String newId(HttpServletRequest request, long created) {

    synchronized (this) {

      // pick a new unique ID!
      String id = null;
      while (id == null || id.length() == 0) {
        long r0 = _weakRandom ? (hashCode() ^ Runtime.getRuntime().freeMemory() ^ _random.nextInt()
            ^ (((long) request.hashCode()) << 32)) : _random.nextLong();
        if (r0 < 0)
          r0 = -r0;
        long r1 = _weakRandom ? (hashCode() ^ Runtime.getRuntime().freeMemory() ^ _random.nextInt()
            ^ (((long) request.hashCode()) << 32)) : _random.nextLong();
        if (r1 < 0)
          r1 = -r1;
        id = Long.toString(r0, 36) + Long.toString(r1, 36);
      }

      return id;
    }

  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {
    initRandom();
    String initParam = filterConfig.getInitParameter(PARAM_COOKIE_NAME);
    confCookieName = initParam != null ? initParam : DEFAULT_COOKIE_NAME;

    initParam = filterConfig.getInitParameter(PARAM_COOKIE_DOMAIN);
    confCookieDomain = initParam != null ? initParam : DEFAULT_COOKIE_DOMAIN;
  }

  protected Cookie getTSCookie(Cookie[] cookies) {

    Cookie tsCookie = null;
    for (Cookie c:cookies) {
      if (c.getName().equals(DEFAULT_COOKIE_NAME))
        tsCookie = c;
    }

    return tsCookie;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    Cookie tsCookie = req.getCookies() != null ? getTSCookie(req.getCookies()) : null;

    String tsParam = null;
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
        tsParam = s;
    }


    String tsessionId = null;

    if (tsCookie == null) {
      if (tsParam == null) {
        tsessionId = newId(req, System.currentTimeMillis());
        tsCookie = new Cookie(confCookieName, tsessionId);
        tsCookie.setDomain(confCookieDomain);;
        tsCookie.setMaxAge(-1);
        res.addCookie(tsCookie);
      } else {
        tsessionId = tsParam;
      }
    } else {
       tsessionId = tsCookie.getValue();
    }

    request.setAttribute(REQUEST_ATTR_TSESSION_ID, tsessionId);

    try {
      chain.doFilter(request, response);
    } finally {
    }

  }

  @Override
  public void destroy() {
  }

}
