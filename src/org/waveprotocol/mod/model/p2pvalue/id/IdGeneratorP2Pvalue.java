package org.waveprotocol.mod.model.p2pvalue.id;

import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

/**
 * 
 * 
 * @author pablojan@gmail.com
 * 
 */
public interface IdGeneratorP2Pvalue extends IdGenerator {

  public static final String COMMUNITY_WAVE_NAMESPACE = "community";
  public static final String PROJECT_DOC_PREFIX = "prj";
  public static final String COMMUNITY_WAVELET_ROOT = "community+root";

  /** Community wave id format: community+3dKS9cD */
  WaveId newCommunityWaveId();

  /** Community root wavelet id format: community+root */
  WaveletId buildCommunityRootWaveletId(WaveId waveId);

  /** Project document id format: prj+3dKS9cD */
  String newProjectId();

}
