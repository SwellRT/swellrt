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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.typesafe.config.Config;
import org.dom4j.Element;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.FederationHostBridge;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolSignerInfo;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.federation.WaveletFederationProvider;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.WaveletName;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

/**
 * This class encapsulates the incoming packet processing portion of the
 * Federation Host. Messages arrive on this class from a foreign Federation
 * Remote for wavelets hosted by the local wave server.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
public class MatrixFederationHost implements WaveletFederationListener.Factory {

  @SuppressWarnings("unused")
  private static final Logger LOG = Logger.getLogger(MatrixFederationHost.class.getCanonicalName());

  private final WaveletFederationProvider waveletProvider;

  private MatrixPacketHandler handler = null;

  // A map of update listeners. There is one per remote domain we are sending updates to.
  // The name 'listener' refers to them listening for updates from the waveserver to send to the
  // network.
  private final LoadingCache<String, WaveletFederationListener> listeners;

  /**
   * Constructor. Note that {@link #setManager} must be called before this class
   * is ready to use.
   *
   * @param waveletProvider used for communicating back to the Host part of the
   *        wavelet server.
   * @param disco used for discovery
   */
  @Inject
  public MatrixFederationHost(@FederationHostBridge WaveletFederationProvider waveletProvider,
      final MatrixRoomManager room, final Config config) {
    this.waveletProvider = waveletProvider;
    listeners = CacheBuilder.newBuilder().build(new CacheLoader<String, WaveletFederationListener>() {
      @Override
      public WaveletFederationListener load(@SuppressWarnings("NullableProblems") String domain) {
        System.out.println("loading hosts");
        return new MatrixFederationHostForDomain(domain, handler, room, config);
      }
    });
  }

  /**
   * Set the handler instance for this class. Must be invoked before any other
   * methods are used.
   * @param handler the MatrixPacketHandler object, used to send packets.
   */
  public void setHandler(MatrixPacketHandler handler) {
    this.handler = handler;
    System.out.println("handler set");
  }

  /**
   * Parse to a ProtocolHashedVersion from a given string version/base64-hash combination.
   *
   * @param startVersion the starting version
   * @param base64Hash   the base64 hash
   * @throws IllegalArgumentException on bad data
   * @return a parsed protobuf object
   */
  private static ProtocolHashedVersion parseFromUnsafe(String startVersion, String base64Hash)
      throws IllegalArgumentException {
    return ProtocolHashedVersion.newBuilder()
        .setVersion(Long.parseLong(startVersion))
        .setHistoryHash(Base64Util.decode(base64Hash)).build();
  }

  /**
   * Reads a history request off the wire, sends it to the WS with a new
   * callback for returning the response.
   * @param request          the history request
   * @param responseCallback the callback to send the response back
   */
  void processHistoryRequest(final JSONObject request, final PacketCallback responseCallback) {
    JSONObject items = null, historyDelta = null;
    JSONObject data = request.optJSONObject("content").optJSONObject("data");
    JSONObject pubsubRequest = data.optJSONObject("pubsub");
    if (pubsubRequest != null) {
      items = pubsubRequest.optJSONObject("items");
      if (items != null) {
        historyDelta = items.optJSONObject("delta-history");
      }
    }
    if (items == null || historyDelta == null
            || historyDelta.optString("start-version").isEmpty()
            || historyDelta.optString("start-version-hash").isEmpty()
            || historyDelta.optString("end-version").isEmpty()
            || historyDelta.optString("end-version-hash").isEmpty()
            || historyDelta.optString("wavelet-name").isEmpty()) {
      responseCallback.error(FederationErrors.badRequest("Malformed history request"));
      return;
    }

    final ProtocolHashedVersion startVersion;
    try {
      startVersion = parseFromUnsafe(historyDelta.optString("start-version"),
          historyDelta.optString("start-version-hash"));
    } catch (IllegalArgumentException e) {
      responseCallback.error(FederationErrors.badRequest("Invalid format of start version"));
      return;
    }

    final ProtocolHashedVersion endVersion;
    try {
      endVersion = parseFromUnsafe(historyDelta.optString("end-version"),
          historyDelta.optString("end-version-hash"));
    } catch (IllegalArgumentException e) {
      responseCallback.error(FederationErrors.badRequest("Invalid format of end version"));
      return;
    }

    final long responseLengthLimit;
    if (!historyDelta.optString("response-length-limit").isEmpty()) {
      try {
        responseLengthLimit = Long.parseLong(historyDelta.optString("response-length-limit"));
      } catch (NumberFormatException e) {
        responseCallback.error(FederationErrors.badRequest("Invalid response length limit"));
        return;
      }
    } else {
      responseLengthLimit = 0;
    }

    final WaveletName waveletName;
    try {
      waveletName =
          MatrixUtil.waveletNameCodec.uriToWaveletName(historyDelta.optString("wavelet-name"));
    } catch (EncodingException e) {
      responseCallback.error(FederationErrors.badRequest(
          "Malformed wavelet name: " + historyDelta.optString("wavelet-name")));
      return;
    }

    // Construct a new response listener inline.
    WaveletFederationProvider.HistoryResponseListener listener =
        new WaveletFederationProvider.HistoryResponseListener() {
          @Override
          public void onFailure(FederationError error) {
            responseCallback.error(error);
          }

          @Override
          public void onSuccess(List<ByteString> appliedDeltaSet,
              ProtocolHashedVersion lastCommittedVersion, long versionTruncatedAt) {
            try {
              JSONObject response = new JSONObject();

              JSONObject pubsub = new JSONObject();
              response.putOpt("pubsub", pubsub);
              JSONArray items = new JSONArray();
              pubsub.putOpt("items", items);

              // Add each delta to the outgoing response.
              for (ByteString appliedDelta : appliedDeltaSet) {
                JSONObject item = new JSONObject();
                items.put(item);
                item.putOpt("applied-delta", Base64Util.encode(appliedDelta));
              }

              // Set the LCV history-hash, if provided.
              // TODO(thorogood): We don't set the hashed version, which is wrong,
              // but it's not part of the current spec (Feb 2010).
              if (lastCommittedVersion != null && lastCommittedVersion.hasVersion()) {
                String version = String.valueOf(lastCommittedVersion.getVersion());
                JSONObject item = new JSONObject();
                items.put(item);

                JSONObject commitNotice = new JSONObject();
                item.putOpt("commit-notice", commitNotice);

                commitNotice.putOpt("version", version);
              }

              // Set the version truncated at, if provided.
              if (versionTruncatedAt > 0) {
                String version = String.valueOf(versionTruncatedAt);
                JSONObject item = new JSONObject();
                items.put(item);

                JSONObject historyTruncated = new JSONObject();
                item.putOpt("history-truncated", historyTruncated);

                historyTruncated.putOpt("version", version);
              }

              // Send the message to the source.
              responseCallback.run(response);
            } catch (JSONException ex) {
              throw new RuntimeException(ex);
            }
          }
    };

    // Hand off a history request to the waveletProvider.
    String remoteDomain = request.optString("sender").split(":", 2)[1];
    waveletProvider.requestHistory(waveletName, remoteDomain, startVersion,
        endVersion, responseLengthLimit, listener);
  }

  /**
   * Handles a submit request from a foreign wave remote. Sends it to the wave
   * server, sets up a callback to send the response.
   * @param request          the submit request
   * @param responseCallback the callback to send the response back
   */
  void processSubmitRequest(final JSONObject request, final PacketCallback responseCallback) {
    JSONObject item = null, submitRequest = null, deltaElement = null;
    JSONObject data = request.optJSONObject("content").optJSONObject("data");
    JSONObject pubsubRequest = data.optJSONObject("pubsub");
    // TODO: check for correct elements.
    JSONObject publish = pubsubRequest.optJSONObject("publish");
    if (publish != null) {
      item = publish.optJSONObject("item");
      if (item != null) {
        submitRequest = item.optJSONObject("submit-request");
        if (submitRequest != null) {
          deltaElement = submitRequest.optJSONObject("delta");
        }
      }
    }
    if (publish == null || item == null || submitRequest == null
            || deltaElement == null
            || deltaElement.optString("wavelet-name").isEmpty()
            || deltaElement.optString("value").isEmpty()) {
      responseCallback.error(FederationErrors.badRequest("Malformed submit request"));
      return;
    }

    final WaveletName waveletName;
    try {
      waveletName =
          MatrixUtil.waveletNameCodec.uriToWaveletName(deltaElement.optString("wavelet-name"));
    } catch (EncodingException e) {
      responseCallback.error(FederationErrors.badRequest(
          "Malformed wavelet name: " + deltaElement.optString("wavelet-name")));
      return;
    }

    final ProtocolSignedDelta delta;
    try {
      delta = ProtocolSignedDelta.parseFrom(Base64Util.decode(deltaElement.optString("value")));
    } catch (InvalidProtocolBufferException e) {
      responseCallback.error(FederationErrors.badRequest(
          "Malformed delta, not a valid protocol buffer"));
      return;
    }

    // Construct a submit result listener inline.
    WaveletFederationProvider.SubmitResultListener listener =
        new WaveletFederationProvider.SubmitResultListener() {
          @Override
          public void onFailure(FederationError error) {
            responseCallback.error(error);
          }

          @Override
          public void onSuccess(int operations, ProtocolHashedVersion version, long timestamp) {
            try {
              JSONObject response = new JSONObject();

              JSONObject pubsub = new JSONObject();
              response.putOpt("pubsub", pubsub);
              JSONObject publish = new JSONObject();
              pubsub.putOpt("publish", publish);
              JSONObject item = new JSONObject();
              publish.putOpt("item", item);
              JSONObject submitResponse = new JSONObject();
              item.putOpt("submit-response", submitResponse);

              submitResponse.putOpt("application-timestamp", String.valueOf(timestamp));
              submitResponse.putOpt("operations-applied", String.valueOf(operations));

              JSONObject hashedVersion = new JSONObject();
              submitResponse.putOpt("hashed-version", hashedVersion);
              hashedVersion.putOpt("history-hash", Base64Util.encode(version.getHistoryHash()));
              hashedVersion.putOpt("version", String.valueOf(version.getVersion()));

              responseCallback.run(response);
            } catch (JSONException ex) {
              throw new RuntimeException(ex);
            }
          }
    };

    // Hand off the submit request to the wavelet provider.
    waveletProvider.submitRequest(waveletName, delta, listener);
  }

  /**
   * Reads a get signer request off the wire, sends it to the WS with a new
   * callback for returning the response.
   * @param request          the get signer request
   * @param responseCallback the callback to send the response back
   */
  void processGetSignerRequest(final JSONObject request, final PacketCallback responseCallback) {
    JSONObject data = request.optJSONObject("content").optJSONObject("data");
    JSONObject items = data.optJSONObject("pubsub").optJSONObject("items");
    JSONObject signerRequest = items != null ? items.optJSONObject("signer-request") : null;

    if (items == null || signerRequest == null
            || signerRequest.optString("wavelet-name").isEmpty()
            || signerRequest.optString("signer-id").isEmpty()
            || signerRequest.optString("version").isEmpty()
            || signerRequest.optString("history-hash").isEmpty()) {
      //manager.sendErrorResponse(request, FederationErrors.badRequest("Malformed signer request"));
      return;
    }

    final ByteString signerId;
    try {
      signerId = Base64Util.decode(signerRequest.optString("signer-id"));
    } catch (IllegalArgumentException e) {
      responseCallback.error(FederationErrors.badRequest("Malformed signer ID"));
      return;
    }

    final ProtocolHashedVersion deltaEndVersion;
    try {
      deltaEndVersion = parseFromUnsafe(signerRequest.optString("version"),
          signerRequest.optString("history-hash"));
    } catch (IllegalArgumentException e) {
      responseCallback.error(FederationErrors.badRequest("Invalid hashed version"));
      return;
    }

    final WaveletName waveletName;
    try {
      waveletName =
          MatrixUtil.waveletNameCodec.uriToWaveletName(signerRequest.optString("wavelet-name"));
    } catch (EncodingException e) {
      responseCallback.error(FederationErrors.badRequest("Malformed wavelet name"));
      return;
    }

    WaveletFederationProvider.DeltaSignerInfoResponseListener listener =
        new WaveletFederationProvider.DeltaSignerInfoResponseListener() {
          @Override
          public void onFailure(FederationError error) {
            responseCallback.error(error);
          }

          @Override
          public void onSuccess(ProtocolSignerInfo signerInfo) {
            try {
              JSONObject response = new JSONObject();

              JSONObject pubsub = new JSONObject();
              response.putOpt("pubsub", pubsub);
              JSONObject items = new JSONObject();
              pubsub.putOpt("items", items);
              MatrixUtil.protocolSignerInfoToJson(signerInfo, items);

              responseCallback.run(response);
            } catch (JSONException ex) {
              throw new RuntimeException(ex);
            }
          }

        };

    waveletProvider.getDeltaSignerInfo(signerId, waveletName, deltaEndVersion, listener);
  }

  /**
   * Reads a post signer request off the wire, sends it to the WS with a new
   * callback for returning the response.
   * @param request          the post signer request
   * @param responseCallback the callback to send the response back
   */
  void processPostSignerRequest(final JSONObject request, final PacketCallback responseCallback) {
    JSONObject data = request.optJSONObject("content").optJSONObject("data");
    JSONObject item = null, signatureElement = null;
    JSONObject pubsubRequest = data.optJSONObject("pubsub");
    JSONObject publish = pubsubRequest.optJSONObject("publish");
    if (publish != null) {
      item = publish.optJSONObject("item");
      if (item != null) {
        signatureElement = item.optJSONObject("signature");
      }
    }

    if (publish == null || item == null || signatureElement == null
            || signatureElement.optString("domain").isEmpty()
            || signatureElement.optString("algorithm").isEmpty()
            || signatureElement.optString("certificate").isEmpty()) {
      responseCallback.error(FederationErrors.badRequest("Malformed post signer request"));
      return;
    }

    ProtocolSignerInfo signer;
    signer = MatrixUtil.jsonToProtocolSignerInfo(signatureElement);
    

    WaveletFederationProvider.PostSignerInfoResponseListener listener =
        new WaveletFederationProvider.PostSignerInfoResponseListener() {
          @Override
          public void onFailure(FederationError error) {
            responseCallback.error(error);
          }

          @Override
          public void onSuccess() {
            try {
              JSONObject response = new JSONObject();

              JSONObject pubsub = new JSONObject();
              response.putOpt("pubsub", pubsub);

              JSONObject publish = new JSONObject();
              pubsub.putOpt("publish", publish);
              JSONObject item = new JSONObject();
              publish.putOpt("item", item);

              item.putOpt("node", "signer");
              item.putOpt("signature-response", "WAVE_SERVER");

              responseCallback.run(response);
            } catch (JSONException ex) {
              throw new RuntimeException(ex);
            }
          }
    };

    // TODO(thorogood,arb): This field is a Bad Idea; it could be faked and not
    // be a provider we host on this instance. Instead, we should infer from the
    // "To:" JID.
    String targetDomain = signatureElement.optString("domain");

    // The first argument is the domain we intend to send this information to.
    waveletProvider.postSignerInfo(targetDomain, signer, listener);
  }

  @Override
  public WaveletFederationListener listenerForDomain(String domain) {
    try {
      // TODO(thorogood): Kick off disco here instead of inside
      // XmppFederationHostForDomain.
      return listeners.get(domain);
    } catch (ExecutionException ex) {
      throw new RuntimeException(ex);
    }
}

}