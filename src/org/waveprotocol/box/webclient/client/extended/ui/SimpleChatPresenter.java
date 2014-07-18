package org.waveprotocol.box.webclient.client.extended.ui;


import org.waveprotocol.wave.model.extended.type.ChatContent;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;


public class SimpleChatPresenter implements SimpleChatView.Listener,
    ChatContent.Listener {


  private final SimpleChatView view;
  private ChatContent chatBackend;


  public static SimpleChatPresenter create(SimpleChatView view) {
    SimpleChatPresenter listPresenter = new SimpleChatPresenter(view);
    listPresenter.init();
    return listPresenter;
  }

  protected SimpleChatPresenter(SimpleChatView view) {
    this.view = view;
  }


  protected void init() {
    this.view.setListener(this);
  }

  public void bind(ChatContent chatDocument) {
    this.chatBackend = chatDocument;
    this.chatBackend.addListener(this);
  }

  //
  // Listen to the View
  //

  @Override
  public void onNewMessage(String msg) {
    chatBackend.addChatLine(msg);

  }

  @Override
  public void onAddParticipant(String address) {

    ParticipantId newParticipantId = null;

    try {
      newParticipantId = ParticipantId.of(address);
      chatBackend.addParticipant(newParticipantId);
    } catch (InvalidParticipantAddress e) {
      // TODO - bad participant address
    }
  }

  //
  // Listen to the Wave's data back-end
  //


  @Override
  public void onAdd(String chatLine) {
    this.view.addChatLine(chatLine);
  }


  @Override
  public void onAddParticipant(ParticipantId participantId) {
    this.view.addParticipant(participantId);

  }

  @Override
  public void onRemoveParticipant(ParticipantId participantId) {
    this.view.removeParticipant(participantId);
  }



}
