package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.wave.model.wave.ParticipantId;

public interface Reminder {

  ParticipantId getParticipant();

  long getDatetime();

}
