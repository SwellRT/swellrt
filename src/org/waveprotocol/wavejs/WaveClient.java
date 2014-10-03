package org.waveprotocol.wavejs;

import com.google.gwt.core.client.Callback;

import org.waveprotocol.wave.client.extended.WaveContentWrapper;
import org.waveprotocol.wave.client.extended.type.WaveChat;
import org.waveprotocol.wavejs.js.WaveChatJS;
import org.waveprotocol.wavejs.js.WaveClientJS;

public class WaveClient {

  private final WaveJS wavejs;
  private static WaveClientJS jso = null; // TODO why static?


  protected static WaveClient create(WaveJS wavejs) {

    WaveClient waveClient = new WaveClient(wavejs);
    jso = WaveClientJS.create(waveClient);
    return waveClient;

  }

  private WaveClient(WaveJS wavejs) {
    this.wavejs = wavejs;
  }

  /**
   * Start a Wave session
   * 
   * @param url
   * @param user
   * @param password
   * @return
   */
  public boolean startSession(String url, String user, String password) {

    return wavejs.startSession(user, password, url, new Callback<String, String>() {

      @Override
      public void onSuccess(String result) {
        jso.callbackEvent("startSession", "onSuccess", result);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("startSession", "onFailure", reason);
      }
    });
  }


  /**
   * Stops the WaveSession. No callback needed.
   * 
   * @return
   */
  public boolean stopSession() {
    return wavejs.stopSession();
  }


  /**
   * Open a Wave to support a content.
   * 
   * @param wave the WaveId
   * @return the WaveId for success, null otherwise
   */
  public String openWave(final String wave) {

    return wavejs.openWave(wave, new Callback<WaveContentWrapper, String>() {

      @Override
      public void onSuccess(WaveContentWrapper result) {
        jso.callbackEvent("openWave", "onSuccess", wave);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("openWave", "onFailure", reason);
      }

    });
  }


  /**
   * Close a wave. No callback needed.
   * 
   * @param wave
   * @return true for success
   */
  public boolean closeWave(String wave) {
    return wavejs.closeWave(wave);
  }


  /**
   * Open a wave as a Chat.
   * 
   * @param wave WaveId
   * @param callback gets the WaveChatJS object on success
   * @return null if wave is not a valid WaveId. The WaveId otherwise.
   */
  public String openChat(final String wave) {


    return wavejs.openWave(wave, new Callback<WaveContentWrapper, String>() {

      @Override
      public void onSuccess(WaveContentWrapper wrapper) {

        WaveChat waveChat =
            WaveChat.create(wrapper.getWave(), wrapper.getLocalDomain(), wrapper.getLoggedInUser(),
                wrapper.isNewWave());

        WaveChatJS waveChatJS = WaveChatJS.create(waveChat);
        waveChat.getChat().addListener(waveChatJS);

        jso.callbackEvent("openChat", "onSuccess", waveChatJS);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("openChat", "onFailure", reason);
      }

    });


  }


  public String createChat() {

    return wavejs.createWave("chat", new Callback<WaveContentWrapper, String>() {

      @Override
      public void onSuccess(WaveContentWrapper wrapper) {

        WaveChat waveChat =
            WaveChat.create(wrapper.getWave(), wrapper.getLocalDomain(), wrapper.getLoggedInUser(),
                wrapper.isNewWave());

        WaveChatJS waveChatJS = WaveChatJS.create(waveChat);
        waveChat.getChat().addListener(waveChatJS);

        jso.callbackEvent("createChat", "onSuccess", waveChatJS);
      }


      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("createChat", "onFailure", reason);
      }


    });

  }


}
