package org.swellrt.api;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
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
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener.UnsavedDataInfo;

/**
 * SwellRT client API entrypoint
 *
 * @author Pablo Ojanguren (pablojan@gmail.com)
 *
 *
 */
public class WaveClient implements SwellRT.Listener {

  private final SwellRT coreClient;
  private static WaveClientJS jsClient = null; // TODO why static?


  protected static WaveClient create(SwellRT coreClient) {

    WaveClient waveClient = new WaveClient(coreClient);
    coreClient.attachListener(waveClient);
    jsClient = WaveClientJS.create(waveClient);
    return waveClient;

  }

  private WaveClient(SwellRT swell) {
    this.coreClient = swell;
  }


  private native void invoke(JavaScriptObject object, String method, Object arg) /*-{
     object[method](arg);
  }-*/;

  //
  // Session
  //

  /**
   * Create a new Wave user.
   *
   * @param host The server hosting the user: http(s)://server.com
   * @param username user address including domain part: username@server.com
   * @param password the user password
   * @param callback
   */
  public void registerUser(String host, String username, String password,
      final JavaScriptObject callback) {

    try {

      coreClient.registerUser(host, username, password, new Callback<String, String>() {

        @Override
        public void onSuccess(String result) {
          invoke(callback, WaveClientJS.SUCCESS, result);
        }

        @Override
        public void onFailure(String reason) {
          invoke(callback, WaveClientJS.FAILURE, reason);
        }
      });

    } catch (Exception e) {
      invoke(callback, WaveClientJS.FAILURE, e.getMessage());
    }

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

    boolean startOk = false;

    try {

      startOk =
          coreClient.startSession(user, password, url, new Callback<JavaScriptObject, String>() {

        @Override
            public void onSuccess(JavaScriptObject result) {
          invoke(callback, WaveClientJS.SUCCESS, result);
        }

        @Override
        public void onFailure(String reason) {
          invoke(callback, WaveClientJS.FAILURE, reason);
        }
      });

    } catch (Exception e) {
      invoke(callback, WaveClientJS.FAILURE, e.getMessage());
    }

    return startOk;
  }


  /**
   * Stops the WaveSession. No callback needed.
   *
   * @return
   */
  public boolean stopSession() {
    return coreClient.stopSession();
  }

  //
  // Data model
  //

  /**
   * Close a data model. No callback needed.
   *
   * @param waveId
   * @return true for success
   */
  public boolean closeModel(String waveId) {
    return coreClient.closeWave(waveId);
  }


  /**
   * Create a new data model.
   *
   * TODO: check if try-catch blocks are necessary
   *
   * @return the new data model Id.
   */
  public String createModel(final JavaScriptObject callback) {

    String waveId = null;

    try {

        waveId = coreClient.createWave(TypeIdGenerator.get(), new Callback<WaveWrapper, String>() {

        @Override
        public void onSuccess(WaveWrapper wrapper) {


          ModelJS modelJS = null;

          try {

            Model model =
                Model.create(wrapper.getWave(), wrapper.getLocalDomain(),
                    wrapper.getLoggedInUser(),
                  wrapper.isNewWave(), wrapper.getIdGenerator());

            modelJS = ModelJS.create(model);
            model.addListener(modelJS);

          } catch (Exception e) {
            invoke(callback, WaveClientJS.FAILURE, e.getMessage());
          }

          invoke(callback, WaveClientJS.SUCCESS, modelJS);

        }

        @Override
        public void onFailure(String reason) {
          invoke(callback, WaveClientJS.FAILURE, reason);
        }


      });

    } catch (Exception e) {
      invoke(callback, WaveClientJS.FAILURE, e.getMessage());
    }


    return waveId;

  }

