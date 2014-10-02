package org.waveprotocol.box.webclient.client.extended.ui;


import org.waveprotocol.wave.client.extended.type.WaveChat;
import org.waveprotocol.wave.model.extended.type.chat.ChatMessage;
import org.waveprotocol.wave.model.extended.type.chat.ChatPresenceStatus;
import org.waveprotocol.wave.model.extended.type.chat.ObservableChat;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;


public class SimpleChatPresenter implements ObservableChat.Listener, SimpleChatView.Listener {


  private final SimpleChatView view;
  private final ParticipantId participant;
  private WaveChat waveContent;

  public static SimpleChatPresenter create(SimpleChatView view, ParticipantId participant) {
    SimpleChatPresenter listPresenter = new SimpleChatPresenter(view, participant);
    listPresenter.init();
    return listPresenter;
  }

  protected SimpleChatPresenter(SimpleChatView view, ParticipantId participant) {
    this.view = view;
    this.participant = participant;
  }


  protected void init() {
    this.view.setListener(this);
  }

  public void bind(WaveChat chatContent) {
    this.waveContent = chatContent;
    this.waveContent.getChat().addListener(this);

    Set<ParticipantId> participants = this.waveContent.getParticipants();

    // Show participants
    this.view.setParticipants(participants);

    // Update participant status
    for (ParticipantId participant : participants)
      this.view.setParticipantStatus(participant,
          this.waveContent.getChat().getParticipantStatus(participant));


    // Show some previous messages
    int totalMessages = waveContent.getChat().numMessages();
    int numShowMessages = totalMessages > 10 ? 10 : totalMessages;
    /*
     * for (int i = totalMessages - numShowMessages; i < totalMessages; i++)
     * this.view.addChatLine(waveContent.getChat().getMessage(i));
     */

    // Show all messages
    for (int i = 0; i < totalMessages; i++)
      this.view.addChatLine(waveContent.getChat().getMessage(i));

  }

  //
  // Listen to the View
  //

  @Override
  public void onNewMessage(String msg) {
    waveContent.getChat().addMessage(new ChatMessage("", msg, System.currentTimeMillis(), participant));
  }


  @Override
  public void onAddParticipant(String address) {

    ParticipantId newParticipantId = null;


    if (waveContent.addParticipant(address)) {

      waveContent.getChat().addMessage(
          new ChatMessage("", address + " has joined to this conversation.", System
              .currentTimeMillis(), participant));
    }
  }

  //
  // Chat's remote events Listener
  //

  @Override
  public void onMessageAdded(ChatMessage message) {
    this.view.addChatLine(message);
  }

  @Override
  public void onParticipantAdded(ParticipantId participant) {
    this.view.setParticipants(waveContent.getParticipants());
  }

  @Override
  public void onParticipantRemoved(ParticipantId participant) {
    this.view.setParticipants(waveContent.getParticipants());
  }

  @Override
  public void startWriting() {
    /*
     * this.waveContent.getChat().setParticipantStatus(participant,
     * ChatPresenceStatus.createWritingStatus());
     */
  }

  @Override
  public void stopWriting() {
    /*
     * this.waveContent.getChat().setParticipantStatus(participant,
     * ChatPresenceStatus.createOnlineStatus());
     */
  }

  @Override
  public void onParticipantStatusChanged(ParticipantId participant, ChatPresenceStatus status) {
    this.view.setParticipantStatus(participant, status);
  }




}
