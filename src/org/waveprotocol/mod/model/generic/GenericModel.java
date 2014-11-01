package org.waveprotocol.mod.model.generic;

import org.waveprotocol.mod.model.p2pvalue.id.IdGeneratorCommunity;
import org.waveprotocol.wave.model.document.ObservableDocument;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.document.util.DefaultDocEventRouter;
import org.waveprotocol.wave.model.document.util.DocEventRouter;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;


public class GenericModel {

  public static final String WAVELET_ID = "generic" + IdUtil.TOKEN_SEPARATOR + "root";

  public interface Listener {

    void onAddParticipant(ParticipantId participant);

    void onRemoveParticipant(ParticipantId participant);

  }

  private final ObservableWavelet wavelet;

  private static final String ROOT_DOC_ID = "model+root";
  private MapType root = null;


  public static GenericModel create(WaveContext wave, String domain, ParticipantId loggedInUser,
      boolean isNewWave, IdGeneratorCommunity idGenerator) {

    WaveletId waveletId = WaveletId.of(domain, WAVELET_ID);
    ObservableWavelet wavelet = wave.getWave().getWavelet(waveletId);

    if (wavelet == null) {
      wavelet = wave.getWave().createWavelet(waveletId);
      wavelet.addParticipant(loggedInUser);
    }

    return new GenericModel(wavelet, idGenerator);
  }


  protected GenericModel(ObservableWavelet wavelet, IdGeneratorCommunity idGenerator) {
    this.wavelet = wavelet;
  }


  public MapType getRoot() {

    if (root == null) {
      ObservableDocument docRoot = wavelet.getDocument(ROOT_DOC_ID);
      DocEventRouter router = DefaultDocEventRouter.create(docRoot);
      root = MapType.create(router, docRoot.getDocumentElement());
    }

    return root;
  }


}
