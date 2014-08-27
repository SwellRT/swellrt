package org.waveprotocol.jsclient.js;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.jsclient.WaveJSClient;

public class WaveJS extends JavaScriptObject {




  /**
   * The wavejs object backed by the main Java/GWT class.
   *
   * Design ideas are:
   *
   * <li>Multiple waves opened simultaneously</li>
   * <li>Multiple collaborative contents in a wave</li>
   * <li>Provide UI for complex contents (e.g. text editor), hook in a DOM element</i>
   *
   */
  public static final native WaveJS create(WaveJSClient jclient) /*-{


    var wavejs = {


         startSession: $entry(function(url, user, password, onSuccess, onFailure) {

            this.startSession_onSuccess = onSuccess;
            this.startSession_onFailure = onFailure;
            jclient.@org.waveprotocol.jsclient.WaveJSClient::startSessionJS(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)(url, user, password);

         }),


         stopSession: $entry(function() {

            return jclient.@org.waveprotocol.jsclient.WaveJSClient::stopSession()();
         }),


         createWave: $entry(function(type) {

         }),


         openWave: $entry(function(wave, onSuccess, onFailure) {

              this.openWave_onSuccess = onSuccess;
              this.openWave_onFailure = onFailure;
              jclient.@org.waveprotocol.jsclient.WaveJSClient::openWaveJS(Ljava/lang/String;)(wave);
         }),


         closeWave: $entry(function(wave) {

             return jclient.@org.waveprotocol.jsclient.WaveJSClient::closeWave(Ljava/lang/String;)(wave);
         }),


         openChat: function(wave) {

         },


         closeChat: function(wave) {

         },


         openTextEditor: function(wave, element) {

         },


         closeTextEditor: function(wave) {

         }



    }; // wavejs

    return wavejs;

  }-*/;


  protected WaveJS() {

  }

  public final native void startSessionOnSucess(String sessionid) /*-{
    this.startSession_onSuccess(sessionid);
  }-*/;

  public final native void startSessionOnFailure(String reason) /*-{
    this.startSession_onFailure(reason);
  }-*/;

  public final native void openWaveOnSucess(String wave) /*-{
    this.openWave_onSuccess(wave);
  }-*/;

  public final native void openWaveOnFailure(String reason) /*-{
    this.openWave_onFailure(reason);
  }-*/;

}
