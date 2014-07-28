package org.waveprotocol.wave.model.extended.model;

import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.Wavelet;

public interface ExtendedWaveView {

  /**
   * Creates a new wavelet in the wave with a custom id
   * 
   * @return a new wavelet.
   */
  Wavelet createWavelet(WaveletId waveletId);

}