  /**
   * Open a data model.
   *
   * TODO: check if try-catch blocks are necessary
   *
   * @return the new data model Id.
   */
  public String openModel(String waveId, final JavaScriptObject callback) {

    String modelId = null;

    try {

      modelId = coreClient.openWave(waveId, new Callback<WaveWrapper, String>() {

        @Override
        public void onSuccess(WaveWrapper wrapper) {

          ModelJS modelJS = null;

          try {

            Model model =
                Model.create(wrapper.getWave(), wrapper.getLocalDomain(),
                    wrapper.getLoggedInUser(), wrapper.isNewWave(), wrapper.getIdGenerator());

            modelJS = ModelJS.create(model);
            model.addListener(modelJS);

          } catch (Exception e) {
            invoke(callback, WaveClientJS.FAILURE, e.getMessage());
          }

          invoke(callback, WaveClientJS.SUCCESS, modelJS);
        }

        @Override
        public void onFailure(String reason) {
          invoke(callback, WaveClientJS.FAILURE, reason);
        }

      });

    } catch (Exception e) {
      invoke(callback, WaveClientJS.FAILURE, e.getMessage());
    }


    return modelId;

  }


  public void query(String expr, final JavaScriptObject callback) {
    coreClient.query(expr, new Callback<String, String>() {

      @Override
      public void onFailure(String reason) {
        invoke(callback, WaveClientJS.FAILURE, reason);
      }

      @Override
      public void onSuccess(String result) {
        invoke(callback, WaveClientJS.SUCCESS, JsonUtils.unsafeEval(result));
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
        coreClient.getDocumentRegistry(text.getModel());

    editor.setDocumentRegistry(documentRegistry);
  }

  /**
   * Enable/disable WebSockets transport. Alternative protocol is long-polling.
   *
   * @param enabled
   */
  public void useWebSocket(boolean enabled) {
    coreClient.useWebSocket(enabled);
  }


  @Override
  public void onDataStatusChanged(UnsavedDataInfo dataInfo) {

    JavaScriptObject payload = JavaScriptObject.createObject();

    SwellRTUtils.addField(payload, "inFlightSize", dataInfo.inFlightSize());
    SwellRTUtils.addField(payload, "uncommittedSize", dataInfo.estimateUncommittedSize());
    SwellRTUtils.addField(payload, "unacknowledgedSize", dataInfo.estimateUnacknowledgedSize());
    SwellRTUtils.addField(payload, "lastAckVersion", dataInfo.laskAckVersion());
    SwellRTUtils.addField(payload, "lastCommitVersion", dataInfo.lastCommitVersion());

    jsClient.triggerEvent(WaveClientJS.DATA_STATUS_CHANGED, payload);
  }

  @Override
  public void onNetworkDisconnected(String cause) {

    JavaScriptObject payload = JavaScriptObject.createObject();
    SwellRTUtils.addField(payload, "cause", cause);
    jsClient.triggerEvent(WaveClientJS.NETWORK_DISCONNECTED, payload);
  }

  @Override
  public void onNetworkConnected() {

    JavaScriptObject payload = JavaScriptObject.createObject();
    jsClient.triggerEvent(WaveClientJS.NETWORK_CONNECTED, payload);
  }

  @Override
  public void onNetworkClosed(boolean everythingCommitted) {

    JavaScriptObject payload = JavaScriptObject.createObject();
    SwellRTUtils.addField(payload, "everythingCommitted", everythingCommitted);

    jsClient.triggerEvent(WaveClientJS.NETWORK_CLOSED, payload);
  }

  @Override
  public void onException(String cause) {
    JavaScriptObject payload = JavaScriptObject.createObject();
    SwellRTUtils.addField(payload, "cause", cause);

    jsClient.triggerEvent(WaveClientJS.FATAL_EXCEPTION, payload);
  }


  private static native void callOnSwellRTReady() /*-{

    if (typeof $wnd.onSwellRTReady === "function")
    $wnd.onSwellRTReady();

  }-*/;

  @Override
  public void onReady() {
    callOnSwellRTReady();
  }

}
