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

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.inject.Inject;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.request.BaseRequest;
import com.mashape.unirest.request.HttpRequest;
import com.mashape.unirest.request.HttpRequestWithBody;
import com.typesafe.config.Config;

/**
 * Talks to a Matrix server using the Client-Server API.
 *
 * Implements {@link OutgoingPacketTransport} allowing users to send packets,
 * and accepts an {@link IncomingPacketHandler} which can process incoming
 * packets.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
@SuppressWarnings("deprecation")
public class AppServicePacketTransport implements Runnable, OutgoingPacketTransport {

  private static final Logger LOG =
      Logger.getLogger(AppServicePacketTransport.class.getCanonicalName());

  static final int MATRIX_SOCKET_TIMEOUT = 10000;
  static final int MATRIX_CONNECT_TIMEOUT = 10000;
  static final int MATRIX_RECONNECT_TIMEOUT = 20000;

  private final IncomingPacketHandler handler;
  private final String userId;
  private final String userName;
  private final String userPass;
  private final String apiAddress;
  private final String appServiceName;
  private final String appServiceToken;
  private final String serverDomain;
  private final String serverAddress;

  private String syncTime;

  // Contains packets queued but not sent (while offline).
  private final Queue<Request> queuedPackets;

  // Object used to lock around online/offline state changes.
  private final Object connectionLock = new Object();

  private boolean connected = false;

  @Inject
  public AppServicePacketTransport(IncomingPacketHandler handler, Config config) {
    this.handler = handler;
    this.userId = config.getString("federation.matrix_id");
    this.userName = config.getString("federation.matrix_name");
    this.userPass = config.getString("federation.matrix_pass");
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
    while(true)
      processPacket();

  }

  public void processPacket() {
    try {
      JSONObject packets = sendPacket(MatrixUtil.syncRequest(syncTime));
      System.out.println("\n\n\n packet:-\n"+packets+"\n\n");
      handler.receivePacket(packets);
      syncTime = packets.getString("next_batch");



    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public JSONObject sendPacket(Request packet) {
    try {
      BaseRequest request = formRequest(packet);
      HttpResponse<JsonNode> jsonResponse = request.asJson();
      if(jsonResponse.getStatus() == 400 || jsonResponse.getStatus() == 404
        || jsonResponse.getStatus() == 500)
        return null;
      else
        return jsonResponse.getBody().getObject();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
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
    login();
    initialSync();
  }

  private void login() {
    try {
      Request request = MatrixUtil.login();
      request.addBody("type", "m.login.password");
      request.addBody("user", userName);
      request.addBody("password", userPass);
      JSONObject credentials = sendPacket(request);
      MatrixUtil.access_token = credentials.getString("access_token");
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void initialSync() {
    try{
      JSONObject sync = sendPacket(MatrixUtil.syncRequest());
      syncTime = sync.getString("next_batch");
      JSONObject rooms = sync.getJSONObject("rooms");
      if(rooms.has("join"))
        rooms.remove("join");
      handler.receivePacket(sync);
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  private void httpConfig() {
    LOG.info("Setting up http Matrix Federation for id: " + userId);

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

    Unirest.setDefaultHeader("Content-Type","application/json");
  }

  // private void findMinTS() throws Exception {

  //   long min_ts = Long.MAX_VALUE;
  //   String min_eventid = null;
  //   String min_roomid = null;



  //   JSONObject rooms = sync.getJSONObject("rooms").getJSONObject("join");

  //   Iterator<String> room_it = rooms.keys();

  //   while(room_it.hasNext()) {
  //     String roomId = room_it.next();

  //     if(roomId.split(":", 2)[1].equals(serverDomain)) {
  //       JSONObject roomInfo = rooms.getJSONObject(roomId);

  //       JSONArray arr = roomInfo.getJSONObject("timeline").getJSONArray("events");

  //       for (int i=0; i < arr.length(); i++) {
  //         JSONObject x = arr.getJSONObject(i);

  //         if(x.getString("type").equals("m.room.message.feedback") ) {


  //           String eventId = x.getString("event_id");

  //           Long timestamp = x.getLong("origin_server_ts");

  //           if(timestamp < min_ts) {
  //             min_ts = timestamp;
  //             min_roomid = roomId;
  //             min_eventid = eventId;
  //           }
  //         }

  //       }

  //     }
  //   }

  //   if(min_eventid!=null && min_roomid!=null)
  //     findSyncTime(min_roomid, min_eventid);
  // }

  // private void findSyncTime(String roomId, String eventId) throws Exception {
  //   System.out.println("\n\n"+eventId+"\n\n");
  //   JSONObject obj = sendPacket(MatrixUtil.syncTimeRequest(roomId, eventId));

  //   String nextID = obj.getString("end");

  //   syncTime = nextID;

  //   System.out.println("\n\n"+syncTime+"\n\n");

  // }

}