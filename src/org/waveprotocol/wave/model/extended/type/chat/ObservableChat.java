package org.waveprotocol.wave.model.extended.type.chat;

import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.SourcesEvents;

public interface ObservableChat extends Chat, SourcesEvents<ObservableChat.Listener> {

  public interface Listener {

    void onMessageAdded(ChatMessage message);

    void onParticipantAdded(ParticipantId participant);

    void onParticipantRemoved(ParticipantId participant);

    void onParticipantStatusChanged(ParticipantId participant, ChatPresenceStatus status);

  }


}
