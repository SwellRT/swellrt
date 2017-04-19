package org.waveprotocol.box.server.rpc;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CacheHeaderFilter implements Filter {

  @Override
  public void destroy() {

  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final HttpServletResponse httpResponse = (HttpServletResponse) response;

    final String requestUri = httpRequest.getRequestURI();

    if (requestUri.matches(".+\\.nocache\\..+")) {
      httpResponse.addHeader("Cache-Control", "public, max-age=0, must-revalidate");
      httpResponse.addHeader("Pragma", "no-cache");
    }

    if (requestUri.matches(".+\\.cache\\..+")
        || (requestUri.matches("/swell/account/.+/avatar/.+") && httpRequest.getMethod().equals(
            "GET"))
        || (requestUri.matches("/attachment/.+") && httpRequest.getMethod().equals("GET"))
        || (requestUri.matches("/thumbnail/.+") && httpRequest.getMethod().equals("GET"))) {
      httpResponse.addHeader("Cache-Control", "max-age=31536000, public");
    }

    chain.doFilter(request, response);

  }

  @Override
  public void init(FilterConfig arg0) throws ServletException {

  }

}
