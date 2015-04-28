package org.swellrt.android.service;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import android.util.Log;

public class WaveHttpLogin {

  public static String WAVE_SESSION_COOKIE = "WSESSIONID";

  private static String LOGIN_CTX = "auth/signin?r=none";
  private static String CHARSET = "utf-8";


  private String host;
  private String username;
  private String password;

  public WaveHttpLogin(String host, String username, String password) {
    this.host = host;
    this.username = username;
    this.password = password;
  }

  public String execute() {

    String sessionId = null;

    String urlStr = host.endsWith("/") ? host + LOGIN_CTX : host + "/" + LOGIN_CTX;
    String queryStr = "";
    try {
      queryStr = "address=" + URLEncoder.encode(username, "UTF-8") + "&password="
          + URLEncoder.encode(password, CHARSET) + "&signIn="
          + URLEncoder.encode("Sign+in", CHARSET);

    } catch (UnsupportedEncodingException e) {
      Log.e(WaveHttpLogin.class.getSimpleName(), "Error in Wave Login", e);
    }

    HttpURLConnection connection = null;

    try {

      URL url = new URL(urlStr);
      connection = (HttpURLConnection) url.openConnection();

      connection.setDoOutput(true);
      connection.setRequestProperty("Accept-Charset", CHARSET);
      connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset="
          + CHARSET);

      OutputStream out = connection.getOutputStream();
      out.write(queryStr.getBytes(CHARSET));


      if (connection.getResponseCode() != 200) {
        Log.e(
            WaveHttpLogin.class.getSimpleName(),
            "HTTP Login response error " + connection.getResponseCode() + ", "
            + connection.getResponseMessage());
      } else {

        List<String> cookies = connection.getHeaderFields().get("Set-Cookie");

        for (String c : cookies) {
          if (c.startsWith(WAVE_SESSION_COOKIE)) {

            String cookie = c;

            if (cookie.contains(";"))
              cookie = cookie.split(";")[0];

            sessionId = cookie.split("=")[1];
            break;
          }
        }

        if (sessionId == null)
          Log.e(WaveHttpLogin.class.getSimpleName(), "Cookie session not found in HTTP response");

      }

    } catch (Exception e) {
      Log.e(WaveHttpLogin.class.getSimpleName(), "Error in Wave HTTP Login", e);
    } finally {
      connection.disconnect();
    }

    return sessionId;

  }

}
