package org.waveprotocol.wave.model.extended.id;

import org.waveprotocol.wave.model.extended.WaveType;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

public class IdGeneratorExtendedImpl implements IdGeneratorExtended {

  private final IdGenerator oldIdGenerator;

  public IdGeneratorExtendedImpl(IdGenerator oldIdGenerator) {
    this.oldIdGenerator = oldIdGenerator;
  }

  @Override
  public WaveId newWaveId() {
    return oldIdGenerator.newWaveId();
  }

  @Override
  public WaveletId newConversationWaveletId() {
    return oldIdGenerator.newConversationWaveletId();
  }

  @Override
  public WaveletId newConversationRootWaveletId() {
    return oldIdGenerator.newConversationRootWaveletId();
  }

  @Override
  public WaveletId buildConversationRootWaveletId(WaveId waveId) {
    return oldIdGenerator.buildConversationRootWaveletId(waveId);
  }

  @Override
  public WaveletId newUserDataWaveletId(String address) {
    return oldIdGenerator.newUserDataWaveletId(address);
  }

  @Override
  public String newBlipId() {
    return oldIdGenerator.newBlipId();
  }

  @Override
  public String peekBlipId() {
    return oldIdGenerator.peekBlipId();
  }

  @Override
  public String newDataDocumentId() {
    return oldIdGenerator.newDataDocumentId();
  }

  @Override
  public String newUniqueToken() {
    return oldIdGenerator.newUniqueToken();
  }

  @Override
  public String newId(String namespace) {
    return oldIdGenerator.newId(namespace);
  }

  @Override
  public String getDefaultDomain() {
    return oldIdGenerator.getDefaultDomain();
  }

  @Override
  public WaveId newWaveId(WaveType type) {
    if (type.equals(WaveType.CONVERSATION))
      return newWaveId();

    return WaveId.of(oldIdGenerator.getDefaultDomain(),
        oldIdGenerator.newId(type.getWaveIdPrefix()));
  }

}
