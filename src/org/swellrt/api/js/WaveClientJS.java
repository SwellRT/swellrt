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

         events: {
           ITEM_CHANGED: "ITEM_CHANGED",
           ITEM_ADDED: "ITEM_ADDED",
           ITEM_REMOVED: "ITEM_REMOVED",
           PARTICIPANT_ADDED: "PARTICIPANT_ADDED",
           PARTICIPANT_REMOVED: "PARTICIPANT_REMOVED"
         },

         type: {
           MAP: "MapType",
           STRING: "StringType",
           TEXT: "TextType",
           LIST: "ListType"
         },

         startSession: function(url, user, password, onSuccess, onFailure) {

            var callback = new Object();
            callback.onSuccess = onSuccess;
            callback.onFailure = onFailure;

            return delegate.@org.swellrt.api.WaveClient::
              startSession(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;
                Lcom/google/gwt/core/client/JavaScriptObject;)(url, user, password, callback);

         },

         stopSession: function() {

            return delegate.@org.swellrt.api.WaveClient::stopSession()();
         },

         openWave: function(wave, onSuccess, onFailure) {

              var callback = new Object();
              callback.onSuccess = onSuccess;
              callback.onFailure = onFailure;

              return delegate.@org.swellrt.api.WaveClient::
                openWave(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(wave, callback);
         },

         closeWave: function(waveid) {

             return delegate.@org.swellrt.api.WaveClient::close(Ljava/lang/String;)(waveid);
         },

         closeModel: function(waveid) {

             return delegate.@org.swellrt.api.WaveClient::close(Ljava/lang/String;)(waveid);
         },

         createModel: function(onSuccess, onFailure) {

            var callback= new Object();
            callback.onSuccess = onSuccess;
            callback.onFailure = onFailure;

            return delegate.@org.swellrt.api.WaveClient::
              createModel(Lcom/google/gwt/core/client/JavaScriptObject;)(callback);

         },


         openModel: function(waveId, onSuccess, onFailure) {

            var callback = new Object();
            callback.onSuccess = onSuccess;
            callback.onFailure = onFailure;

            return delegate.@org.swellrt.api.WaveClient::
              openModel(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(waveId, callback);

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

  public final native void callbackEvent(String event, final JavaScriptObject callback, Object parameter) /*-{
    callback[event](parameter);
}-*/;

}
