package org.swellrt.api.js;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.api.WaveClient;

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


    var swellrt = {

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

            return delegate.@org.swellrt.api.WaveClient::startSession(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(url, user, password);

         },

         stopSession: function() {

            return delegate.@org.swellrt.api.WaveClient::stopSession()();
         },

         openWave: function(wave, onSuccess, onFailure) {

              this.callbackMap.openWave = new Object();
              this.callbackMap.openWave.onSuccess = onSuccess;
              this.callbackMap.openWave.onFailure = onFailure;

              return delegate.@org.swellrt.api.WaveClient::openWave(Ljava/lang/String;)(wave);
         },

         closeWave: function(waveid) {

             return delegate.@org.swellrt.api.WaveClient::close(Ljava/lang/String;)(waveid);
         },

         closeModel: function(waveid) {

             return delegate.@org.swellrt.api.WaveClient::close(Ljava/lang/String;)(waveid);
         },

         createModel: function(onSuccess, onFailure) {

            this.callbackMap.createModel = new Object();
            this.callbackMap.createModel.onSuccess = onSuccess;
            this.callbackMap.createModel.onFailure = onFailure;

            return delegate.@org.swellrt.api.WaveClient::createModel()();

         },


         openModel: function(waveId, onSuccess, onFailure) {

            this.callbackMap.openModel = new Object();
            this.callbackMap.openModel.onSuccess = onSuccess;
            this.callbackMap.openModel.onFailure = onFailure;

            return delegate.@org.swellrt.api.WaveClient::openModel(Ljava/lang/String;)(waveId);

         },

         editor: function(elementId) {
           return delegate.@org.swellrt.api.WaveClient::getTextEditor(Ljava/lang/String;)(elementId);
         },

         useWebSocket: function(enabled) {
           delegate.@org.swellrt.api.WaveClient::useWebSocket(Z)(enabled);
         }



    }; // SwellRT



    // Accessible from the Window object
    $wnd.SwellRT = swellrt;

    return swellrt;

  }-*/;




  protected WaveClientJS() {

  }




  public final native void callbackEvent(String method, String event, Object parameter) /*-{
    this.callbackMap[method][event](parameter);
  }-*/;


}
