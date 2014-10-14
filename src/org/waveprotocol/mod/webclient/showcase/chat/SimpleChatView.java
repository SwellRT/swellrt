package org.waveprotocol.mod.webclient.showcase.chat;

import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.mod.model.showcase.chat.ChatMessage;
import org.waveprotocol.mod.model.showcase.chat.ChatPresenceStatus;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;
import java.util.Set;

public interface SimpleChatView {

  public interface Listener {

    void onNewMessage(String text);

    void onAddParticipant(String address);

    void startWriting();

    void stopWriting();

  }

  // Binding with presenter
  void setListener(Listener listener);

  void unsetListener();

  // Update the View
  void addChatLines(List<ChatMessage> lines);

  void addChatLine(ChatMessage line);

  void clearChatLines();

  void addParticipant(ParticipantId participantId);

  void removeParticipant(ParticipantId participantId);

  void setParticipants(Set<ParticipantId> participants);

  void setParticipantStatus(ParticipantId participant, ChatPresenceStatus status);

  Widget asWidget();

}
