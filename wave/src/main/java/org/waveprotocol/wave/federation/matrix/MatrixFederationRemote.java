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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.typesafe.config.Config;
import org.apache.commons.codec.binary.Base64;
import org.dom4j.Attribute;
import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.FederationRemoteBridge;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.WaveletName;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Remote implementation. Receives submit and history requests from the local
 * wave server and sends them to a remote wave server Host, and also receives
 * update messages from a remote wave server Host and sends them to the local
 * wave server.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
public class MatrixFederationRemote implements WaveletFederationProvider {
  private static final Logger LOG = Logger.getLogger(MatrixFederationRemote.class.getCanonicalName());

  // Timeout for outstanding provider calls sent over Matrix.
  private static final int MATRIX_PROVIDER_TIMEOUT = 30;

  private final WaveletFederationListener.Factory updatesListenerFactory;
  private final MatrixRoomManager room;
  private final String id;

  private MatrixPacketHandler handler = null;

  /**
   * Constructor. Note that {@link #setManager} must be called before this class
   * is ready to use.
   *
   * @param updatesListenerFactory used to communicate back to the local wave
   *        server when an update arrives.
   */
  @Inject
  public MatrixFederationRemote(
      @FederationRemoteBridge WaveletFederationListener.Factory updatesListenerFactory,
      MatrixRoomManager room, Config config) {
    this.updatesListenerFactory = updatesListenerFactory;
    this.room = room;
    this.id = config.getString("federation.matrix_id");
  }

  /**
   * Set the manager instance for this class. Must be invoked before any other
   * methods are used.
   */
  public void setHandler(MatrixPacketHandler handler) {
    this.handler = handler;
  }

