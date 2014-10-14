package org.waveprotocol.mod.model.showcase.chat;

import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class WaveChat {


  public final static String WAVELET_ID = "chat" + IdUtil.TOKEN_SEPARATOR + "root";

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


    ObservableWavelet wavelet =
 wave.getWave().getWavelet(WaveletId.of(localDomain, WAVELET_ID));

    if (wavelet == null) {
      wavelet =
 wave.getWave().createWavelet(WaveletId.of(localDomain, WAVELET_ID));

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
   * Retrive a sublist of messages
   * 
   * @param from index of the oldest message
   * @param to index of the newest message
   * @return
   */
  public List<ChatMessage> getMessages(int from, int to) {

    List<ChatMessage> messages = new ArrayList<ChatMessage>();

    int total = getChat().numMessages();

    if (from > total || to < from) return messages;

    int marker = 0;

    for (ChatMessage m : getChat().getMessages()) {
      if (marker >= from && marker <= to) messages.add(m);
      marker++;
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
