package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.wave.model.wave.ParticipantId;

public interface Reminder {

  public class Initialiser {

    public long datetime;
    public String participant;

    public Initialiser(String participant, long datetime) {
      this.datetime = datetime;
      this.participant = participant;
    }

  }

  ParticipantId getParticipant();

  long getDatetime();

}