  /**
   * Request submission of signed delta. This is part of the Federation Remote
   * interface - sends a submit request on behalf of the wave server. Part of
   * the WaveletFederationProvider interface.
   *
   * @param waveletName name of wavelet.
   * @param signedDelta delta signed by the submitting wave server.
   * @param listener callback for the result of the submit.
   */
  @Override
  public void submitRequest(final WaveletName waveletName,
                            final ProtocolSignedDelta signedDelta,
                            final SubmitResultListener listener) {
    try{

      final JSONObject submitIq = new JSONObject();

      LOG.info("Submitting delta to remote server, wavelet " + waveletName);

      JSONObject pubsub = new JSONObject();
      submitIq.putOpt("pubsub", pubsub);
      JSONObject publish = new JSONObject();
      pubsub.putOpt("publish", publish);

      publish.putOpt("node", "wavelet");
      JSONObject item = new JSONObject();
      publish.putOpt("item", item);
      JSONObject submitRequest = new JSONObject();
      item.putOpt("submit-request", submitRequest);

      JSONObject deltaElement = new JSONObject();
      submitRequest.putOpt("delta", deltaElement);

      deltaElement.putOpt("value", Base64Util.encode(signedDelta.toByteArray()));
      try {
        deltaElement.putOpt("wavelet-name",
            MatrixUtil.waveletNameCodec.waveletNameToURI(waveletName));
      } catch (EncodingException e) {
        listener.onFailure(FederationErrors.badRequest(
            "Couldn't encode wavelet name " + waveletName));
        return;
      }

      final PacketCallback callback = new PacketCallback() {
        @Override
        public void error(FederationError error) {
          listener.onFailure(error);
        }

        @Override
        public void run(JSONObject packet) {
          processSubmitResponse(packet, listener);
        }
      };

      room.searchRemoteId(waveletName.waveletId.getDomain(),
        new SuccessFailCallback<String, String>() {
          @Override
          public void onSuccess(String roomId) {
            Preconditions.checkNotNull(roomId);
            Request request = MatrixUtil.createMessage(roomId);
            request.addBody("msgtype", "m.set");
            request.addBody("body", "");
            request.addBody("data", submitIq);
            handler.send(request, callback, MATRIX_PROVIDER_TIMEOUT);
          }

          @Override
          public void onFailure(String errorMessage) {
            // TODO(thorogood): Broken, Disco should return the error (and it
            // should be timeout/etc)
            listener.onFailure(FederationErrors.badRequest(
                "No such wave server " + waveletName.waveletId.getDomain() + ": " + errorMessage));
          }
        });
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Retrieve delta history for the given wavelet. <p/> Part of the
   * WaveletFederationProvider interface.
   *
   * @param waveletName  name of wavelet.
   * @param domain       the remote Federation Host
   * @param startVersion beginning of range (inclusive), minimum 0.
   * @param endVersion   end of range (exclusive).
   * @param lengthLimit  estimated size, in bytes, as an upper limit on the
   *                     amount of data returned.
   * @param listener     callback for the result.
   */
  public void requestHistory(final WaveletName waveletName,
                             final String domain,
                             ProtocolHashedVersion startVersion,
                             ProtocolHashedVersion endVersion,
                             long lengthLimit,
                             final WaveletFederationProvider.HistoryResponseListener listener) {

    try {
      final JSONObject submitIq = new JSONObject();

      LOG.info("Getting history from remote server, wavelet " + waveletName
                  + " version " + startVersion + " (inc) through " + endVersion
                  + " (ex)");

      JSONObject pubsub = new JSONObject();
      submitIq.putOpt("pubsub", pubsub);
      JSONObject items = new JSONObject();
      pubsub.putOpt("items", items);

      items.putOpt("node", "wavelet");
      JSONObject historyDelta = new JSONObject();
      
      items.putOpt("delta-history", historyDelta);

      historyDelta.putOpt("start-version", Long.toString(startVersion
          .getVersion()));
      historyDelta.putOpt("start-version-hash", Base64Util
          .encode(startVersion.getHistoryHash()));
      historyDelta.putOpt("end-version", Long.toString(endVersion
          .getVersion()));
      historyDelta.putOpt("end-version-hash", Base64Util.encode(endVersion
          .getHistoryHash()));
      if (lengthLimit > 0) {
        historyDelta.putOpt("response-length-limit", Long
            .toString(lengthLimit));
      }
      try {
        historyDelta.putOpt("wavelet-name",
            MatrixUtil.waveletNameCodec.waveletNameToURI(waveletName));
      } catch (EncodingException e) {
        listener.onFailure(
            FederationErrors.badRequest("Couldn't encode wavelet name " + waveletName));
        return;
      }

      final PacketCallback callback = new PacketCallback() {
        public void error(FederationError error) {
          listener.onFailure(error);
        }

        @Override
        public void run(JSONObject packet) {
          processHistoryResponse(packet, listener);
        }
      };

      room.searchRemoteId(domain, new SuccessFailCallback<String, String>() {
        @Override
        public void onSuccess(String roomId) {
          Preconditions.checkNotNull(roomId);
          Request request = MatrixUtil.createMessage(roomId);
          request.addBody("msgtype", "m.get");
          request.addBody("body", "");
          request.addBody("data", submitIq);
          handler.send(request, callback, MATRIX_PROVIDER_TIMEOUT);
        }

        @Override
        public void onFailure(String errorMessage) {
          listener.onFailure(FederationErrors.badRequest(
              "No such wave server " + domain + ": " + errorMessage));
        }
      });
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void getDeltaSignerInfo(ByteString signerId, WaveletName waveletName,
                                 ProtocolHashedVersion deltaEndVersion,
                                 final DeltaSignerInfoResponseListener listener) {

    try{
      final JSONObject getSignerIq = new JSONObject();

      JSONObject pubsub = new JSONObject();
      getSignerIq.putOpt("pubsub", pubsub);
      JSONObject items = new JSONObject();
      pubsub.putOpt("items", items);

      // Extract domain from waveletId
      final String remoteDomain = waveletName.waveletId.getDomain();
      
      items.putOpt("node", "signer");
      // TODO: should allow multiple requests in the same packet
      JSONObject signerRequest = new JSONObject();
      items.putOpt("signer-request", signerRequest);

      signerRequest.putOpt("signer-id", Base64Util.encode(signerId));
      signerRequest.putOpt("history-hash", Base64Util
          .encode(deltaEndVersion.getHistoryHash()));
      signerRequest.putOpt("version", String.valueOf(deltaEndVersion
          .getVersion()));
      try {
        signerRequest.putOpt("wavelet-name",
            MatrixUtil.waveletNameCodec.waveletNameToURI(waveletName));
      } catch (EncodingException e) {
        listener.onFailure(FederationErrors.badRequest(
            "Couldn't encode wavelet name " + waveletName));
        return;
      }

      final PacketCallback callback = new PacketCallback() {
        @Override
        public void error(FederationError error) {
          listener.onFailure(error);
        }

        @Override
        public void run(JSONObject packet) {
          processGetSignerResponse(packet, listener);
        }
      };

      room.searchRemoteId(
          remoteDomain, new SuccessFailCallback<String, String>() {
            @Override
            public void onSuccess(String roomId) {
              Preconditions.checkNotNull(roomId);
              Request request = MatrixUtil.createMessage(roomId);
              request.addBody("msgtype", "m.get");
              request.addBody("body", "");
              request.addBody("data", getSignerIq);
              handler.send(request, callback, MATRIX_PROVIDER_TIMEOUT);
            }

            @Override
            public void onFailure(String errorMessage) {
              listener.onFailure(FederationErrors.badRequest(
                  "No such wave server " + remoteDomain + ": " + errorMessage));
            }
          });
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  @Override
  public void postSignerInfo(
      final String remoteDomain,
      ProtocolSignerInfo signerInfo,
      final WaveletFederationProvider.PostSignerInfoResponseListener listener) {

    try {
      LOG.info("remoteDomain: " + remoteDomain);
      final JSONObject signerIq = new JSONObject();

      JSONObject pubsub = new JSONObject();
      signerIq.putOpt("pubsub", pubsub);
      JSONObject publish = new JSONObject();
      pubsub.putOpt("publish", publish);

      publish.putOpt("node", "signer");

      JSONObject item = new JSONObject();
      publish.putOpt("item", item);
      MatrixUtil.protocolSignerInfoToJson(signerInfo, item);

      final PacketCallback callback = new PacketCallback() {
        @Override
        public void error(FederationError error) {
          listener.onFailure(error);
        }

        @Override
        public void run(JSONObject packet) {
          processPostSignerResponse(packet, listener);
        }
      };

      room.searchRemoteId(
          remoteDomain, new SuccessFailCallback<String, String>() {
            @Override
            public void onSuccess(String roomId) {
              Preconditions.checkNotNull(roomId);
              Request request = MatrixUtil.createMessage(roomId);
              request.addBody("msgtype", "m.set");
              request.addBody("body", "");
              request.addBody("data", signerIq);
              handler.send(request, callback, MATRIX_PROVIDER_TIMEOUT);
            }

            @Override
            public void onFailure(String errorMessage) {
              listener.onFailure(FederationErrors.badRequest(
                  "No such wave server " + remoteDomain + ": " + errorMessage));
            }
          });
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Handles a wavelet update message from a foreign Federation Host. Passes the
   * message to the local waveserver (synchronously) and replies.
   *
   * @param updateMessage the incoming Matrix message.
   * @param responseCallback response callback for acks and errors
   */
  public void update(final JSONObject updateMessage, final PacketCallback responseCallback) {

    JSONObject data = updateMessage.optJSONObject("content").optJSONObject("data");

    // Check existence of <event>
    JSONObject event = data.optJSONObject("event");
    if (event == null) {
      responseCallback.error(FederationErrors.badRequest("Event element missing from message"));
      return;
    }

    // Check existence of <items> within <event>
    JSONObject items = event.optJSONObject("items");
    if (items == null) {
      responseCallback.error(FederationErrors.badRequest(
          "Items element missing from update message"));
      return;
    }

    // Complain if no items have been included.
    JSONArray elements = items.optJSONArray("item");
    if (elements.length() == 0) {
      responseCallback.error(FederationErrors.badRequest("No items included"));
      return;
    }

    // Create a callback latch counter and corresponding countDown runnable.
    // When the latch reaches zero, send receipt (if it was requested).
    final AtomicInteger callbackCount = new AtomicInteger(1);
    final Runnable countDown = new Runnable() {
      @Override
      public void run() {
        try {
          if (callbackCount.decrementAndGet() == 0) {
            JSONObject response = new JSONObject();
            response.putOpt("received", "MATRIX_RECEIPTS");
            responseCallback.run(response);
          }
        } catch (JSONException ex) {
          throw new RuntimeException(ex);
        }
      }
    };

    WaveletFederationListener.WaveletUpdateCallback callback =
        new WaveletFederationListener.WaveletUpdateCallback() {
          @Override
          public void onSuccess() {
            countDown.run();
          }

          @Override
          public void onFailure(FederationError error) {
            // Note that we don't propogate the error, we just ack the stanza
            // and continue.
            // TODO(thorogood): We may want to rate-limit misbehaving servers
            // that are sending us invalid/malicious data.
            LOG.warning("Incoming Matrix waveletUpdate failure: " + error);
            countDown.run();
          }
        };

    // We must call callback once on every iteration to ensure that we send
    // response if receiptRequested != null.
    for (int i=0; i<elements.length(); i++) {
      JSONObject waveletUpdate = elements.optJSONObject(i);

      if (waveletUpdate == null) {
        callback.onFailure(FederationErrors.badRequest(
            "wavelet-update element missing from message: " + updateMessage));
        continue;
      }

      final WaveletName waveletName;
      try {
        waveletName = MatrixUtil.waveletNameCodec.uriToWaveletName(
            waveletUpdate.optString("wavelet-name"));
      } catch (EncodingException e) {
        callback.onFailure(FederationErrors.badRequest(
            "Couldn't decode wavelet name: " + waveletUpdate.optString("wavelet-name")));
        continue;
      }

      WaveletFederationListener listener =
          updatesListenerFactory.listenerForDomain(waveletName.waveletId.getDomain());

      // Submit all applied deltas to the domain-focused listener.
      List<ByteString> deltas = Lists.newArrayList();
      String deltaBody = waveletUpdate.optString("applied-delta");
      System.out.println("wait");
      if (!deltaBody.isEmpty()) {
        System.out.println("wtf");
        deltas.add(Base64Util.decode(deltaBody));
        callbackCount.incrementAndGet(); // Increment required callbacks.
        listener.waveletDeltaUpdate(waveletName, deltas, callback);
      }

      // Optionally submit any received last committed notice.
      JSONObject commitNoticeElement = waveletUpdate.optJSONObject("commit-notice");
      if (commitNoticeElement != null) {
        ProtocolHashedVersion version = ProtocolHashedVersion.newBuilder()
            .setHistoryHash(Base64Util.decode(commitNoticeElement.optString("history-hash")))
            .setVersion(Long.parseLong(commitNoticeElement.optString("version"))).build();
        callbackCount.incrementAndGet(); // Increment required callbacks.
        listener.waveletCommitUpdate(waveletName, version, callback);
      }
    }

    

    // Release sentinel so that 'expected' callbacks from the WS don't invoke
    // sending a receipt.
    countDown.run();
  }

  /**
   * Parses the response to a submitRequest and passes the result to the correct
   * wave server.
   *
   * @param result   the XMPP Packet
   * @param listener the listener to invoke with the response.
   */
  private void processSubmitResponse(JSONObject result, SubmitResultListener listener) {

    JSONObject data = result.optJSONObject("content").optJSONObject("data");

    JSONObject publish = null;
    JSONObject item = null;
    JSONObject submitResponse = null;
    JSONObject hashedVersionElement = null;
    JSONObject pubsub = data.optJSONObject("pubsub");
    if (pubsub != null) {
      publish = pubsub.optJSONObject("publish");
      if (publish != null) {
        item = publish.optJSONObject("item");
        if (item != null) {
          submitResponse = item.optJSONObject("submit-response");
          if (submitResponse != null) {
            hashedVersionElement = submitResponse.optJSONObject("hashed-version");
          }
        }
      }
    }

    if (pubsub == null || publish == null || item == null
        || submitResponse == null || hashedVersionElement == null
        || hashedVersionElement.optString("history-hash").isEmpty()
        || hashedVersionElement.optString("version").isEmpty()
        || submitResponse.optString("application-timestamp").isEmpty()
        || submitResponse.optString("operations-applied").isEmpty()) {
      LOG.severe("Unexpected submitResponse to submit request: " + result);
      listener.onFailure(FederationErrors.badRequest("Invalid submitResponse: " + result));
      return;
    }

    ProtocolHashedVersion.Builder hashedVersion = ProtocolHashedVersion.newBuilder();
    hashedVersion.setHistoryHash(
        Base64Util.decode(hashedVersionElement.optString("history-hash")));
    hashedVersion.setVersion(Long.parseLong(hashedVersionElement.optString("version")));
    long applicationTimestamp =
        Long.parseLong(submitResponse.optString("application-timestamp"));
    int operationsApplied = Integer.parseInt(submitResponse.optString("operations-applied"));
    listener.onSuccess(operationsApplied, hashedVersion.build(), applicationTimestamp);
  }

  /**
   * Parses a response to a history request and passes the result to the wave
   * server.
   *
   * @param historyResponse the XMPP packet
   * @param listener        interface to the wave server
   */
  @SuppressWarnings("unchecked")
  private void processHistoryResponse(JSONObject historyResponse,
      WaveletFederationProvider.HistoryResponseListener listener) {

    JSONObject data = historyResponse.optJSONObject("content").optJSONObject("data");
    JSONObject pubsubResponse = data.optJSONObject("pubsub");
    JSONArray items = pubsubResponse.optJSONArray("items");
    long versionTruncatedAt = -1;
    long lastCommittedVersion = -1;
    List<ByteString> deltaList = Lists.newArrayList();

    if (items != null) {
      for (int i=0; i<items.length(); i++) {

        JSONObject element = items.optJSONObject(i);

        String elementName = (String)element.keys().next();
        switch (elementName) {
          case "applied-delta":
            String deltaBody = element.optString(elementName);
            deltaList.add(Base64Util.decode(deltaBody));
            break;
          case "commit-notice":
            String commitVersion = element.optJSONObject(elementName).optString("version");
            if (!commitVersion.isEmpty()) {
              try {
                lastCommittedVersion = Long.parseLong(commitVersion);
              } catch (NumberFormatException e) {
                lastCommittedVersion = -1;
              }
            }
            break;
          case "history-truncated":
            String truncVersion = element.optJSONObject(elementName).optString("version");
            if (!truncVersion.isEmpty()) {
              try {
                versionTruncatedAt = Long.parseLong(truncVersion);
              } catch (NumberFormatException e) {
                versionTruncatedAt = -1;
              }
            }
            break;
          default:
            listener.onFailure(FederationErrors.badRequest(
                    "Bad response packet: " + historyResponse));
            break;
        }
      }
    } else {
      listener.onFailure(FederationErrors.badRequest("Bad response packet: " + historyResponse));
    }

    final ProtocolHashedVersion lastCommitted;
    if (lastCommittedVersion > -1) {
      // TODO(thorogood): fedone doesn't send a history hash, and it's arguable
      // that it's even sane to include it.
      // Can't set it to null - NPE
      lastCommitted =
          ProtocolHashedVersion.newBuilder()
              .setVersion(lastCommittedVersion).setHistoryHash(ByteString.EMPTY)
              .build();
    } else {
      lastCommitted = null;
    }
    listener.onSuccess(deltaList, lastCommitted, versionTruncatedAt);
  }

  /**
   * Parses a GetSigner response, passes result to the waveserver.
   *
   * @param packet   the response packet
   * @param listener the interface to the wave server
   */
  private void processGetSignerResponse(JSONObject packet, DeltaSignerInfoResponseListener listener) {
    JSONObject data = packet.optJSONObject("content").optJSONObject("data");
    JSONObject items = data.optJSONObject("pubsub").optJSONObject("items");
    JSONObject signature = items.optJSONObject("signature");
    if (signature == null) {
      LOG.severe("Empty getDeltaSignerRequest response: " + packet);
      listener.onFailure(FederationErrors.badRequest("Bad getDeltaSignatureRequest response"));
      return;
    }
    String domain = signature.optString("domain");
    String hashName = signature.optString("algorithm");
    if (domain.isEmpty() || hashName.isEmpty() || signature.optJSONArray("certificate") == null) {
      LOG.severe("Bad getDeltaSignerRequest response: " + packet);
      listener.onFailure(FederationErrors.badRequest("Bad getDeltaSignatureRequest response"));
      return;
    }
    ProtocolSignerInfo signer = MatrixUtil.jsonToProtocolSignerInfo(signature);
    listener.onSuccess(signer);
  }

  /**
   * Parses a response to a PostSigner request, passes result to wave server.
   *
   * @param packet   the response Matrix packet
   * @param listener the listener to invoke
   */
  private void processPostSignerResponse(
      JSONObject packet,
      WaveletFederationProvider.PostSignerInfoResponseListener listener) {
    JSONObject data = packet.optJSONObject("content").optJSONObject("data");
    JSONObject pubsub = data.optJSONObject("pubsub");
    JSONObject item = pubsub.optJSONObject("publish").optJSONObject("item");
    if (!item.optString("signature-response").isEmpty()) {
      listener.onSuccess();
    } else {
      listener.onFailure(FederationErrors.badRequest("No valid response"));
    }
  }

}