package org.waveprotocol.box.server.swell;

import org.waveprotocol.wave.model.version.HashedVersion;

/**
 * Interface to get all {@link ReadableBlipContributions} of a Wavelet.
 *
 */
public interface ReadableWaveletContributions {

  /** Get the version of the wavelet */
  public HashedVersion getWaveletVersion();

  /** Get contributions of a particular blip */
  public ReadableBlipContributions getBlipContributions(String blipdId);

}
