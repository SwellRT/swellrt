package org.waveprotocol.box.server.swell;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

public class ServletUtils {

  public final static long NO_CACHE = 0;
  public final static long CACHE_24H = 86400; // seconds

  public static Map<String, String> getUrlParams(HttpServletRequest req, List<String> paramNames) {

    Map<String, String> map = new HashMap<String, String>();

    try {
      String pathInfo = req.getPathInfo().substring(1);
      String[] tokens = pathInfo.split("/");

      for (int i = 0; i < tokens.length; i++) {
        map.put(paramNames.get(i), tokens[i]);
      }

    } catch (Exception e) {

    }

    return map;
  }

  public static void responseJson(HttpServletResponse response, long maxAgeSeconds) {

    response.setStatus(HttpServletResponse.SC_OK);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    if (maxAgeSeconds == 0)
      response.setHeader("Cache-Control", "no-store");
    else
      response.setHeader("Cache-control", "public, max-age=" + String.valueOf(maxAgeSeconds));

  }

  public static void responseJson(HttpServletResponse response, String json, long maxAgeSeconds)
      throws IOException {

    responseJson(response, maxAgeSeconds);
    response.getWriter().append(json);

  }

  public static JsonWriter responseJsonWriter(HttpServletResponse response, long maxAgeSeconds)
      throws IOException {

    responseJson(response, maxAgeSeconds);
    return new JsonWriter(response.getWriter());
  }

  public static void responseJsonEmpty(HttpServletResponse response) throws IOException {
    responseJsonWriter(response, 0).beginObject().endObject();
  }

  public static void responseJsonObject(HttpServletResponse response, JsonObject data, long maxAgeSeconds)
      throws IOException {

    responseJson(response, maxAgeSeconds);

    Gson gson = new Gson();
    String stringResponse = gson.toJson(data);
    response.getWriter().append(stringResponse);
  }

  public static void responseXml(HttpServletResponse response, String xml, long maxAgeSencods)
      throws IOException {

    responseJson(response, maxAgeSencods);
    response.getWriter().append(xml);
  }

  public static void responseInternalError(HttpServletResponse response, String message)
      throws IOException {
    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, message);
  }

  public static void responseBadRequest(HttpServletResponse response, String message)
      throws IOException {
    response.sendError(HttpServletResponse.SC_BAD_REQUEST, message);
  }

}
