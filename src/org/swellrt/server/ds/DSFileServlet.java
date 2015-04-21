package org.swellrt.server.ds;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.apache.commons.codec.binary.Base64;
import org.waveprotocol.wave.util.logging.Log;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Servlet to handle binary files via HTTP
 *
 * Experimental service for Decentralized-Sharing Working Group (DSWG)
 *
 *
 * @author pablojan@gmail.com
 *
 */
@SuppressWarnings("serial")
@Singleton
public class DSFileServlet extends HttpServlet {

  private static final Log LOG = Log.get(DSFileServlet.class);

  @Inject
  public DSFileServlet() {

  }


  /**
   * Puts a file into this server with basic authorization
   * 
   * <pre>
   * curl -vX PUT http://localhost:9898/shared/file.txt
   * --data-binary @file.txt
   * -H "Authorization: Basic YXVzZXJuYW1lOmFwYXNzd29yZA=="
   * -H "Content-Type: text/plain"
   * </pre>
   * 
   */
  @Override
  protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {


    try {

      // Check auth
      checkCredentials(req);

      // Check if a place to store the file is in the URL
      String path = req.getPathInfo();
      if (path == null) {
        throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST);
      }

      // TODO match path with a destination for the file


      // Process file content

      // req.getContentType();

      BufferedInputStream inputBuffer = new BufferedInputStream(req.getInputStream());
      byte[] readBuffer = new byte[128];
      int r = 0;
      int totalBytes = 0;
      while ((r = inputBuffer.read(readBuffer)) != -1) {
        // do something with data
        totalBytes += r;
      }

      LOG.info("Received " + totalBytes + " bytes for file " + path);


    } catch (HttpResponseException e) {

      for (Entry<String, String> h : e.getHeaders().entrySet())
        resp.addHeader(h.getKey(), h.getValue());

      resp.sendError(e.getHttpResponseCode());

    } catch (IOException e) {

      resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    }

    resp.setStatus(HttpServletResponse.SC_OK);
  }

  protected void checkCredentials(HttpServletRequest req) throws HttpResponseException {

    String username = null;
    String password = null;

    String authHeader = req.getHeader("Authorization");

    if (authHeader != null) {
      StringTokenizer st = new StringTokenizer(authHeader);

      if (st.hasMoreTokens()) {

        try {

          String basic = st.nextToken();

          if (basic.equalsIgnoreCase("Basic")) {

            String credentials = "";

            credentials = new String(Base64.decodeBase64(st.nextToken()), "UTF-8");


            int p = credentials.indexOf(":");
            if (p != -1) {
              username = credentials.substring(0, p).trim();
              password = credentials.substring(p + 1).trim();
            } else {
              throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST);
            }

          }

        } catch (IndexOutOfBoundsException e) {
            throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST);

          } catch (UnsupportedEncodingException e) {
            throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST);

          } catch (NoSuchElementException e) {
            throw new HttpResponseException(HttpServletResponse.SC_BAD_REQUEST);
          }


        }

    }


    if (username == null || username.isEmpty()) {
      // TODO check if this is the rigt response for a bad formed request
      throw new HttpResponseException(HttpServletResponse.SC_FORBIDDEN).addHeader(
          "WWW-Authenticate", "Basic realm=\"swell\"");
    }

    // TODO login with username and password


  }

}
