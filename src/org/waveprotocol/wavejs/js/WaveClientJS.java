package org.waveprotocol.wavejs.js;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.wavejs.WaveClient;

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

         startSession: function(url, user, password, onSuccess, onFailure) {

            this.callbackMap.startSession = new Object();
            this.callbackMap.startSession.onSuccess = onSuccess;
            this.callbackMap.startSession.onFailure = onFailure;

            return delegate.@org.waveprotocol.wavejs.WaveClient::startSession(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(url, user, password);

         },

         stopSession: function() {

            return $entry(delegate.@org.waveprotocol.wavejs.WaveClient::stopSession()());
         },

         openWave: function(wave, onSuccess, onFailure) {

              this.callbackMap.openWave = new Object();
              this.callbackMap.openWave.onSuccess = onSuccess;
              this.callbackMap.openWave.onFailure = onFailure;

              return delegate.@org.waveprotocol.wavejs.WaveClient::openWave(Ljava/lang/String;)(wave);
         },

         closeWave: function(wave) {

             return delegate.@org.waveprotocol.wavejs.WaveClient::closeWave(Ljava/lang/String;)(wave);
         },


         openChat: function(wave, onSuccess, onFailure) {

            this.callbackMap.openChat = new Object();
            this.callbackMap.openChat.onSuccess = onSuccess;
            this.callbackMap.openChat.onFailure = onFailure;

            return delegate.@org.waveprotocol.wavejs.WaveClient::openChat(Ljava/lang/String;)(wave);

         },


         createChat: function(onSuccess, onFailure) {

            this.callbackMap.createChat = new Object();
            this.callbackMap.createChat.onSuccess = onSuccess;
            this.callbackMap.createChat.onFailure = onFailure;

            return delegate.@org.waveprotocol.wavejs.WaveClient::createChat()();

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
