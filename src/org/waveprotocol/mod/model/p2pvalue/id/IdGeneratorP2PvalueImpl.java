package org.waveprotocol.mod.model.p2pvalue.id;

import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

public class IdGeneratorP2PvalueImpl implements IdGeneratorP2Pvalue {

  private IdGenerator formerIdGenerator;

  public IdGeneratorP2PvalueImpl(IdGenerator formerIdGenerator) {
    this.formerIdGenerator = formerIdGenerator;
  }

  @Override
  public WaveId newWaveId() {
    return formerIdGenerator.newWaveId();
  }

  @Override
  public WaveletId newConversationWaveletId() {
    return formerIdGenerator.newConversationWaveletId();
  }

  @Override
  public WaveletId newConversationRootWaveletId() {
    return formerIdGenerator.newConversationRootWaveletId();
  }

  @Override
  public WaveletId buildConversationRootWaveletId(WaveId waveId) {
    return formerIdGenerator.buildConversationRootWaveletId(waveId);
  }

  @Override
  public WaveletId newUserDataWaveletId(String address) {
    return formerIdGenerator.newUserDataWaveletId(address);
  }

  @Override
  public String newBlipId() {
    return formerIdGenerator.newBlipId();
  }

  @Override
  @Deprecated
  public String peekBlipId() {
    return formerIdGenerator.peekBlipId();
  }

  @Override
  public String newDataDocumentId() {
    return formerIdGenerator.newDataDocumentId();
  }

  @Override
  public String newUniqueToken() {
    return formerIdGenerator.newUniqueToken();
  }

  @Override
  public String newId(String namespace) {
    return formerIdGenerator.newId(namespace);
  }

  @Override
  public String getDefaultDomain() {
    return formerIdGenerator.getDefaultDomain();
  }

  // P2Pvalue methods

  @Override
  public WaveId newCommunityWaveId() {

    return WaveId.of(formerIdGenerator.getDefaultDomain(),
        formerIdGenerator.newId(IdGeneratorP2Pvalue.COMMUNITY_WAVE_NAMESPACE));
  }

  @Override
  public String newProjectId() {
    return newId(IdGeneratorP2Pvalue.PROJECT_DOC_PREFIX);
  }

  @Override
  public WaveletId buildCommunityRootWaveletId(WaveId waveId) {
    return WaveletId.of(waveId.getDomain(), COMMUNITY_WAVELET_ROOT);
  }


}
