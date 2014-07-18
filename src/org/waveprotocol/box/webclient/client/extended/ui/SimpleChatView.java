package org.waveprotocol.box.webclient.client.extended.ui;

import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;

public interface SimpleChatView {

  public interface Listener {

    void onNewMessage(String msg);

    void onAddParticipant(String address);

  }

  // Binding with presenter and
  void setListener(Listener listener);

  // Update the View
  void addChatLines(List<String> lines);

  // Update the View
  void addChatLine(String line);

  // Upadte the View
  void clearChatLines();

  void addParticipant(ParticipantId participantId);

  void removeParticipant(ParticipantId participantId);

  Widget asWidget();

}
