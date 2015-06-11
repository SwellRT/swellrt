package org.swellrt.android.service;

import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import android.util.Log;

public class WaveHttpRegister {
  private static String REGISTER_CTX = "auth/register";
  private static String CHARSET = "utf-8";


  private String host;
  private String username;
  private String password;

  public WaveHttpRegister(String host, String username, String password) {
    this.host = host;
    this.username = username;
    this.password = password;
  }

  public boolean execute() {

    boolean result = false;
    String urlStr = host.endsWith("/") ? host + REGISTER_CTX : host + "/" + REGISTER_CTX;

    String queryStr = "";
    try {
      queryStr = "address=" + URLEncoder.encode(username, CHARSET) + "&password="
          + URLEncoder.encode(password, CHARSET);
    } catch (UnsupportedEncodingException e) {
      Log.e(WaveHttpRegister.class.getSimpleName(), "Error Registering User", e);
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
            WaveHttpRegister.class.getSimpleName(),
            "Error Registering User" + connection.getResponseCode() + ", "
            + connection.getResponseMessage());
      } else
        result = true;

    } catch (Exception e) {
      Log.e(WaveHttpRegister.class.getSimpleName(), "Error Registering User", e);
    } finally {
      connection.disconnect();
    }

    return result;

  }

}
