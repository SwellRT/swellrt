package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;


public interface Task extends SourcesEvents<Task.Listener> {


  public class Initialiser {

    public String name;

    public Initialiser(String name) {
      this.name = name;
    }

  }

  // Name

  void setName(String name);

  String getName();

  // Status (only none or completed)

  void setStatus(String status);

  String getStatus();

  // Description

  void setDescription(String description);

  String getDescription();

  // Participants

  void addParticipant(String participant);

  void removeParticipant(String participant);

  Iterable<String> getParticipants();

  boolean isParticipantInTask(String participant);

  // Deadline (shared across all participants)

  void setDeadline(long datetime);

  long getDeadline();


  // Reminders (separated by participants, they should be placed in a private
  // wavelet)

  void addReminder(ParticipantId participant, long datetime);

  void removeReminder(Reminder reminder);

  Reminder[] getReminders(ParticipantId participant);



  public interface Listener {

    void onStatusChanged(String status);

    void onDeadlineChanged(long deadline);

    void onParticipantAdded(ParticipantId participant);

    void onParticipantRemoved(ParticipantId participant);

  }

}
