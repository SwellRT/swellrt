package org.waveprotocol.wave.model.account.group;

import org.waveprotocol.wave.model.account.ObservableRoles;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;

import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Group")
public interface Group extends ReadableGroup, SourcesEvents<Group.Listener> {

  /**
   * Listens to changes (addition, removal, update) of participants.
   */
  interface Listener {
    /**
     * A participant has changed.
     */
    void onChanged();
  }

  public void setName(String name);

  public void addParticipant(ParticipantId participant);

  public void removeParticipant(ParticipantId participant);

  public ObservableRoles getRoles();

}
