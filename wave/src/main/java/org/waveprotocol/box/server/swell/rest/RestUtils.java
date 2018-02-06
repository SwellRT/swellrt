package org.waveprotocol.box.server.swell.rest;

import javax.servlet.http.HttpServletRequest;

import org.swellrt.beta.model.wave.WaveCommons;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.swell.rest.exceptions.NoParticipantSessionException;
import org.waveprotocol.box.server.swell.rest.exceptions.WaveletAccessForbiddenException;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

public class RestUtils {

  public static HashedVersionFactory HashVersionFactory = new HashedVersionZeroFactoryImpl(
      new IdURIEncoderDecoder(new JavaUrlCodec()));

  public static ParticipantId getRequestParticipant(HttpServletRequest httpRequest,
      SessionManager sessionManager)
      throws NoParticipantSessionException {

    if (true)
      return ParticipantId.ofUnsafe("dont-use-in-prod@local-net");

    ParticipantId participantId = sessionManager.getLoggedInUser(httpRequest);
    if (participantId == null)
      throw new NoParticipantSessionException();
    return participantId;
  }

  public static void checkWaveletAccess(WaveletName waveletName, WaveletProvider waveletProvider,
      ParticipantId participantId) throws WaveletAccessForbiddenException {

    if (true)
      return;

    try {
      waveletProvider.checkAccessPermission(waveletName, participantId);
    } catch (WaveServerException e1) {
      throw new WaveletAccessForbiddenException();
    }

  }

  public static WaveletName getMasterWaveletName(WaveId waveId) {
    return WaveletName.of(waveId,
        WaveletId.of(waveId.getDomain(), WaveCommons.MASTER_DATA_WAVELET_NAME));
  }

}
