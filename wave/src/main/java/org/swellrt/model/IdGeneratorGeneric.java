package org.swellrt.model;

import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;


/**
 * Interface providing Id generators for components of a specific Wave type
 * 
 * 
 * @author pablojan@gmail.com
 * 
 */
public interface IdGeneratorGeneric {

  IdGeneratorGeneric initialize(IdGenerator idGenerator);

  WaveId newWaveId();

}
