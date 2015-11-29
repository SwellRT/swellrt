package org.swellrt.model;

import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

public interface ReadableModel {

  public String getWaveId();

  public String getWaveletId();

  public Set<ParticipantId> getParticipants();

  public ReadableMap getRoot();

  public ReadableType fromPath(String path);

}
