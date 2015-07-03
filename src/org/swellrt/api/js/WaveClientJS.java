package org.swellrt.api.js;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.api.WaveClient;

public class WaveClientJS extends JavaScriptObject {

  public static final String SUCCESS = "success";
  public static final String FAILURE = "failure";

  public static final String ITEM_CHANGED = "item-changed";
  public static final String ITEM_ADDED = "item-added";
  public static final String ITEM_REMOVED = "item-removed";
  public static final String PARTICIPANT_ADDED = "participant-added";
  public static final String PARTICIPANT_REMOVED = "participant-removed";

  public static final String DATA_STATUS_CHANGED = "data-status-changed";
  public static final String NETWORK_DISCONNECTED = "network-disconnected";
  public static final String NETWORK_CONNECTED = "network-connected";
  public static final String NETWORK_CLOSED = "network-closed";

  /** This shouldn't be used. Just for debugging purpose */
  public static final String FATAL_EXCEPTION = "exception";


  public static final String METHOD_START_SESSION = "startSession";
  public static final String METHOD_OPEN_MODEL = "openModel";
  public static final String METHOD_CREATE_MODEL = "createModel";

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

         handlers: new Object(),

         events: {

           FATAL_EXCEPTION: "exception",

           SUCCESS: "success",
           FAILURE: "failure",

           ITEM_CHANGED: "item-changed",
           ITEM_ADDED: "item-added",
           ITEM_REMOVED: "item-removed",
           PARTICIPANT_ADDED: "participant-added",
           PARTICIPANT_REMOVED: "participant-removed",

           DATA_STATUS_CHANGED: "data-status-changed",
           NETWORK_DISCONNECTED: "network-disconnected",
           NETWORK_CONNECTED: "network-connected",
           NETWORK_CLOSED: "network-closed"
         },

         type: {
           MAP: "MapType",
           STRING: "StringType",
           TEXT: "TextType",
           LIST: "ListType"
         },


         on: function(event, handler) {

           this.handlers[event] = handler;

           return this;
         },

         //
         // Session
         //

         startSession: function(url, user, password, onSuccess, onFailure) {

            var callback = new Object();
            callback.success =  onSuccess;
            callback.failure =  onFailure;

            return delegate.@org.swellrt.api.WaveClient::startSession(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(url, user, password, callback);
         },

         stopSession: function() {

            return delegate.@org.swellrt.api.WaveClient::stopSession()();
         },


         //
         // Data Model
         //

         closeModel: function(waveid) {

             return delegate.@org.swellrt.api.WaveClient::closeModel(Ljava/lang/String;)(waveid);
         },

         createModel: function(onSuccess, onFailure) {

            var callback = new Object();
            callback.success =  onSuccess;
            callback.failure =  onFailure;

            return delegate.@org.swellrt.api.WaveClient::createModel(Lcom/google/gwt/core/client/JavaScriptObject;)(callback);

         },


         openModel: function(waveId, onSuccess, onFailure) {

            var callback = new Object();
            callback.success =  onSuccess;
            callback.failure =  onFailure;

            return delegate.@org.swellrt.api.WaveClient::openModel(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(waveId, callback);

         },

         //
         // Editor
         //

         editor: function(elementId) {
           return delegate.@org.swellrt.api.WaveClient::getTextEditor(Ljava/lang/String;)(elementId);
         },


         //
         // Options
         //

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




  public final native void triggerEvent(String event, Object parameter) /*-{

    if (this.handlers[event] !== undefined)
      this.handlers[event](parameter);

  }-*/;


}
