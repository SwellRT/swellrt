package org.swellrt.api.js;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.api.WaveClient;

public class WaveClientJS extends JavaScriptObject {

  public static final String SUCCESS = "success";
  public static final String FAILURE = "failure";
  public static final String READY = "ready";

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
           READY: "ready",

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

         user: {
           ANONYMOUS: "_anonymous_"
         },

         on: function(event, handler) {

           this.handlers[event] = handler;

           return this;
         },

         //
         // Users
         //

         registerUser: function(host, user, password, onSuccess, onFailure) {

            var callback = new Object();
            callback.success =  onSuccess;
            callback.failure =  onFailure;

            try {
              return delegate.@org.swellrt.api.WaveClient::registerUser(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(host, user, password, callback);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }
         },

         createUser: function(parameters, onComplete) {

            try {
              return delegate.@org.swellrt.api.WaveClient::createUser(Lcom/google/gwt/core/client/JavaScriptObject;Lorg/swellrt/api/ServiceCallback;)(parameters, onComplete);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }

         },

         updateUserProfile: function(parameters, onComplete) {

            try {
              return delegate.@org.swellrt.api.WaveClient::updateUserProfile(Lcom/google/gwt/core/client/JavaScriptObject;Lorg/swellrt/api/ServiceCallback;)(parameters, onComplete);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }

          },

          getUserProfile: function(onComplete) {

            try {
              return delegate.@org.swellrt.api.WaveClient::getUserProfile(Lorg/swellrt/api/ServiceCallback;)(onComplete);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }

          },

          setUserEmail: function(email, onSuccess, onFailure) {

            var callback = new Object();
            callback.success =  onSuccess;
            callback.failure =  onFailure;

            try {
              return delegate.@org.swellrt.api.WaveClient::setUserEmail(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(email, callback);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }
         },

         setPassword: function(id, token, newPassword, onSuccess, onFailure) {

            var callback = new Object();
            callback.success =  onSuccess;
            callback.failure =  onFailure;

            try {
              return delegate.@org.swellrt.api.WaveClient::setPassword(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(id, token, newPassword, callback);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }

         },

         recoverPassword: function(idOrEmail, recoverUrl, onSuccess, onFailure){
            var callback = new Object();
            callback.success =  onSuccess;
            callback.failure =  onFailure;
            try {
              return delegate.@org.swellrt.api.WaveClient::recoverPassword(Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(idOrEmail, recoverUrl, callback);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }

         },
         //
         // Session
         //


         login: function(parameters, onComplete) {

            try {
              return delegate.@org.swellrt.api.WaveClient::login(Lcom/google/gwt/core/client/JavaScriptObject;Lorg/swellrt/api/ServiceCallback;)(parameters, onComplete)
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }

         },

         resume: function(onComplete) {

            try {
              return delegate.@org.swellrt.api.WaveClient::resume(Lorg/swellrt/api/ServiceCallback;)(onComplete);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }

         },

         logout: function(onComplete) {

            try {
              return delegate.@org.swellrt.api.WaveClient::logout(Lorg/swellrt/api/ServiceCallback;)(onComplete);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }

         },

         startSession: function(url, user, password, onSuccess, onFailure) {

            var callback = new Object();
            callback.success =  onSuccess;
            callback.failure =  onFailure;

            try {
              return delegate.@org.swellrt.api.WaveClient::startSession(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(url, user, password, callback);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }
         },

         stopSession: function() {
            try {
              return delegate.@org.swellrt.api.WaveClient::stopSession()();
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }
         },

         resumeSession: function(onSuccess, onFailure) {

            var callback = new Object();
            callback.success =  onSuccess;
            callback.failure =  onFailure;

            try {
              return delegate.@org.swellrt.api.WaveClient::resumeSession(Lcom/google/gwt/core/client/JavaScriptObject;)(callback);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }
         },


         //
         // Data Model
         //

         closeModel: function(waveid) {
          try {
             return delegate.@org.swellrt.api.WaveClient::closeModel(Ljava/lang/String;)(waveid);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }
         },

         createModel: function(onReady, onFailure) {

            var callback = new Object();
            callback.ready =  onReady;
            callback.failure =  onFailure;

            try {
              return delegate.@org.swellrt.api.WaveClient::createModel(Lcom/google/gwt/core/client/JavaScriptObject;)(callback);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }
         },


         openModel: function(waveId, onReady, onFailure) {

            var callback = new Object();
            callback.ready =  onReady;
            callback.failure =  onFailure;

            try {
              return delegate.@org.swellrt.api.WaveClient::openModel(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(waveId, callback);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }

         },

         query: function(expr, onSuccess, onFailure) {

            var callback = new Object();
            callback.success =  onSuccess;
            callback.failure =  onFailure;
            var stringQueryExpr;

            if (typeof expr == "string") {
              expr = JSON.parse(expr);
            }

            if(expr._query){
              stringQueryExpr = JSON.stringify(expr._query);
              var stringProjExpr = JSON.stringify(expr._projection);
            }
            else if (expr._aggregate) {
              var stringAggrExpr = JSON.stringify(expr._aggregate);
            }
            else {
              stringQueryExpr = JSON.stringify(expr);
            }

           try {
             return delegate.@org.swellrt.api.WaveClient::query(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(stringQueryExpr, stringProjExpr, stringAggrExpr, callback);
            } catch (e) {
              throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }

         },


         domain: function() {

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
         },

         //
         // Notifications
         //

         notifications: {

           register: function(deviceId, onSuccess, onFailure){
             var callback = new Object();
             callback.success =  onSuccess;
             callback.failure =  onFailure;

             try {
               delegate.@org.swellrt.api.WaveClient::notificationRegister(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(deviceId, callback);
             } catch (e) {
               throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
            }
           },

           unregister: function(deviceId, onSuccess, onFailure){
             var callback = new Object();
             callback.success =  onSuccess;
             callback.failure =  onFailure;

             try {
               delegate.@org.swellrt.api.WaveClient::notificationUnregister(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(deviceId, callback);
             } catch (e) {
               throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
             }
           },

           subscribe: function(waveId, onSuccess, onFailure){
             var callback = new Object();
             callback.success =  onSuccess;
             callback.failure =  onFailure;
             try {
               delegate.@org.swellrt.api.WaveClient::notificationSubscribe(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(waveId, callback);
             } catch (e) {
               throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
             }
           },

           unsubscribe: function(waveId, onSuccess, onFailure){
             var callback = new Object();
             callback.success =  onSuccess;
             callback.failure =  onFailure;

             try {
               delegate.@org.swellrt.api.WaveClient::notificationUnsubscribe(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(waveId, callback);
             } catch (e) {
               throw @org.swellrt.api.WaveClient::wrapJavaException(Ljava/lang/Object;)(e);
             }
           },
         },

         //
         // Utils
         //

         utils: {

           avatar: function(avatars, options) {

             if (options === undefined)
              options = new Object();

             if (avatars === undefined)
               return null;

             return delegate.@org.swellrt.api.WaveClient::avatar(Lcom/google/gwt/core/client/JsArray;Lorg/swellrt/api/AvatarOptions;)(avatars, options);
           }

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
