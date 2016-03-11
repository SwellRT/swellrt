package org.swellrt.server.box.servlet;

import junit.framework.TestCase;

public class ServiceUtilsTest extends TestCase {

  protected void setUp() throws Exception {
    super.setUp();
  }

  public void testCompleteRelativeUrls() {

    StringBuilder sb =
        new StringBuilder(
            "{ \"property\" : \"value\", \"url\" : \"/myservice/path/content.txt\", \"property\" : \"value\" , \"url\" : \"/myservice/path/data.txt\" }");

    UrlBuilder ub = new UrlBuilder() {

      @Override
      public String build(String relativePath, String queryString) {
        return "http://server.com" + relativePath;
      }
    };

    ServiceUtils.completeRelativeUrls(sb, "url", ub);

    assertTrue(sb.toString().contains("\"http://server.com/myservice/path/content.txt\""));
    assertTrue(sb.toString().contains("\"http://server.com/myservice/path/data.txt\""));

  }

}
