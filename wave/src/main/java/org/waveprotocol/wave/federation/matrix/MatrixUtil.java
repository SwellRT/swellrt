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

import org.json.JSONObject;

/**
 * Common utility code for Matrix JSON packet generation and parsing.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
public class MatrixUtil {

  public static String access_token = null;

  public static Request syncRequest() {
    Request request = new Request("GET", "/sync");
    request.addQueryString("access_token", access_token);
    return request;
  }

  public static Request syncRequest(String syncTime) {
    Request request = new Request("GET", "/sync");
    request.addQueryString("access_token", access_token);
    request.addQueryString("since", syncTime);
    request.addQueryString("timeout", "30000");
    return request;
  }

  public static Request syncTimeRequest(String roomId, String eventId) {
    Request request = new Request("GET", "/rooms/" + roomId + "/context/" + eventId);
    request.addQueryString("limit", "0");
    request.addQueryString("access_token", access_token);
    return request;
  }

  public static Request joinRoom(String roomId) {
    Request request = new Request("POST", "/rooms/" + roomId + "/join");
    request.addQueryString("access_token", access_token);
    return request;
  }

  public static String encodeDomain(String domain) {
    return domain.replace(":", "%3A");
  }

  public static String decodeDomain(String domain) {
    return domain.replace("%3A", ":");
  }

}