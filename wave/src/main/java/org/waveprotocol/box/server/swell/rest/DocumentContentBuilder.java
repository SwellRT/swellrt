package org.waveprotocol.box.server.swell.rest;

import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.frontend.CommittedWaveletSnapshot;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.ReadableBlipData;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;

public class DocumentContentBuilder {

  private static class WaveletBuilder implements Receiver<TransformedWaveletDelta> {

    final WaveletName waveletName;
    ObservableWaveletData wavelet;
    boolean started = false;
    boolean wasError = false;

    public WaveletBuilder(WaveletName waveletName) {
      this.waveletName = waveletName;
    }

    public boolean hasError() {
      return wasError;
    }

    public ReadableWaveletData getWavelet() {
      return wavelet;
    }

    @Override
    public boolean put(TransformedWaveletDelta delta) {

      try {

        if (!started) {

          wavelet = WaveletDataUtil.createEmptyWavelet(waveletName, delta.getAuthor(), // creator
              HashedVersion.unsigned(0), // garbage hash, is overwritten by
                                         // first delta below
              delta.getApplicationTimestamp()); // creation time

          WaveletDataUtil.applyWaveletDelta(delta, wavelet);

          started = true;
          return true;

        } else {

          WaveletDataUtil.applyWaveletDelta(delta, wavelet);

        }
      } catch (OperationException e) {
        e.printStackTrace();
        return false;
      }

      return true;
    }

  }

  public static String build(WaveletProvider waveletProvider, WaveletName waveletName,
      String documentId, HashedVersion version) throws WaveServerException {

    WaveletBuilder waveletBuilder = new WaveletBuilder(waveletName);

    ReadableBlipData blip = null;
    HashedVersion blipVersion = null;

    if (version == null) {

      CommittedWaveletSnapshot csnapshot = waveletProvider.getSnapshot(waveletName);
      blip = csnapshot.snapshot.getDocument(documentId);
      blipVersion = csnapshot.committedVersion;

    } else {

      waveletProvider.getHistory(waveletName,
          RestUtils.HashVersionFactory.createVersionZero(waveletName), version, waveletBuilder);

      ReadableWaveletData wavelet = waveletBuilder.getWavelet();
      blip = wavelet.getDocument(documentId);
      blipVersion = version;

    }

    return blip.getContent().getMutableDocument().toXmlString();


  }

}
