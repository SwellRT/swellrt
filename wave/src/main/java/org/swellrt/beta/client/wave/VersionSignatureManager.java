package org.swellrt.beta.client.wave;

import java.util.Map;

import org.waveprotocol.wave.client.common.util.ClientPercentEncoderDecoder;
import org.waveprotocol.wave.federation.ProtocolHashedVersion;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;

import com.google.common.base.Preconditions;

/**
 * The box server uses an incompatible signature scheme to the wave-protocol
 * libraries. This manager resolves those incompatibilities.
 */
public class VersionSignatureManager {
  private static final HashedVersionFactory HASHER =
      new HashedVersionZeroFactoryImpl(new IdURIEncoderDecoder(new ClientPercentEncoderDecoder()));

  /** Most recent signed versions. */
  private final Map<WaveletName, ProtocolHashedVersion> versions = CollectionUtils.newHashMap();

  /**
   * Records a signed server version.
   */
  public void updateHistory(WaveletName wavelet, ProtocolHashedVersion update) {
    ProtocolHashedVersion current = versions.get(wavelet);
    if (current != null && current.getVersion() > update.getVersion()) {
      RemoteWaveViewService.LOG.info("Ignoring superceded hash update: " + update);
      return;
    }
    versions.put(wavelet, update);
  }

  /**
   * Finds the most recent signed version for a delta.
   */
  public ProtocolHashedVersion getServerVersion(WaveletName wavelet, WaveletDelta delta) {
    if (delta.getTargetVersion().getVersion() == 0) {
      return WaveFactories.protocolMessageUtils.serialize(HASHER.createVersionZero(wavelet));
    } else {
      ProtocolHashedVersion current = versions.get(wavelet);
      Preconditions.checkNotNull(current);
      double prevVersion = current.getVersion();
      double deltaVersion = delta.getTargetVersion().getVersion();
      if (deltaVersion != prevVersion) {
        throw new IllegalArgumentException(
            "Client delta expressed against non-server version.  Server version: " + prevVersion
                + ", client delta: " + deltaVersion);
      }
      return current;
    }
  }
}