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

  @Override
  public void submitRequest(
      final WaveletName waveletName,
      final ProtocolSignedDelta signedDelta,
      final SubmitResultListener listener) {

  }

  @Override
  public void requestHistory(
      final WaveletName waveletName,
      final String domain,
      ProtocolHashedVersion startVersion,
      ProtocolHashedVersion endVersion,
      long lengthLimit,
      final WaveletFederationProvider.HistoryResponseListener listener) {

  }

  @Override
  public void getDeltaSignerInfo(
      ByteString signerId,
      WaveletName waveletName,
      ProtocolHashedVersion deltaEndVersion,
      final DeltaSignerInfoResponseListener listener) {

  }

  @Override
  public void postSignerInfo(
      final String remoteDomain,
      ProtocolSignerInfo signerInfo,
      final WaveletFederationProvider.PostSignerInfoResponseListener listener) {

  }

}