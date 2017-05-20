package org.waveprotocol.box.server.swell;

import org.waveprotocol.wave.model.version.HashedVersion;

/**
 *
 * 
 * @author pablojan@apache.org (Pablo Ojanguren)
 *
 */
public interface ReadableWaveletContributions {

  public HashedVersion getWaveletVersion();

  public ReadableBlipContributions getBlipContributions(String blipdId);

}
