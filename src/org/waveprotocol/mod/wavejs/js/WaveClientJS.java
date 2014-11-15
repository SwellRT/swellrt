package org.waveprotocol.mod.wavejs.js;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.mod.wavejs.WaveClient;

public class WaveClientJS extends JavaScriptObject {




  /**
   * The JS Wave Client main interface. Backed by WaveClient
   *
   * Design ideas are:
   *
   * <li>Multiple waves opened simultaneously</li> <li>Multiple collaborative
   * contents in a wave</li> <li>Provide UI for complex contents (e.g. text
   * editor), hook in a DOM element</i>
   *
   */
  public static final native WaveClientJS create(WaveClient delegate) /*-{


    var wavejs = {

         callbackMap: new Object(),

         events: {

           ITEM_CHANGED: "ITEM_CHANGED",
           ITEM_ADDED: "ITEM_ADDED",
           ITEM_REMOVED: "ITEM_REMOVED",
           PARTICIPANT_ADDED: "PARTICIPANT_ADDED",
           PARTICIPANT_REMOVED: "PARTICIPANT_REMOVED"

         },

         startSession: function(url, user, password, onSuccess, onFailure) {

            this.callbackMap.startSession = new Object();
            this.callbackMap.startSession.onSuccess = onSuccess;
            this.callbackMap.startSession.onFailure = onFailure;

            return delegate.@org.waveprotocol.mod.wavejs.WaveClient::startSession(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(url, user, password);

         },

         stopSession: function() {

            return delegate.@org.waveprotocol.mod.wavejs.WaveClient::stopSession()();
         },

         openWave: function(wave, onSuccess, onFailure) {

              this.callbackMap.openWave = new Object();
              this.callbackMap.openWave.onSuccess = onSuccess;
              this.callbackMap.openWave.onFailure = onFailure;

              return delegate.@org.waveprotocol.mod.wavejs.WaveClient::openWave(Ljava/lang/String;)(wave);
         },

         close: function(waveid) {

             return delegate.@org.waveprotocol.mod.wavejs.WaveClient::close(Ljava/lang/String;)(waveid);
         },


         openListModel: function(wave, onSuccess, onFailure) {

            this.callbackMap.openListModel = new Object();
            this.callbackMap.openListModel.onSuccess = onSuccess;
            this.callbackMap.openListModel.onFailure = onFailure;

            return delegate.@org.waveprotocol.mod.wavejs.WaveClient::openListModel(Ljava/lang/String;)(wave);

         },


         createListModel: function(onSuccess, onFailure) {

            this.callbackMap.createListModel = new Object();
            this.callbackMap.createListModel.onSuccess = onSuccess;
            this.callbackMap.createListModel.onFailure = onFailure;

            return delegate.@org.waveprotocol.mod.wavejs.WaveClient::createListModel()();

         },

         // GENERIC MODEL API ---------------------------------------

         createModel: function(onSuccess, onFailure) {

            this.callbackMap.createModel = new Object();
            this.callbackMap.createModel.onSuccess = onSuccess;
            this.callbackMap.createModel.onFailure = onFailure;

            return delegate.@org.waveprotocol.mod.wavejs.WaveClient::createModel()();

         },


         openModel: function(waveId, onSuccess, onFailure) {

            this.callbackMap.openModel = new Object();
            this.callbackMap.openModel.onSuccess = onSuccess;
            this.callbackMap.openModel.onFailure = onFailure;

            return delegate.@org.waveprotocol.mod.wavejs.WaveClient::openModel(Ljava/lang/String;)(modelId);

         }

    }; // wavejs



    // Accessible from the Window object
    $wnd.WaveJS = wavejs;

    return wavejs;

  }-*/;




  protected WaveClientJS() {

  }




  public final native void callbackEvent(String method, String event, Object parameter) /*-{
    this.callbackMap[method][event](parameter);
  }-*/;


}
