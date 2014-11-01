package org.waveprotocol.mod.wavejs;

import com.google.gwt.core.client.Callback;

import org.waveprotocol.mod.client.WaveWrapper;
import org.waveprotocol.mod.model.dummy.IdGeneratorDummy;
import org.waveprotocol.mod.model.dummy.ListModel;
import org.waveprotocol.mod.model.p2pvalue.CommunityModel;
import org.waveprotocol.mod.model.p2pvalue.id.IdGeneratorCommunity;
import org.waveprotocol.mod.model.showcase.chat.WaveChat;
import org.waveprotocol.mod.model.showcase.id.IdGeneratorChat;
import org.waveprotocol.mod.wavejs.js.WaveClientJS;
import org.waveprotocol.mod.wavejs.js.dummy.ListModelJS;
import org.waveprotocol.mod.wavejs.js.p2pvalue.CommunityModelJS;
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
   * @param waveId
   * @return true for success
   */
  public boolean close(String waveId) {
    return wavejs.close(waveId);
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

  //
  // P2Pvalue
  //

  /**
   * Creates a new Community Model. JavaScript callback is called passing the
   * Chat JS object.
   *
   * @return the WaveId or null if wave wasn't created.
   */
  public String createCommunityModel() {

    return wavejs.createWave(IdGeneratorCommunity.get(), new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper wrapper) {

        CommunityModel community =
            CommunityModel.create(wrapper.getWave(), wrapper.getLocalDomain(),
                wrapper.getLoggedInUser(), wrapper.isNewWave(),
                IdGeneratorCommunity.get(wrapper.getIdGenerator()));

        CommunityModelJS communityJS = CommunityModelJS.create(community);
        community.addListener(communityJS);

        jso.callbackEvent("createCommunityModel", "onSuccess", communityJS);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("createCommunityModel", "onFailure", reason);
      }


    });

  }




    /**
   * Opens a wave as a Community Model. JavaScript callback is called passing
   * the Chat JS object.
   *
   *
   * @param wave WaveId
   * @return null if wave is not a valid WaveId. The WaveId otherwise.
   */
  public String openCommunityModel(final String wave) {


    return wavejs.openWave(wave, new Callback<WaveWrapper, String>() {


      @Override
      public void onSuccess(WaveWrapper wrapper) {

        CommunityModel community =
            CommunityModel.create(wrapper.getWave(), wrapper.getLocalDomain(),
                wrapper.getLoggedInUser(), wrapper.isNewWave(),
                IdGeneratorCommunity.get(wrapper.getIdGenerator()));

        CommunityModelJS communityJS = CommunityModelJS.create(community);
        community.addListener(communityJS);

        jso.callbackEvent("openCommunityModel", "onSuccess", communityJS);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("openCommunityModel", "onFailure", reason);
      }

    });


  }


  //
  // Dummy types
  //


  public String createListModel() {

    return wavejs.createWave(IdGeneratorDummy.get(), new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper wrapper) {

        ListModel list =
            ListModel.create(wrapper.getWave(), wrapper.getLocalDomain(),
                wrapper.getLoggedInUser(), wrapper.isNewWave(),
                IdGeneratorDummy.get(wrapper.getIdGenerator()));


        ListModelJS listJS = ListModelJS.create(list);
        list.addListener(listJS);

        jso.callbackEvent("createListModel", "onSuccess", listJS);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("createListModel", "onFailure", reason);
      }


    });

  }


  public String openListModel(final String wave) {


    return wavejs.openWave(wave, new Callback<WaveWrapper, String>() {


      @Override
      public void onSuccess(WaveWrapper wrapper) {

        ListModel list =
            ListModel.create(wrapper.getWave(), wrapper.getLocalDomain(),
                wrapper.getLoggedInUser(), wrapper.isNewWave(),
                IdGeneratorDummy.get(wrapper.getIdGenerator()));


        ListModelJS listJS = ListModelJS.create(list);
        list.addListener(listJS);


        jso.callbackEvent("openListModel", "onSuccess", listJS);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("openListModel", "onFailure", reason);
      }

    });


  }


}
