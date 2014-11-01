package org.waveprotocol.mod.model.dummy;

import org.waveprotocol.mod.model.IdGeneratorGeneric;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;

public class IdGeneratorDummy implements IdGeneratorGeneric {


  private static IdGeneratorDummy singleton = null;

  public static IdGeneratorDummy get() {
    if (singleton == null) singleton = new IdGeneratorDummy();
    return singleton;
  }

  public static IdGeneratorDummy get(IdGenerator idGenerator) {
    if (singleton == null) singleton = new IdGeneratorDummy();
    singleton.idGenerator = idGenerator;
    return singleton;
  }

  public static final String WAVE_ID_PREFIX = "dummy";

  private IdGenerator idGenerator;

  /**
   * Private constructor
   */
  IdGeneratorDummy() {

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

  public String newDocumentId(String prefix) {
    return idGenerator.newId(prefix);
  }

}
