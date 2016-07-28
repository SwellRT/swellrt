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

import com.google.common.annotations.VisibleForTesting;
import com.google.protobuf.ByteString;

import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Common utility code for Matrix JSON packet generation and parsing.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
public class MatrixUtil {

  public static String access_token = null;

  public static final IdURIEncoderDecoder waveletNameCodec =
      new IdURIEncoderDecoder(new JavaUrlCodec());

  /**
   * Convert the signer information to JSON and place the result within the
   * passed JSONObject. This method should never fail.
   */
  public static void protocolSignerInfoToJson(ProtocolSignerInfo signerInfo, JSONObject parent) {
    try {
      JSONObject signature = new JSONObject();
      parent.putOpt("signature", signature);
      signature.putOpt("domain", signerInfo.getDomain());
      ProtocolSignerInfo.HashAlgorithm hashValue = signerInfo.getHashAlgorithm();

      signature.putOpt("algorithm", hashValue.name());
      JSONArray certificate = new JSONArray();
      signature.putOpt("certificate", certificate);
      for (ByteString cert : signerInfo.getCertificateList()) {
        certificate.put(Base64Util.encode(cert));
      }
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Convert the given Element to a signer information JSONObject.
   *
   * @throws UnknownSignerType when the given hash algorithm is not understood
   */
  public static ProtocolSignerInfo jsonToProtocolSignerInfo(JSONObject signature) {
    ProtocolSignerInfo.HashAlgorithm hash;
    String algorithm = signature.optString("algorithm").toUpperCase();
    try {
      hash = ProtocolSignerInfo.HashAlgorithm.valueOf(algorithm);
    } catch (IllegalArgumentException ex) {
      throw new RuntimeException(ex);
    }

    ProtocolSignerInfo.Builder builder = ProtocolSignerInfo.newBuilder();
    builder.setHashAlgorithm(hash);
    builder.setDomain(signature.optString("domain"));
    JSONArray certificate = signature.optJSONArray("certificate");
    for (int i=0; i<certificate.length(); i++) {
      String cert = certificate.optString(i);
      builder.addCertificate(Base64Util.decode(cert));
    }
    return builder.build();
  }

  public static Request syncRequest() {
    Request request = new Request("GET", "/sync");
    request.addQueryString("access_token", access_token);
    return request;
  }

  public static Request syncRequest(String syncTime) {
    Request request = new Request("GET", "/sync");
    request.addQueryString("access_token", access_token);
    if(syncTime != null)
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

  public static Request createRoom() {
    Request request = new Request("POST", "/createRoom");
    request.addQueryString("access_token", access_token);
    return request;
  }

  public static Request joinRoom(String roomId) {
    Request request = new Request("POST", "/rooms/" + encodeRoomId(roomId) + "/join");
    request.addQueryString("access_token", access_token);
    return request;
  }

  public static Request getRoom(String roomAlias) {
    Request request = new Request("GET", "/directory/room/" + encodeRoomAlias(roomAlias));
    request.addQueryString("access_token", access_token);
    return request;
  }

  public static Request inviteRoom(String roomId) {
    Request request = new Request("POST", "/rooms/" + encodeRoomId(roomId) + "/invite");
    request.addQueryString("access_token", access_token);
    return request;
  }

  public static Request getMembers(String roomId) {
    Request request = new Request("GET", "/rooms/" + encodeRoomId(roomId) + "/members");
    request.addQueryString("access_token", access_token);
    return request;
  }

  public static Request createMessage(String roomId) {
    Request request = new Request("POST", "/rooms/" + encodeRoomId(roomId) + "/send/m.room.message");
    request.addQueryString("access_token", access_token);
    return request;
  }

  public static Request createMessageFeedback(String roomId) {
    Request request = new Request("POST", "/rooms/" + encodeRoomId(roomId) + "/send/m.room.message.feedback");
    request.addQueryString("access_token", access_token);
    return request;
  }


  public static String encodeDomain(String domain) {
    return domain.replace(":", "&");
  }

  public static String decodeDomain(String domain) {
    return domain.replace("&", ":");
  }

  public static String encodeRoomId(String domain) {
    return domain.replace("!", "%21");
  }

  public static String encodeRoomAlias(String domain) {
    return domain.replace("#", "%23");
  }

}