package org.swellrt.server.box.servlet;


public interface UrlBuilder {

  public String build(String relativePath, String queryString);

}
