package org.waveprotocol.box.rest.dsharing;

import java.util.HashMap;
import java.util.Map;


@SuppressWarnings("serial")
public class HttpResponseException extends Exception {


  private final int httpResponseCode;
  private Map<String, String> headers;

  public HttpResponseException(int httpResponseCode) {
    super("");
    this.httpResponseCode = httpResponseCode;
    this.headers = new HashMap<String, String>();

  }

  public HttpResponseException(int httpResponseCode,
      String message) {
    super(message);
    this.httpResponseCode = httpResponseCode;
  }

  public int getHttpResponseCode() {
    return httpResponseCode;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public HttpResponseException addHeader(String name, String value) {
    headers.put(name, value);
    return this;
  }

}
