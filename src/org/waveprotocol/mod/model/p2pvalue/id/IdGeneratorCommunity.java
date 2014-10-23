package org.waveprotocol.mod.model.p2pvalue.id;

import org.waveprotocol.mod.model.IdGeneratorGeneric;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;

public class IdGeneratorCommunity implements IdGeneratorGeneric {


  private static IdGeneratorCommunity singleton = null;

  public static IdGeneratorCommunity get() {
    if (singleton == null) singleton = new IdGeneratorCommunity();
    return singleton;
  }

  public static IdGeneratorCommunity get(IdGenerator idGenerator) {
    if (singleton == null) singleton = new IdGeneratorCommunity();
    singleton.idGenerator = idGenerator;
    return singleton;
  }

  public static final String WAVE_ID_PREFIX = "p2pv.c";

  private IdGenerator idGenerator;

  /**
   * Private constructor
   */
  IdGeneratorCommunity() {

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
