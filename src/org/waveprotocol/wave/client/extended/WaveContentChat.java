package org.waveprotocol.wave.client.extended;

import org.waveprotocol.wave.model.extended.type.chat.DocumentBasedChat;
import org.waveprotocol.wave.model.extended.type.chat.ObservableChat;
import org.waveprotocol.wave.model.wave.ObservableWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

public class WaveContentChat {

  private final ObservableChat wcChat;
  private final ObservableWavelet wavelet;

  protected WaveContentChat(ObservableWavelet wavelet) {

    this.wcChat = DocumentBasedChat.create(wavelet);
    this.wavelet = wavelet;
  }


  public ObservableChat getChat() {
    return wcChat;
  }

  public void addParticipant(ParticipantId participant) {
    wavelet.addParticipant(participant);
  }

  public void removeParticipant(ParticipantId participant) {
    wavelet.removeParticipant(participant);
  }

  public Set<ParticipantId> getParticipants() {
    return wavelet.getParticipantIds();
  }

  public static WaveContentChat create(WaveContentWrapper wcWrapper) {

    // String chatRootWaveletId =
    // WaveExtendedModel.CONTENT_WAVELET_CHAT_PREFIX + IdUtil.TOKEN_SEPARATOR
    // + WaveExtendedModel.CONTENT_WAVELET_ROOT;
    //
    // ObservableWavelet wavelet =
    // wcWrapper.wave.getWave().getWavelet(WaveletId.of(wcWrapper.localDomain,
    // chatRootWaveletId));
    //
    // if (wavelet == null) {
    // wavelet =
    // wcWrapper.wave.getWave().createWavelet(
    // WaveletId.of(wcWrapper.localDomain, chatRootWaveletId));
    //
    // wavelet.addParticipant(wcWrapper.loggedInUser);
    // }

    ObservableWavelet wavelet = wcWrapper.wave.getWave().getRoot();

    WaveContentChat wcChat = new WaveContentChat(wavelet);
    if (wcWrapper.isNewWave) {
      wcChat.getChat().setCreator(wcWrapper.loggedInUser);
    }


    return wcChat;
  }


}
