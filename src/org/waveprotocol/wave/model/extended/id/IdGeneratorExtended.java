package org.waveprotocol.wave.model.extended.id;

import org.waveprotocol.wave.model.extended.WaveType;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;

/**
 * A IdGenerator extension to support wave id with types
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface IdGeneratorExtended extends IdGenerator {

  /**
   * Creates a new unique wave id.
   * 
   * Conversational waves (all the waves we have today) are specified by a
   * leading token 'w' followed by a pseudo-random string, e.g. w+3dKS9cD.
   */
  WaveId newWaveId(WaveType type);

}
