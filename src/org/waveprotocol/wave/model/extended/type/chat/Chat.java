package org.waveprotocol.wave.model.extended.type.chat;

import org.waveprotocol.wave.model.wave.ParticipantId;



public interface Chat {

  void setCreator(ParticipantId creator);

  ParticipantId getCreator();

  void addMessage(ChatMessage message);

  int numMessages();

  Iterable<ChatMessage> getMessages();

  ChatMessage getMessage(int index);

  void setParticipantStatus(ParticipantId participant, ChatPresenceStatus status);

  ChatPresenceStatus getParticipantStatus(ParticipantId participant);


}
