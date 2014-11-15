package org.waveprotocol.mod.model.generic;

import org.waveprotocol.mod.model.IdGeneratorGeneric;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;

public class TypeIdGenerator implements IdGeneratorGeneric {


  private static TypeIdGenerator singleton = null;

  public static TypeIdGenerator get() {
    if (singleton == null) singleton = new TypeIdGenerator();
    return singleton;
  }

  public static TypeIdGenerator get(IdGenerator idGenerator) {
    if (singleton == null) singleton = new TypeIdGenerator();
    singleton.idGenerator = idGenerator;
    return singleton;
  }

  public static final String WAVE_ID_PREFIX = "gen";

  private IdGenerator idGenerator;

  /**
   * Private constructor
   */
  TypeIdGenerator() {

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
