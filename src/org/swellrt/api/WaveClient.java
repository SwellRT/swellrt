package org.swellrt.api;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;

import org.swellrt.api.js.WaveClientJS;
import org.swellrt.api.js.editor.TextEditorJS;
import org.swellrt.api.js.generic.ModelJS;
import org.swellrt.client.WaveWrapper;
import org.swellrt.client.editor.TextEditor;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TextType;
import org.swellrt.model.generic.TypeIdGenerator;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wave.WaveDocuments;

/**
 * SwellRT client API entrypoint
 * 
 * @author Pablo Ojanguren (pablojan@gmail.com)
 * 
 * 
 */
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
  public boolean startSession(String url, String user, String password,
      final JavaScriptObject callback) {

    return swelljs.startSession(user, password, url, new Callback<String, String>() {

      @Override
      public void onSuccess(String result) {
        jso.callbackEvent("onSuccess", callback, result);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("onFailure", callback, reason);
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
  public String openWave(final String wave, final JavaScriptObject callback) {

    return swelljs.openWave(wave, new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper result) {
        jso.callbackEvent("onSuccess", callback, wave);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("onFailure", callback, reason);
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

  public String createModel(final JavaScriptObject callback) {

    return swelljs.createWave(TypeIdGenerator.get(), new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper wrapper) {

        Model model =
            Model.create(wrapper.getWave(), wrapper.getLocalDomain(), wrapper.getLoggedInUser(),
                wrapper.isNewWave(), wrapper.getIdGenerator());

        ModelJS modelJS = ModelJS.create(model);
        model.addListener(modelJS);

        jso.callbackEvent("onSuccess", callback, modelJS);

      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("onFailure", callback, reason);
      }


    });

  }


  public String openModel(String waveId, final JavaScriptObject callback) {

    return swelljs.openWave(waveId, new Callback<WaveWrapper, String>() {

      @Override
      public void onSuccess(WaveWrapper wrapper) {

        Model model =
            Model.create(wrapper.getWave(), wrapper.getLocalDomain(), wrapper.getLoggedInUser(),
                wrapper.isNewWave(), wrapper.getIdGenerator());

        ModelJS modelJS = ModelJS.create(model);
        model.addListener(modelJS);

        jso.callbackEvent("onSuccess", callback, modelJS);
      }

      @Override
      public void onFailure(String reason) {
        jso.callbackEvent("onFailure", callback, reason);
      }


    });

  }

  public TextEditorJS getTextEditor(String elementId) {
    Preconditions.checkArgument(Document.get().getElementById(elementId) != null,
        "Element id is not provided");

    TextEditor textEditor = TextEditor.create();
    textEditor.setElement(elementId);
    return TextEditorJS.create(textEditor, this);
  }

  /**
   * Set TextEditor dependencies. In particular, set the document registry
   * associated with TextType's Model before editing.
   * 
   * @param text
   */
  public void configureTextEditor(TextEditor editor, TextType text) {
    WaveDocuments<? extends InteractiveDocument> documentRegistry =
        swelljs.getDocumentRegistry(text.getModel());

    editor.setDocumentRegistry(documentRegistry);
  }

  /**
   * Enable/disable WebSockets transport. Alternative protocol is long-polling.
   * 
   * @param enabled
   */
  public void useWebSocket(boolean enabled) {
    swelljs.useWebSocket(enabled);
  }

}
