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

import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.waveprotocol.wave.federation.FederationErrorProto.FederationError;
import org.waveprotocol.wave.federation.FederationErrors;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.WaveletFederationListener;
import org.waveprotocol.wave.model.id.URIEncoderDecoder.EncodingException;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.util.logging.Log;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import com.typesafe.config.Config;


/**
 * An instance of this class is created on demand for outgoing
 * messages to another wave Federation Remote. The wave server asks
 * the MatrixFederationHost to create these.
 *
 * @author khwaqee@gmail.com (Waqee Khalid)
 */
class MatrixFederationHostForDomain implements WaveletFederationListener {

  private static final Log LOG = Log.get(MatrixFederationHostForDomain.class);

  // Timeout for outstanding listener updates sent over Matrix.
  private static final int MATRIX_LISTENER_TIMEOUT = 30;

  private final String remoteDomain;
  private final MatrixPacketHandler handler;
  private final String id;
  private final MatrixRoomManager room;

  @Inject
  public MatrixFederationHostForDomain(final String domain, MatrixPacketHandler handler,
      MatrixRoomManager room, Config config) {
    this.remoteDomain = domain;
    this.handler = handler;
    this.id = config.getString("federation.matrix_id");
    this.room = room;
  }

  @Override
  public void waveletCommitUpdate(WaveletName waveletName, ProtocolHashedVersion committedVersion,
      WaveletUpdateCallback callback) {
    waveletUpdate(waveletName, null, committedVersion, callback);
  }

  @Override
  public void waveletDeltaUpdate(WaveletName waveletName, List<ByteString> appliedDeltas,
      WaveletUpdateCallback callback) {
    waveletUpdate(waveletName, appliedDeltas, null, callback);
  }

  /**
   * Sends a wavelet update message on behalf of the wave server. This
   * method just triggers a disco lookup (which may be cached) and
   * sets up a callback to call the real method that does the work.
   * This method may contain applied deltas, a commit notice, or both.
   *
   * @param waveletName the wavelet name
   * @param deltaList the deltas to include in the message, or null
   * @param committedVersion last committed version to include, or null
   * @param callback callback to invoke on delivery success/failure
   */
  public void waveletUpdate(final WaveletName waveletName, final List<ByteString> deltaList,
      final ProtocolHashedVersion committedVersion, final WaveletUpdateCallback callback) {
    if ((deltaList == null || deltaList.isEmpty()) && committedVersion == null) {
      throw new IllegalArgumentException("Must send at least one delta, or a last committed " +
          "version notice, for the target wavelet: " + waveletName);
    }

    LOG.fine("Looking up matrix room for remote domain " + remoteDomain + "...");

    room.searchRemoteId(remoteDomain, new SuccessFailCallback<String, String>() {
      @Override
      public void onSuccess(String roomId) {
        LOG.fine("Matrix room for remote domain " + remoteDomain + " is " + roomId);
        internalWaveletUpdate(waveletName, deltaList, committedVersion, callback, roomId);
      }

      @Override
      public void onFailure(String errorMessage) {
        LOG.warning("Matrix room look up failed for remote domain " + remoteDomain
            + ", waveletUpdate not sent");
        callback.onFailure(FederationErrors.newFederationError(
            FederationError.Code.RESOURCE_CONSTRAINT, errorMessage));
      }
    });
  }

  /**
   * Sends a wavelet update message on behalf of the wave server once disco is
   * complete. This method may contain applied deltas, a commit notice, or both.
   *
   * @param waveletName      the wavelet name
   * @param deltaList        the deltas to include in the message, or null
   * @param committedVersion last committed version to include, or null
   * @param callback         callback to invoke on delivery success/failure
   * @param remoteJid        the remote JID to send the update to
   */
  private void internalWaveletUpdate(final WaveletName waveletName,
      final List<ByteString> deltaList, final ProtocolHashedVersion committedVersion,
      final WaveletUpdateCallback callback, String roomId) {
    try{
      Request message = MatrixUtil.createMessage(roomId);
      message.addBody("msgtype", "m.message");
      message.addBody("body", "");

      JSONObject data = new JSONObject();
      message.addBody("data", data);

      final String encodedWaveletName;
      try {
        encodedWaveletName = MatrixUtil.waveletNameCodec.waveletNameToURI(waveletName);
      } catch (EncodingException e) {
        callback.onFailure(FederationErrors.badRequest("Bad wavelet name " + waveletName));
        return;
      }

      JSONObject event = new JSONObject();
      data.putOpt("event", event);

      JSONObject items = new JSONObject();
      event.putOpt("items", items);

      JSONArray item = new JSONArray();
      items.putOpt("item", item);

      if (deltaList != null) {
        for (ByteString delta : deltaList) {

          JSONObject waveletUpdate = new JSONObject();
          item.put(waveletUpdate);

          waveletUpdate.putOpt("wavelet-name", encodedWaveletName);
          waveletUpdate.putOpt("applied-delta", Base64Util.encode(delta));
        }
      }
      if (committedVersion != null) {
        JSONObject waveletUpdate = new JSONObject();
        item.put(waveletUpdate);

        waveletUpdate.putOpt("wavelet-name", encodedWaveletName);

        JSONObject commitNotice = new JSONObject();
        waveletUpdate.putOpt("commit-notice", commitNotice);

        commitNotice.putOpt("version", Long.toString(committedVersion.getVersion()));
        commitNotice.putOpt("history-hash", Base64Util.encode(committedVersion.getHistoryHash()));
      }

      if (LOG.isFinerLoggable()) {
        LOG.finer("waveletUpdate to remote: \n\n" + message.getBody().toString());
      }

      // Send the generated message through to the foreign Matrix server.
      handler.send(message, new PacketCallback() {
        @Override
        public void error(FederationError error) {
          callback.onFailure(error);
        }

        @Override
        public void run(JSONObject packet) {
          callback.onSuccess();
        }
      }, MATRIX_LISTENER_TIMEOUT);
    } catch (JSONException ex) {
      throw new RuntimeException(ex);
    }
  }
}