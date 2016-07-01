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
    JSONObject packet = sendPacket(MatrixUtil.syncRequest(syncTime));
    syncTime = packet.getString("next_batch");

    System.out.println(packet);
  }

  @Override
  public JSONObject sendPacket(Request packet) {
    try {
      BaseRequest request = formRequest(packet);
      HttpResponse<JsonNode> jsonResponse = request.asJson();
      return jsonResponse.getBody().getObject();
    }catch (Exception e)
    ;
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
    synchronized (connectionLock) {
      httpConfig();
      try {
        JSONObject rooms = getRooms();
        findMinTS(rooms);
      } catch (Exception e)
      ;

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

    MatrixUtil.access_token = appServiceToken;
  }

  private JSONObject getRooms() throws Exception {

    JSONObject sync = sendPacket(MatrixUtil.syncRequest());

    JSONObject joinedRooms = sync.getJSONObject("rooms").getJSONObject("join");

    return joinedRooms;
  }

  private void findMinTS(JSONObject rooms) throws Exception {
    
    long min_ts = Long.MAX_VALUE;
    String min_eventid = null;
    String min_roomid = null;

    Iterator<String> room_it = rooms.keys();

    while(room_it.hasNext()) {
      String roomId = room_it.next();

      if(roomId.split(":")[1].equals(serverDomain)) {
        JSONObject roomInfo = rooms.getJSONObject(roomId);
        
        JSONArray arr = roomInfo.getJSONObject("ephemeral").getJSONArray("events");

        for (int i=0; i < arr.length(); i++) {
            JSONObject x = arr.getJSONObject(i);
            
            if(x.getString("type").equals("m.receipt")) {

              JSONObject content = x.getJSONObject("content");

              String eventId = ((Iterator<String>)(content.keys())).next();

              Long timestamp = content.getJSONObject(eventId).getJSONObject("m.read").getJSONObject(userId).getLong("ts");

              if(timestamp < min_ts) {
                min_ts = timestamp;
                min_roomid = roomId;
                min_eventid = eventId;
              }
            }

        }

      }
    }

    if(min_eventid!=null && min_roomid!=null)
      findSyncTime(min_roomid, min_eventid);
  }

  private void findSyncTime(String roomId, String eventId) throws Exception{
      JSONObject obj = sendPacket(MatrixUtil.syncTimeRequest(roomId, eventId));

      String nextID = obj.getString("end");

      syncTime = nextID;

      System.out.println("\n\n"+syncTime+"\n\n");
   
  }

}