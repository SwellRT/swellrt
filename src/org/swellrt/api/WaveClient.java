package org.swellrt.api;

import com.google.gwt.core.client.Callback;

import org.swellrt.api.js.WaveClientJS;
import org.swellrt.api.js.generic.ModelJS;
import org.swellrt.client.WaveWrapper;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TypeIdGenerator;

public class WaveClient {

  private final SwellRT swelljs;
  private static WaveClientJS jso = null; // TODO why static?


  protected static WaveClient create(SwellRT swelljs) {

    WaveClient waveClient = new WaveClient(swelljs);
    jso = WaveClientJS.create(waveClient);
    return waveClient;

  }

  private WaveClient(SwellRT swelljs) {
    this.swelljs = swelljs;
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

    return swelljs.startSession(user, password, url, new Callback<String, String>() {

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
    return swelljs.stopSession();
  }


  /**
   * Open a Wave to support a content.
   *
   * @param wave the WaveId
   * @return the WaveId for success, null otherwise
   */
  public String openWave(final String wave) {

    return swelljs.openWave(wave, new Callback<WaveWrapper, String>() {

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
    return swelljs.closeWave(waveId);
  }


  //
  // Generic model
  //

  public String createModel() {

    return swelljs.createWave(TypeIdGenerator.get(), new Callback<WaveWrapper, String>() {

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

    return swelljs.openWave(waveId, new Callback<WaveWrapper, String>() {

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
