package org.swellrt.model;

import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

public interface ReadableModel {

  public String getId();

  public WaveId getWaveId();

  public WaveletId getWaveletId();

  public Set<ParticipantId> getParticipants();

  public ReadableMap getRoot();

  public ReadableType fromPath(String path);

}
