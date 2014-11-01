package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.wave.model.adt.ObservableBasicSet;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.wave.SourcesEvents;


public interface Task extends SourcesEvents<Task.Listener> {

  public class Initialiser {

    public String name, status, description;

    Initialiser(String name, String status, String description) {
      this.name = name;
      this.status = status;
      this.description = description;
    }

  }

  // Task Data

  void setName(String name);

  String getName();

  void setStatus(String status);

  String getStatus();

  void setDescription(String description);

  String getDescription();

  // Participants

  ObservableBasicSet<String> getParticipants();

  // Deadline (shared across all participants)

  void setDeadline(long datetime);

  long getDeadline();

  // Reminders

  ObservableElementList<Reminder, Reminder.Initialiser> getReminders();

  // Listener

  public interface Listener {

    void onStatusChanged(String status);

    void onDeadlineChanged(long deadline);

  }

}
