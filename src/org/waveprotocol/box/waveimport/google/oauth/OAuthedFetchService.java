/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.box.waveimport.google.oauth;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;

/**
 * A fetch service that signs fetch requests with OAuth2.
 *
 * @author hearnden@google.com (David Hearnden)
 * @author ohler@google.com (Christian Ohler)
 */
public final class OAuthedFetchService {

  /**
   * Detects whether an HTTP response indicates that the OAuth token has
   * expired.
   */
  public interface TokenRefreshNeededDetector {
    boolean refreshNeeded(int responseCode, Header[] responseHeaders, byte[] responseContent) throws IOException;
  }

  public static final TokenRefreshNeededDetector RESPONSE_CODE_401_DETECTOR =
      new TokenRefreshNeededDetector() {
        @Override public boolean refreshNeeded(int responseCode, Header[] responseHeaders, byte[] responseContent) {
          return responseCode == 401;
        }
      };

  @SuppressWarnings("unused")
  private static final Logger log = Logger.getLogger(OAuthedFetchService.class.getName());

  private final HttpClient fetch;
  private final OAuthRequestHelper helper;

  @Inject
  public OAuthedFetchService(HttpClient fetch, OAuthRequestHelper helper) {
    this.fetch = fetch;
    this.helper = helper;
  }

  private String describeRequest(EntityEnclosingMethod req) throws URIException {
    StringBuilder b = new StringBuilder(req.getName() + " " + req.getURI());
    for (Header h : req.getRequestHeaders()) {
      b.append("\n" + h.getName() + ": " + h.getValue());
    }
    return "" + b;
  }

  private String describeResponse(int responseCode, Header[] responseHeaders, byte[] responseBody, boolean includeBody) {
    StringBuilder b = new StringBuilder(responseCode
        + " with " + responseBody.length + " bytes of content");
    for (Header h : responseHeaders) {
      b.append("\n" + h.getName() + ": " + h.getValue());
    }
    if (includeBody) {
      b.append("\n" + new String(responseBody, Charsets.UTF_8));
    } else {
      b.append("\n<content elided>");
    }
    return "" + b;
  }

  private int fetch1(EntityEnclosingMethod req, TokenRefreshNeededDetector refreshNeeded,
      boolean tokenJustRefreshed) throws IOException {
    log.info("Sending request (token just refreshed: " + tokenJustRefreshed + "): "
        + describeRequest(req));
    helper.authorize(req);
    //log.info("req after authorizing: " + describeRequest(req));
    int code = fetch.executeMethod(req);
    log.info("response: " + describeResponse(code, req.getResponseHeaders(), req.getResponseBody(), false));
    if (refreshNeeded.refreshNeeded(code, req.getResponseHeaders(), req.getResponseBody())) {
      if (tokenJustRefreshed) {
        throw new NeedNewOAuthTokenException("Token just refreshed, still no good: "
            + describeResponse(code, req.getResponseHeaders(), req.getResponseBody(), true));
      } else {
        helper.refreshToken();
        return fetch1(req, refreshNeeded, true);
      }
    } else {
      return code;
    }
  }

  public int fetch(EntityEnclosingMethod request, TokenRefreshNeededDetector refreshNeeded)
      throws IOException {
    return fetch1(request, refreshNeeded, false);
  }

  public int fetch(EntityEnclosingMethod request) throws IOException {
    return fetch(request, RESPONSE_CODE_401_DETECTOR);
  }

  // TODO(ohler): Move these static utility methods to some other utility class.

  /** Gets the values of all headers with the name {@code headerName}. */
  public static List<String> getHeaders(Header[] respHeaders, String headerName) {
    ImmutableList.Builder<String> b = ImmutableList.builder();
    for (Header h : respHeaders) {
      // HTTP header names are case-insensitive.  App Engine downcases them when
      // deployed but not when running locally.
      if (headerName.equalsIgnoreCase(h.getName())) {
        b.add(h.getValue());
      }
    }
    return b.build();
  }

  /**
   * Checks that exactly one header named {@code headerName} is present and
   * returns its value.
   */
  public static String getSingleHeader(Header[] respHeaders, String headerName) {
    return Iterables.getOnlyElement(getHeaders(respHeaders, headerName));
  }

  /** Returns the body of {@code resp}, assuming that its encoding is UTF-8. */
  private static String getUtf8ResponseBodyUnchecked(byte[] respContent) {
    if (respContent == null) {
      return "";
    } else {
      return new String(respContent, Charsets.UTF_8);
    }
  }

  /**
   * Checks that the Content-Type of {@code resp} is
   * {@code expectedUtf8ContentType} (which is assumed to imply UTF-8 encoding)
   * and returns the body as a String.
   */
  public static String getUtf8ResponseBody(Header[] respHeaders, byte[] respContent, String expectedUtf8ContentType)
      throws IOException {
    String contentType = getSingleHeader(respHeaders, "Content-Type");
    if (!expectedUtf8ContentType.equals(contentType)) {
      throw new IOException("Unexpected Content-Type: " + contentType
          + " (wanted " + expectedUtf8ContentType + "); body as UTF-8: "
          + getUtf8ResponseBodyUnchecked(respContent));
    }
    return getUtf8ResponseBodyUnchecked(respContent);
  }

}
