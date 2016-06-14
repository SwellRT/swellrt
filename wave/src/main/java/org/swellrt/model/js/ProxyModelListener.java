package org.swellrt.model.js;


import org.swellrt.model.generic.Model.Listener;
import org.waveprotocol.wave.model.wave.ParticipantId;

public class ProxyModelListener extends ProxyListener implements Listener {

  public static final String ON_PARTICIPANT_ADDED = "onCollaboratorAdded";
  public static final String ON_PARTICIPANT_REMOVED = "onCollaboratorRemoved";

  @Override
  public void onAddParticipant(ParticipantId participant) {
    trigger(ON_PARTICIPANT_ADDED, getAdapter().ofParticipant(participant));
  }

  @Override
  public void onRemoveParticipant(ParticipantId participant) {
    trigger(ON_PARTICIPANT_REMOVED, getAdapter().ofParticipant(participant));
  }

}
