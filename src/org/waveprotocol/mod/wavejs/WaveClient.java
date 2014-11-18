package org.waveprotocol.mod.wavejs;

import com.google.gwt.core.client.Callback;

import org.waveprotocol.mod.client.WaveWrapper;
import org.waveprotocol.mod.model.dummy.IdGeneratorDummy;
import org.waveprotocol.mod.model.dummy.ListModel;
import org.waveprotocol.mod.model.generic.Model;
import org.waveprotocol.mod.model.generic.TypeIdGenerator;
import org.waveprotocol.mod.wavejs.js.WaveClientJS;
import org.waveprotocol.mod.wavejs.js.dummy.ListModelJS;
import org.waveprotocol.mod.wavejs.js.generic.ModelJS;

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
    return wavejs.closeWave(waveId);
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

  //
  // Generic model
  //

  public String createModel() {

    return wavejs.createWave(TypeIdGenerator.get(), new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper wrapper) {

        Model model =
            Model.create(wrapper.getWave(), wrapper.getLocalDomain(), wrapper.getLoggedInUser(),
                wrapper.isNewWave(), wrapper.getIdGenerator());

        ModelJS modelJS = ModelJS.create(model);
        model.addListener(modelJS);

        jso.callbackEvent("createModel", "onSuccess", modelJS);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("createModel", "onFailure", reason);
      }


    });

  }


  public String openModel(String waveId) {

    return wavejs.openWave(waveId, new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper wrapper) {

        Model model =
            Model.create(wrapper.getWave(), wrapper.getLocalDomain(), wrapper.getLoggedInUser(),
                wrapper.isNewWave(), wrapper.getIdGenerator());

        ModelJS modelJS = ModelJS.create(model);
        model.addListener(modelJS);

        jso.callbackEvent("openModel", "onSuccess", modelJS);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("openModel", "onFailure", reason);
      }


    });

  }


}
