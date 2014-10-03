package org.waveprotocol.wave.client.extended.type;

import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.extended.WaveExtendedModel;
import org.waveprotocol.wave.model.extended.type.chat.ChatMessage;
import org.waveprotocol.wave.model.extended.type.chat.ChatPresenceStatus;
import org.waveprotocol.wave.model.extended.type.chat.DocumentBasedChat;
import org.waveprotocol.wave.model.extended.type.chat.ObservableChat;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WaveChat {

  private final ObservableChat chat;
  private final ObservableWavelet wavelet;
  private final ParticipantId me;

  protected WaveChat(ObservableWavelet wavelet, ParticipantId me) {

    this.wavelet = wavelet;
    this.chat = DocumentBasedChat.create(wavelet);
    this.me = me;

  }


  public ObservableChat getChat() {
    return chat;
  }


  public static WaveChat create(WaveContext wave, String localDomain,
      ParticipantId loggedInUser, boolean isNewWave) {

    // Use this to use wavelet chat+root
    String chatRootWaveletId =
        WaveExtendedModel.CONTENT_WAVELET_CHAT_PREFIX + IdUtil.TOKEN_SEPARATOR
            + WaveExtendedModel.CONTENT_WAVELET_ROOT;

    ObservableWavelet wavelet =
        wave.getWave().getWavelet(WaveletId.of(localDomain, chatRootWaveletId));

    if (wavelet == null) {
      wavelet =
 wave.getWave().createWavelet(WaveletId.of(localDomain, chatRootWaveletId));

      wavelet.addParticipant(loggedInUser);
    }

    // Use this to use the wavelet conv+root
    // ObservableWavelet wavelet = wcWrapper.wave.getWave().getRoot();

    WaveChat wcChat = new WaveChat(wavelet, loggedInUser);
    if (isNewWave) {
      wcChat.getChat().setCreator(loggedInUser);
    }


    return wcChat;
  }


  public boolean addParticipant(String participant) {

    try {
      wavelet.addParticipant(ParticipantId.of(participant));
    } catch (InvalidParticipantAddress e) {
      return false;
    }

    return true;
  }

  public boolean removeParticipant(String participant) {

    try {
      wavelet.removeParticipant(ParticipantId.of(participant));
    } catch (InvalidParticipantAddress e) {
      return false;
    }

    return true;


  }

  public Set<ParticipantId> getParticipants() {
    return wavelet.getParticipantIds();
  }


  public void send(String text) {
    getChat().addMessage(
        new ChatMessage("", text, System.currentTimeMillis(), me));
  }


  public int getNumMessages() {
    return getChat().numMessages();
  }


  /**
   * 
   * 
   * @param from index of the latest message (newest)
   * @param to index of the oldest message
   * @return
   */
  public List<ChatMessage> getMessages(int from, int to) {

    List<ChatMessage> messages = new ArrayList<ChatMessage>();

    int total = getChat().numMessages();

    if (from > total || to < 0) return messages;

    int marker = total;

    // to <= marker <= from

    for (ChatMessage m : getChat().getMessages()) {

      if (marker <= from && marker >= to) messages.add(m);
    }

    return messages;

  }

  public void setStatusOnline() {
    getChat().setParticipantStatus(me, ChatPresenceStatus.createOnlineStatus());
  }

  public void setStatusWriting() {
    getChat().setParticipantStatus(me, ChatPresenceStatus.createWritingStatus());
  }


}
