/**
 * Copyright 2012 A. Kaplanov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.waveprotocol.box.server.waveserver;

import com.google.gxp.org.apache.xerces.impl.dv.util.Base64;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.protobuf.ByteString;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import org.waveprotocol.box.server.persistence.AttachmentStore;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.util.logging.Log;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolHashedVersion;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

/**
 *
 * @author (akaplanov@gmail.com) (Andrew Kaplanov)
 */
@SuppressWarnings("serial")
@Singleton
public class ImportServlet extends HttpServlet {

  private static final Log LOG = Log.get(ImportServlet.class);
  public static final String GWAVE_PUBLIC_DOMAIN = "a.gwave.com";
  public static final String GWAVE_PUBLIC_USER_NAME = "public";
  public static final String WIAB_SHARED_USER_NAME = "";
  private static final IdURIEncoderDecoder URI_CODEC = new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);
  private final WaveletProvider waveletProvider;
  private final AttachmentStore attachmentStore;
  private final WaveMap waveMap;

  @Inject
  private ImportServlet(WaveletProvider waveletProvider, AttachmentStore attachmentStore,
      WaveMap waveMap) {
    this.waveletProvider = waveletProvider;
    this.attachmentStore = attachmentStore;
    this.waveMap = waveMap;
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    String domain = request.getHeader("domain");
    WaveId waveId = WaveId.deserialise(request.getHeader("waveId"));
    WaveletId waveletId = WaveletId.deserialise(request.getHeader("waveletId"));
    final StringWriter error = new StringWriter();
    boolean somethingProcessed = false;
    boolean somethingSkipped = false;
    try {
      JSONObject exp = new JSONObject(readToString(request.getReader()));
      JSONArray rawDeltas = exp.getJSONObject("data").getJSONArray("rawDeltas");
      if (rawDeltas.length() != 0) {
        List<ProtocolAppliedWaveletDelta> deltas = new LinkedList<ProtocolAppliedWaveletDelta>();
        for (int i = 0; i < rawDeltas.length(); i++) {
          deltas.add(ProtocolAppliedWaveletDelta.parseFrom(Base64.decode(rawDeltas.getString(i))));
        }
        WaveletName waveletName = WaveletName.of(waveId, waveletId);
        LocalWaveletContainerImpl wavelet = (LocalWaveletContainerImpl) waveMap.getLocalWavelet(waveletName);
        Set<ParticipantId> participants = new HashSet<ParticipantId>();
        if (wavelet != null) {
          participants.addAll(wavelet.accessSnapshot().getParticipants());
        }
        for (ProtocolAppliedWaveletDelta appliedDelta : deltas) {
          ProtocolWaveletDelta delta = ProtocolWaveletDelta.parseFrom(
              appliedDelta.getSignedOriginalDelta().getDelta());
          long currentVersion = 0;
          if (wavelet != null) {
            currentVersion = wavelet.getCurrentVersion().getVersion();
          }
          if (currentVersion == delta.getHashedVersion().getVersion()) {
            ProtocolWaveletDelta newDelta = convertDelta(delta, domain, wavelet, waveletName,
                participants);

            waveletProvider.submitRequest(waveletName, newDelta,
                new WaveletProvider.SubmitRequestListener() {

                  @Override
                  public void onSuccess(int operationsApplied, HashedVersion hashedVersionAfterApplication,
                      long applicationTimestamp) {
                  }

                  @Override
                  public void onFailure(String errorMessage) {
                    error.write(errorMessage);
                  }
                });
            if (error.getBuffer().length() != 0) {
              break;
            }
            if (wavelet == null) {
              wavelet = (LocalWaveletContainerImpl) waveMap.getLocalWavelet(waveletName);
            }
            somethingProcessed = true;
          } else {
            somethingSkipped = true;
          }
        }
      }
    } catch (Exception ex) {
      LOG.log(Level.SEVERE, "waveId " + waveId.toString() + ", waveletId " + waveletId.toString(), ex);
      throw new IOException(ex);
    }
    response.setStatus(HttpServletResponse.SC_OK);
    if (error.getBuffer().length() != 0) {
      response.getOutputStream().write(("error : " + error.getBuffer()).getBytes());
    } else if (somethingProcessed && !somethingSkipped) {
      response.getOutputStream().write(("imported").getBytes());
    } else if (somethingProcessed && somethingSkipped) {
      response.getOutputStream().write(("appended").getBytes());
    } else {
      response.getOutputStream().write(("skipped").getBytes());
    }
  }

  /**
   * Convert delta from GWave to Wiab
   *
   * @param delta from GWave
   * @param domain target domain
   * @param wavelet to append delta
   * @param waveletName name of wavelet
   * @param set participants of wavelet at this moment
   * @return delta to import
   * @throws InvalidParticipantAddress deserialize of participant error
   */
  protected static ProtocolWaveletDelta convertDelta(ProtocolWaveletDelta delta, String domain,
      LocalWaveletContainerImpl wavelet, WaveletName waveletName,
      Set<ParticipantId> participants) throws InvalidParticipantAddress {
    ProtocolWaveletDelta.Builder newDelta = ProtocolWaveletDelta.newBuilder(delta);
    ParticipantId creator = null;
    if (wavelet != null) {
      creator = wavelet.getCreator();
    }
    ParticipantId author = makeParticipantId(delta.getAuthor(), domain);
    if (!participants.contains(author) && creator != null) {
      // Assign the authorship of the delta from internal GWave users
      // (panda@a.gwave.com, spelly@a.gwave.com) or others to the creator of wave.
      if (!author.getAddress().endsWith("@" + GWAVE_PUBLIC_DOMAIN)) {
        LOG.warning("Unknown participant " + author.getAddress()
            + ", wave " + wavelet.getWaveletName().waveId.getId());
      }
      author = creator;
    }
    newDelta.setAuthor(author.getAddress());
    for (int i = 0; i < delta.getOperationCount(); i++) {
      ProtocolWaveletOperation op = delta.getOperation(i);
      ProtocolWaveletOperation.Builder newOp = ProtocolWaveletOperation.newBuilder(op);
      if (op.hasAddParticipant()) {
        initAddParticipantOperation(newOp, op, domain, participants);
        if (creator == null && newOp.hasAddParticipant()) {
          creator = ParticipantId.of(newOp.getAddParticipant());
        }
      } else if (op.hasRemoveParticipant()) {
        initRemoveParticipantOperation(newOp, op, domain, participants);
      }
      // TODO (Andrew Kaplanov) import attachments
      newDelta.setOperation(i, newOp);
    }
    if (wavelet == null) {
      ProtocolHashedVersion ver = ProtocolHashedVersion.newBuilder(delta.getHashedVersion()).
          setHistoryHash(ByteString.copyFrom(HASH_FACTORY.createVersionZero(waveletName).getHistoryHash())).
          build();
      newDelta.setHashedVersion(ver);
    } else {
      ProtocolHashedVersion ver = ProtocolHashedVersion.newBuilder(delta.getHashedVersion()).
          setHistoryHash(ByteString.copyFrom(wavelet.getCurrentVersion().getHistoryHash())).
          build();
      newDelta.setHashedVersion(ver);
    }
    return newDelta.build();
  }

  /**
   * Convert adding participant operation.
   * Skip operation if participant already exists.
   */
  private static void initAddParticipantOperation(ProtocolWaveletOperation.Builder newOperation,
      ProtocolWaveletOperation operation, String domain,
      Set<ParticipantId> participants) throws InvalidParticipantAddress {
    ParticipantId participant = makeParticipantId(operation.getAddParticipant(), domain);
    if (!participants.contains(participant)) {
      newOperation.setAddParticipant(participant.getAddress());
      participants.add(participant);
    } else {
      newOperation.setNoOp(true);
    }
  }

  /**
   * Convert removal participant operation.
   * Skip operation if nothing to remove.
   */
  private static void initRemoveParticipantOperation(ProtocolWaveletOperation.Builder newOperation,
      ProtocolWaveletOperation operation, String domain,
      Set<ParticipantId> participants) throws InvalidParticipantAddress {
    ParticipantId participant = makeParticipantId(operation.getRemoveParticipant(), domain);
    if (participants.contains(participant)) {
      newOperation.setRemoveParticipant(participant.getAddress());
      participants.remove(participant);
    } else {
      newOperation.setNoOp(true);
    }
  }

  /**
   * Make WIAB participant Id
   *
   * @param participant in GWave
   * @param domain of WIAB server
   */
  private static ParticipantId makeParticipantId(String participant, String domain)
      throws InvalidParticipantAddress {
    int index = participant.indexOf('@');
    if (index != -1) {
      if (participant.substring(0, index).equals(GWAVE_PUBLIC_USER_NAME)
          && participant.substring(index + 1).equals(GWAVE_PUBLIC_DOMAIN)) {
        participant = WIAB_SHARED_USER_NAME + "@" + domain;
      } else {
        participant = participant.substring(0, index + 1) + domain;
      }
    }
    return ParticipantId.of(participant);
  }

  private static String readToString(Reader reader) throws FileNotFoundException, IOException {
    StringBuilder sb = new StringBuilder();
    char buf[] = new char[1000];
    for (;;) {
      int ret = reader.read(buf, 0, buf.length);
      if (ret == -1) {
        break;
      }
      sb.append(buf, 0, ret);
    }
    return sb.toString();
  }
}
