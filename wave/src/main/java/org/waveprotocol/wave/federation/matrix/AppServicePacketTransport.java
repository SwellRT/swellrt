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
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Iterator;
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

  static final int MATRIX_SOCKET_TIMEOUT = 10000;
  static final int MATRIX_CONNECT_TIMEOUT = 10000;
  static final int MATRIX_RECONNECT_TIMEOUT = 20000;

  private final IncomingPacketHandler handler;
  private final String userId;
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
    this.userId = config.getString("federation.matrix_id");
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
  }

  @Override
  public void sendPacket(Request packet) {
    // packet.addQueryString("access_token", matrix_appservice_token);

    // BaseRequest request = formRequest(packet);

    // Future<HttpResponse<JsonNode>> future jsonResponse = request.asJsonAsync(
    //     new Callback<JsonNode>() {

    //       public void completed(HttpResponse<JsonNode> response) {
    //            JSONObject myObj = response.getBody().getObject();

    //         System.out.println("\n\n\n"+myObj);
    //       }

    //       public void failed(UnirestException e) {
    //           System.out.println("The request has failed" + e);
    //       }

    // });

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
      httpConfig();
      start();
  }

  private void httpConfig() {
    LOG.info("Setting up http Matrix Federation for id: " + userId);

    RequestConfig requestConfig = RequestConfig.custom()
        .setSocketTimeout(MATRIX_SOCKET_TIMEOUT)
        .setConnectTimeout(MATRIX_CONNECT_TIMEOUT)
        .build();
    SSLContext sslcontext;

    try {
      sslcontext = SSLContexts.custom()
          .loadTrustMaterial(null, new TrustSelfSignedStrategy())
          .build();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

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

  private void start() {
    synchronized (connectionLock) {
      Request req = new Request("GET", "/sync");
      req.addQueryString("access_token", appServiceToken);
      BaseRequest request = formRequest(req);
      try {
        HttpResponse<JsonNode> jsonResponse = request.asJson();

        JSONObject sync = jsonResponse.getBody().getObject();

        JSONObject joinedRooms = sync.getJSONObject("rooms").getJSONObject("join");

        long minimum_ts = Long.MAX_VALUE;
        String minimum_eventid = null;
        String minimum_roomid = null;

        Iterator<String> room_it = joinedRooms.keys();

        while(room_it.hasNext()) {
          String roomId = room_it.next();

          if(roomId.split(":")[1].equals(serverDomain)) {
            JSONObject roomInfo = joinedRooms.getJSONObject(roomId);
            
            JSONArray arr = roomInfo.getJSONObject("ephemeral").getJSONArray("events");

            for (int i=0; i < arr.length(); i++) {
                JSONObject x = arr.getJSONObject(i);
                
                if(x.getString("type").equals("m.receipt")) {

                  JSONObject content = x.getJSONObject("content");

                  String eventid = ((Iterator<String>)(content.keys())).next();

                  Long timestamp = content.getJSONObject(eventid).getJSONObject("m.read").getJSONObject(userId).getLong("ts");

                  if(timestamp < minimum_ts) {
                    minimum_ts = timestamp;
                    minimum_roomid = roomId;
                    minimum_eventid = eventid;
                  }
                }

            }

          }
        }

        if(minimum_eventid!=null && minimum_roomid!=null) {
          req = new Request("GET", "/rooms/" + minimum_roomid + "/context/" + minimum_eventid);
          req.addQueryString("limit", "0");
          req.addQueryString("access_token", appServiceToken);
          request = formRequest(req);
          
          jsonResponse = request.asJson();

          JSONObject obj = jsonResponse.getBody().getObject();

          String nextID = obj.getString("end");

          System.out.println("\n\n"+nextID+"\n\n");
      
        }
      } catch (Exception ex) {
          System.out.println("\n\n"+ex+"\n\n");
      }

      
    }
  }

}