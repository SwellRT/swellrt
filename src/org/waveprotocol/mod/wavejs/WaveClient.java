package org.waveprotocol.mod.wavejs;

import com.google.gwt.core.client.Callback;

import org.waveprotocol.mod.client.WaveWrapper;
import org.waveprotocol.mod.model.p2pvalue.CommunityWavelet;
import org.waveprotocol.mod.model.p2pvalue.id.IdGeneratorCommunity;
import org.waveprotocol.mod.model.showcase.chat.WaveChat;
import org.waveprotocol.mod.model.showcase.id.IdGeneratorChat;
import org.waveprotocol.mod.wavejs.js.WaveClientJS;
import org.waveprotocol.mod.wavejs.js.p2pvalue.CommunityWaveletJS;
import org.waveprotocol.mod.wavejs.js.showcase.chat.WaveChatJS;

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

    return wavejs.openWave(wave, new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper result) {
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


  // Chat


  /**
   * Opens a wave as a Chat. JavaScript callback is called passing the Chat JS
   * object.
   * 
   * 
   * @param wave WaveId
   * @return null if wave is not a valid WaveId. The WaveId otherwise.
   */
  public String openChat(final String wave) {


    return wavejs.openWave(wave, new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper wrapper) {

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


  /**
   * Creates a new Chat Wave. JavaScript callback is called passing the Chat JS
   * object.
   * 
   * @return the WaveId or null if wave wasn't created.
   */
  public String createP2PvCommunity() {

    return wavejs.createWave(IdGeneratorCommunity.get(), new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper wrapper) {

        CommunityWavelet community =
            CommunityWavelet.create(wrapper.getWave(), wrapper.getLocalDomain(),
                wrapper.getLoggedInUser(), wrapper.isNewWave());

        CommunityWaveletJS communityJS = CommunityWaveletJS.create(community);
        community.addListener(communityJS);

        jso.callbackEvent("createP2PvCommunity", "onSuccess", communityJS);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("createP2PvCommunity", "onFailure", reason);
      }


    });

  }

  // P2Pvalue


  /**
   * Opens a wave as a Chat. JavaScript callback is called passing the Chat JS
   * object.
   * 
   * 
   * @param wave WaveId
   * @return null if wave is not a valid WaveId. The WaveId otherwise.
   */
  public String openP2PvCommunity(final String wave) {


    return wavejs.openWave(wave, new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper wrapper) {


        WaveChat waveChat =
            WaveChat.create(wrapper.getWave(), wrapper.getLocalDomain(), wrapper.getLoggedInUser(),
                wrapper.isNewWave());

        WaveChatJS waveChatJS = WaveChatJS.create(waveChat);
        waveChat.getChat().addListener(waveChatJS);

        jso.callbackEvent("openP2PvCommunity", "onSuccess", waveChatJS);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("openP2PvCommunity", "onFailure", reason);
      }

    });


  }


  /**
   * Creates a new Chat Wave. JavaScript callback is called passing the Chat JS
   * object.
   * 
   * @return the WaveId or null if wave wasn't created.
   */
  public String createChat() {

    return wavejs.createWave(IdGeneratorChat.get(), new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper wrapper) {

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
