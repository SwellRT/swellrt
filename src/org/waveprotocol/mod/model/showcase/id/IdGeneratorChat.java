package org.waveprotocol.mod.model.showcase.id;

import org.waveprotocol.mod.model.IdGeneratorGeneric;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;

/**
 * Implementation for example Chat Wave data model
 * 
 * @author pablojan@gmail.com
 * 
 */
public class IdGeneratorChat implements IdGeneratorGeneric {

  private static IdGeneratorChat singleton;

  public static IdGeneratorChat get() {
    if (singleton == null) singleton = new IdGeneratorChat();
    return singleton;
  }


  public final static String WAVE_ID_PREFIX = "sc.chat";

  private IdGenerator idGenerator;

  /**
   * Private Constructor
   */
  IdGeneratorChat() {

  }

  @Override
  public IdGeneratorGeneric initialize(IdGenerator idGenerator) {
    this.idGenerator = idGenerator;
    return this;
  }

  @Override
  public WaveId newWaveId() {
    return WaveId.of(idGenerator.getDefaultDomain(), idGenerator.newId(WAVE_ID_PREFIX));
  }

}
