/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.wave.federation.matrix;

import com.google.inject.Inject;
import com.mashape.unirest.http.*;
import com.mashape.unirest.request.*;
import com.typesafe.config.Config;

import org.apache.http.conn.ssl.*;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import javax.net.ssl.*;

/**
 * Talks to a Matrix server using the Client-Server API.
 *
 * Implements {@link OutgoingPacketTransport} allowing users to send packets,
 * and accepts an {@link IncomingPacketHandler} which can process incoming
 * packets.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
public class AppServicePacketTransport implements Runnable, OutgoingPacketTransport {
	
  private static final Logger LOG = 
      Logger.getLogger(AppServicePacketTransport.class.getCanonicalName());

  private final IncomingPacketHandler handler;
  private final String apiAddress;
  private final String appServiceName;
  private final String appServiceToken;
  private final String serverDomain;
  private final String serverAddress;

  // Contains packets queued but not sent (while offline).
  private final Queue<Request> queuedPackets;

  // Object used to lock around online/offline state changes.
  private final Object connectionLock = new Object();

  private boolean connected = false;

  @Inject
  public AppServicePacketTransport(IncomingPacketHandler handler, Config config) {
    this.handler = handler;
    this.apiAddress = config.getString("federation.matrix_api_address");
    this.appServiceName = config.getString("federation.matrix_appservice_name");
    this.appServiceToken = config.getString("federation.matrix_appservice_token");
    this.serverDomain = config.getString("federation.matrix_server_hostname");
    this.serverAddress = config.getString("federation.matrix_server_ip");

    queuedPackets = new LinkedList<>();
  }

  @Override
  public void run() {
  	setUp();
    Request req = new Request("GET", "/sync");
    req.addQueryString("access_token", "wfghWEGh3wgWHEf3478sHFWE");
    
    try {
      sendPacket(req);
    } catch (Exception e) {
      System.out.println("\n\n" + e+"\n\n");
    }
  }

  @Override
  public void sendPacket(Request packet) throws Exception {

    BaseRequest request = formRequest(packet);

    HttpResponse<JsonNode> jsonResponse = request.asJson();
    JSONObject myObj = jsonResponse.getBody().getObject();

    System.out.println(myObj);
  }

  private BaseRequest formRequest(Request packet) {
    HttpRequest httpRequest = null;

    String url = "https://" + serverAddress + apiAddress + packet.getUrl();

    String method = packet.getMethod();
    switch (method) {
      case "GET":
        httpRequest = Unirest.get(url);
        break;

      case "POST":
        httpRequest = Unirest.post(url);
        break;

      case "PUT":
        httpRequest = Unirest.put(url);
        break;
    }

    Map<String, String> headers = packet.getHeaders();
    httpRequest = httpRequest.headers(headers);

    Map<String, Object> queryStrings = packet.getQueryStrings();
    httpRequest = httpRequest.queryString(queryStrings);

    BaseRequest request = httpRequest;

    if(!method.equals("GET")) {
      JSONObject body = packet.getBody();
      request = ((HttpRequestWithBody)request).body(body.toString());
    }
    
    return request;
  } 

  private void setUp() {
    try {
      httpConfig();
    } catch (Exception e) {
      System.out.println("\n\n" + e +"\n\n");
    }
  }

  private void httpConfig() throws Exception {

    RequestConfig requestConfig = RequestConfig.custom()
        .setSocketTimeout(10000)
        .setConnectTimeout(10000)
        .build();

    SSLContext sslcontext = SSLContexts.custom()
        .loadTrustMaterial(null, new TrustSelfSignedStrategy())
        .build();

    CloseableHttpClient httpclient = HttpClients.custom()
        .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
        .setSSLContext(sslcontext)
        .build();
    Unirest.setHttpClient(httpclient);

    CloseableHttpAsyncClient client = HttpAsyncClients.custom()
        .setHostnameVerifier(SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
        .setSSLContext(sslcontext)
        .setDefaultRequestConfig(requestConfig)
        .setMaxConnPerRoute(1000)
        .setMaxConnTotal(1000)
        .build();

    Unirest.setAsyncHttpClient(client);

    Unirest.setDefaultHeader("Content-Type","application/json");
  }

}