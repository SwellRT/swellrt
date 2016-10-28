package org.swellrt.api;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RootPanel;

import org.swellrt.api.ServiceCallback.JavaScriptResponse;
import org.swellrt.api.js.generic.ModelJS;
import org.swellrt.client.SwellRTProfileManager;
import org.swellrt.client.WaveLoader;
import org.swellrt.client.editor.TextEditor;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TypeIdGenerator;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.box.webclient.client.ClientIdGenerator;
import org.waveprotocol.box.webclient.client.RemoteViewServiceMultiplexer;
import org.waveprotocol.box.webclient.client.WaveSocket.WaveSocketStartCallback;
import org.waveprotocol.box.webclient.client.WaveWebSocketClient;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.doodad.annotation.jso.JsoAnnotationController;
import org.waveprotocol.wave.client.doodad.widget.jso.JsoWidgetController;
import org.waveprotocol.wave.client.events.ClientEvents;
import org.waveprotocol.wave.client.events.NetworkStatusEvent;
import org.waveprotocol.wave.client.events.NetworkStatusEventHandler;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wave.WaveDocuments;
import org.waveprotocol.wave.concurrencycontrol.common.CorruptionDetail;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A generic module to manage Wave's top operations. It encapsulates
 * Wave-semantic from SwellRT data model interface.
 *
 * It should avoid JSNI/ operations.
 *
 * @author Pablo Ojanguren (pablojan@gmail.com)
 *
 */
public class SwellRT implements EntryPoint, UnsavedDataListener {

  private static final Logger log = Logger.getLogger(SwellRT.class.getName());


  public interface Listener {

    public void onDataStatusChanged(UnsavedDataInfo dataInfo);

    public void onNetworkDisconnected(String cause);

    public void onNetworkConnected();

    public void onNetworkClosed(boolean everythingCommitted);

    public void onException(String cause);

    public void onReady();


  }

  public static class LoginResponse extends JavaScriptObject {


    protected LoginResponse() {

    }

    public final native String getSessionId() /*-{
      return this.sessionId;
    }-*/;

    public final native String getParticipantId() /*-{
      return this.participantId;
    }-*/;

  }

  public static class RequestAccessResponse extends JavaScriptObject {

    protected RequestAccessResponse() {

    }

    public final native boolean canRead() /*-{
      return this.canRead;
    }-*/;

    public final native boolean canWrite() /*-{
      return this.canWrite;
    }-*/;

  }

  private static String CHARSET = "utf-8";


  /* Components shared across sessions */
  private final SchemaProvider schemaProvider;

  /* Objects depending on the user session */
  private ParticipantId loggedInUser = null;
  private String sessionId = null;
  private String seed = null;
  private RemoteViewServiceMultiplexer channel;

  private String waveDomain;

  /**
   * This URL doesn't end with "/". Example: http://localhost:9898
   */
  private String baseServerUrl = SwellRTUtils.getBaseUrl();

  private WaveWebSocketClient websocket;


  /** List of living waves for the active session. */
  private Map<WaveId, WaveLoader> waveRegistry = CollectionUtils.newHashMap();

  /** List of living collab objects with waves as substrate  */
  private Map<WaveId, ModelJS> objectRegistry = CollectionUtils.newHashMap();

  /** List of editors created in the app */
  private Map<Element, TextEditor> editorRegistry = CollectionUtils.newHashMap();
  
  private SwellRTProfileManager profileManager;  

  /** A listener to global data/network/runtime events */
  private SwellRT.Listener listener = null;

  private boolean useWebSocket = true;

  private boolean shouldOpenWebsocket = true;


  /**
   * Default constructor
   */
  public SwellRT() {
    this.schemaProvider = new ConversationSchemas();
    Timing.setEnabled(false);
  }

  /**
   *
   * @param listener
   */
  public void attachListener(SwellRT.Listener listener) {
    this.listener = listener;
  }


  protected void cleanSessionData() {
    sessionId = null;
    loggedInUser = null;
    BrowserSession.clearUserData();
  }

  protected void cleanChannelData() {
    // Destroy all waves
    for (Entry<WaveId, WaveLoader> entry : waveRegistry.entrySet())
      entry.getValue().destroy();

    waveRegistry.clear();
    websocket.disconnect(true);
    channel = null;
  }
  
  /**
   * Calls server's echo service to check if
   * session cookie is being sent by the browser.
   * It allows to check if 3rd party cookies are
   * blocked by the browser and then use URL params
   * to propagate session.
   * 
   * @param callback
   * @throws RequestException 
   */
  protected void echo(RequestCallback callback) {
    
    String url = baseServerUrl + "/swell/echo";

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.GET, url);
    builder.setHeader("Content-Type", "text/plain; charset=utf-8");
    try {
      builder.sendRequest("", callback);    
    } catch (RequestException e) {
      callback.onError(null, e);
    }
  }

  /** 
   * @return profile manager instance.
   */
  protected ProfileManager getProfileManager() {
    if (this.profileManager == null) {
      this.profileManager = new SwellRTProfileManager(this);
    }
    return this.profileManager;  
  }

  public void login(JavaScriptObject parameters, ServiceCallback _callback)
      throws RequestException {

    if (_callback == null)
      _callback = ServiceCallback.getVoidCallback();

    final ServiceCallback callback = _callback;

    JsoView jsParameters = JsoView.as(parameters);
    String participantId = null;
    if (jsParameters != null)
      participantId = jsParameters.getString("id");

    String url = baseServerUrl + "/swell/auth/"+(participantId != null ? participantId : "");

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);
    builder.setHeader("Content-Type", "text/plain; charset=utf-8");
    builder.sendRequest(ServiceParameters.toJSON(parameters), new RequestCallback() {

      @Override
      public void onResponseReceived(Request request, Response response) {

        if (response.getStatusCode() != 200)
          callback.onComplete(ServiceCallback.JavaScriptResponse.error(response.getText()));
        else {
          
          // Clean everything before setting new session
          cleanSessionData();
          shouldOpenWebsocket = true;
          if (websocket != null)
            websocket.disconnect(true);

          JavaScriptResponse responseData =
              ServiceCallback.JavaScriptResponse.success(response.getText());

          loggedInUser = ParticipantId.ofUnsafe(responseData.getValue("id"));
          sessionId = responseData.getValue("sessionId");
          waveDomain = responseData.getValue("domain");
          seed = SwellRTUtils.nextBase64(10);
          
   
          
          echo(new RequestCallback() {

            @Override
            public void onResponseReceived(Request request, Response response) {
              
              boolean sessionCookie = false;
              
              if (response.getStatusCode() == 200) {
              
                JavaScriptResponse echoResponseData =
                    ServiceCallback.JavaScriptResponse.success(response.getText());

                sessionCookie = echoResponseData.getBoolean("sessionCookie");                
              }
  
              BrowserSession.setUserData(loggedInUser.getDomain(), loggedInUser.getAddress(), seed,
                  sessionId, BrowserSession.getWindowId(), sessionCookie);
              
              // Do this after window.__session is set
              TypeIdGenerator.get().initialize(ClientIdGenerator.create());   
              
              callback.onComplete(responseData);
            }

            @Override
            public void onError(Request request, Throwable exception) {
              
              // Fall into safe state, if we cannot ensure cookies are received in the server
              // force URL rewriting
              BrowserSession.setUserData(loggedInUser.getDomain(), loggedInUser.getAddress(), seed,
                  sessionId, BrowserSession.getWindowId(), false);
              
              // Do this after window.__session is set
              TypeIdGenerator.get().initialize(ClientIdGenerator.create());   
              
              callback.onComplete(responseData);
            }
            
          });
        }

      }

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onComplete(ServiceCallback.JavaScriptResponse.error("SERVICE_EXCEPTION",
            exception.getMessage()));
      }
    });

  }

  public void resume(JavaScriptObject parameters, ServiceCallback _callback)
      throws RequestException {

    if (_callback == null)
      _callback = ServiceCallback.getVoidCallback();

    final ServiceCallback callback = _callback;

    JsoView jsParameters = JsoView.as(parameters);
    String participantId = null;
    if (jsParameters != null)
      participantId = jsParameters.getString("id");

    String url = baseServerUrl + "/swell/auth/"+(participantId != null ? participantId : "");
    url = BrowserSession.addSessionToUrl(url);

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.GET, url);
    builder.setHeader("Content-Type", "text/plain; charset=utf-8");
    builder.sendRequest("{}", new RequestCallback() {

      @Override
      public void onResponseReceived(Request request, Response response) {

        if (response.getStatusCode() != 200)
          callback.onComplete(ServiceCallback.JavaScriptResponse.error(response.getText()));
        else {

          // Clean everything before setting new session
          shouldOpenWebsocket = true;
          if (websocket != null)
            websocket.disconnect(true);

          JavaScriptResponse responseData =
              ServiceCallback.JavaScriptResponse.success(response.getText());

          loggedInUser = ParticipantId.ofUnsafe(responseData.getValue("id"));
          sessionId = responseData.getValue("sessionId");
          waveDomain = responseData.getValue("domain");
          seed = SwellRTUtils.nextBase64(10);

          // If session is resumed we can ensure that session cookie is received by the server
          // lets set isCookieEnabled = true
          BrowserSession.setUserData(loggedInUser.getDomain(), loggedInUser.getAddress(), seed,
              sessionId, BrowserSession.getWindowId(), true);

          // Init Id generator
          TypeIdGenerator.get().initialize(ClientIdGenerator.create());

          callback.onComplete(responseData);
        }

      }

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onComplete(ServiceCallback.JavaScriptResponse.error("SERVICE_EXCEPTION",
            exception.getMessage()));
      }
    });

  }


  public void logout(JavaScriptObject parameters, ServiceCallback _callback) throws RequestException {

    if (_callback == null)
      _callback = ServiceCallback.getVoidCallback();

    final ServiceCallback callback = _callback;

    //
    // Clean session, websocket ,objects and registries
    //

    try {

      for (TextEditor editor : editorRegistry.values())
        if (!editor.isClean())
          editor.cleanUp();
      editorRegistry.clear();

      for (ModelJS co : objectRegistry.values())
        SwellRTUtils.deleteJsObject(co);
      objectRegistry.clear();

      for (WaveLoader wave : waveRegistry.values())
        wave.destroy();
      waveRegistry.clear();

      shouldOpenWebsocket = true;
      if (websocket != null) {
        websocket.disconnect(false);
        websocket = null;
      }

      //
      // Call server to close remote session
      //

      JsoView jsParameters = JsoView.as(parameters);
      String participantId = null;
      if (jsParameters != null)
       participantId = jsParameters.getString("id");

      String url = baseServerUrl + "/swell/auth/"+ (participantId != null ? participantId : "");
      url = BrowserSession.addSessionToUrl(url);

      RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.DELETE, url);
      builder.setHeader("Content-Type", "text/plain; charset=utf-8");
      builder.sendRequest("{}", new RequestCallback() {

        @Override
        public void onResponseReceived(Request request, Response response) {

          if (response.getStatusCode() != 200)
            callback.onComplete(ServiceCallback.JavaScriptResponse.error(response.getText()));
          else {
            callback.onComplete(ServiceCallback.JavaScriptResponse.success(response.getText()));
          }

        }

        @Override
        public void onError(Request request, Throwable exception) {
          callback.onComplete(ServiceCallback.JavaScriptResponse.error("SERVICE_EXCEPTION",
              exception.getMessage()));
        }
      });

    } catch (RuntimeException e) {

      // TODO

    } finally {

      cleanSessionData();
    }

  }

  protected void openWebsocket(final Callback<Void, Void> callback) {
    Preconditions.checkArgument(loggedInUser != null, "User not logged in. Can't open websocket.");

    // this is needed to atmosphere to work
    setWebsocketAddress(baseServerUrl);

    // Use Model.MODEL_VERSION to get the client version
    websocket = new WaveWebSocketClient(SwellRTUtils.toWebsocketAddress(baseServerUrl), "1.0");
    websocket.connect(new WaveSocketStartCallback() {

      @Override
      public void onSuccess() {
        channel = new RemoteViewServiceMultiplexer(websocket, loggedInUser.getAddress());
        shouldOpenWebsocket = false;
        callback.onSuccess((Void) null);
      }

      @Override
      public void onFailure() {
        callback.onFailure((Void) null);

      }
    });

  }

  private void openProc(WaveId waveId, final Callback<WaveLoader, String> callback) {

     final WaveLoader wave =
          new WaveLoader(WaveRef.of(waveId), channel, TypeIdGenerator.get()
              .getUnderlyingGenerator(), waveDomain, Collections.<ParticipantId> emptySet(),
              loggedInUser, this);

    if (wave.isLoaded()) {
      callback.onSuccess(wave);
    } else {

      try {

        wave.load(new Command() {
          @Override
          public void execute() {
            callback.onSuccess(wave);
          }
        });

        } catch(RuntimeException e) {
            callback.onFailure(e.getMessage());
        }
    }


  }

  /**
   * Open or create a collaborative object.
   * The underlying websocket will be openend if it is necessary.
   *
   * @param parameters field "id" for collab object id or void to create a new one
   * @param callback
   * @throws RequestException
   */
  public void open(JavaScriptObject parameters, ServiceCallback _callback) throws RequestException {

    if (_callback == null)
      _callback = ServiceCallback.getVoidCallback();

    final ServiceCallback callback = _callback;

    Preconditions.checkArgument(loggedInUser != null, "Login is not present");

    JsoView p = JsoView.as(parameters);

    WaveId id = null;
    if (p.getString("id") != null) {
      id = WaveId.deserialise(p.getString("id"));
    } else {
      id = TypeIdGenerator.get().newWaveId();
    }

    final WaveId waveId = id;

    final Callback<WaveLoader, String> openProcCallback = new  Callback<WaveLoader, String>() {

      @Override
      public void onFailure(String reason) {
        callback.onComplete(ServiceCallback.JavaScriptResponse.error("SERVICE_EXCEPTION", reason));
      }

      @Override
      public void onSuccess(WaveLoader wave) {

        waveRegistry.put(waveId, wave);

        ModelJS cobJsFacade = null;

        Model cob =
          Model.create(wave.getWave(), wave.getLocalDomain(),
                  wave.getLoggedInUser(),
                wave.isNewWave(), wave.getIdGenerator());

          cobJsFacade = ModelJS.create(cob);
          cob.addListener(cobJsFacade);

          objectRegistry.put(waveId, cobJsFacade);

          callback.onComplete(ServiceCallback.JavaScriptResponse.success(cobJsFacade));
      }

    };

    if (waveRegistry.containsKey(waveId)) {
      ModelJS cobJsFacade = objectRegistry.get(waveId);

      if (cobJsFacade != null)
        callback.onComplete(ServiceCallback.JavaScriptResponse.success(cobJsFacade));
      else
        callback.onComplete(ServiceCallback.JavaScriptResponse.error("SERVICE_EXCEPTION", "Object is open but no native facade found"));

      return; // don't continue
    }


    if (shouldOpenWebsocket) {
      openWebsocket(new Callback<Void, Void>() {


        @Override
        public void onFailure(Void reason) {
          callback.onComplete(ServiceCallback.JavaScriptResponse.error("WEBSOCKET_ERROR", "Websocket can't be open"));
        }

        @Override
        public void onSuccess(Void result) {
          openProc(waveId, openProcCallback);
        }
      });
    } else {
      openProc(waveId, openProcCallback);
    }

  }

  private native String extractWaveIdParameter(JavaScriptObject parameters) /*-{

    if (parameters == null || parameters === undefined)
      return null;

    if (typeof parameters == "string")
      return parameters;

    if (parameters.id && typeof parameters.id == "function")
       return parameters.id();

    if (parameters.id && typeof parameters.id == "string")
       return parameters.id;

    return null;
  }-*/;


  public void close(JavaScriptObject parameters, ServiceCallback callback) throws RequestException {

    if (callback == null)
      callback = ServiceCallback.getVoidCallback();

    String id = extractWaveIdParameter(parameters);
    Preconditions.checkArgument(id != null, "Missing object or id");
    WaveId waveId = WaveId.deserialise(id);

    if (!waveRegistry.containsKey(waveId)) {
      return;
    }

    for (TextEditor e: editorRegistry.values())
      if (!e.isClean())
        if (e.getWaveId().equals(waveId))
          e.cleanUp();

    ModelJS co = objectRegistry.get(waveId);
    objectRegistry.remove(waveId);
    SwellRTUtils.deleteJsObject(co);

    waveRegistry.remove(waveId).destroy();
  }


  public TextEditor createTextEditor(Element parent, StringMap<JsoWidgetController> widgetControllers, StringMap<JsoAnnotationController> annotationControllers) {

    TextEditor textEditor = TextEditor.create(parent,
        widgetControllers,
        annotationControllers);

    editorRegistry.put(parent, textEditor);

    return textEditor;
  }

  //
  // *******************************************************************************
  //

  /**
   * Performs a login against Wave's /auth servlet. This method doesn't start a
   * web socket session. Wave server will set a session cookie.
   *
   * CORS and XHR is taken into account:
   * http://stackoverflow.com/questions/10977058
   * /xmlhttprequest-and-set-cookie-cookie
   *
   * @param user name of the user with or without domain part
   * @param callback
   * @throws RequestException
   */
  @Deprecated
  private void login(final String user, final String password,
      final Callback<JavaScriptObject, String> callback) throws RequestException {


    String query = URL.encodeQueryString("address") + "=" + user;
    query += "&password=" + URL.encodeQueryString(password);

    // redirect to the profile servlet (json output)
    // query += "&r=" + URL.encodeQueryString("/profile/?adresses=");

    String url = baseServerUrl + "/auth/signin?r=none";
    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);
    builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
    builder.sendRequest(query, new RequestCallback() {

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onFailure(exception.getMessage());
      }

      @Override
      public void onResponseReceived(Request request, Response response) {

        // xmlHTTPResquest object doesn't handle 302 properly
        if (response.getStatusCode() == 200) {

          // Get full user address and session id from the server
          LoginResponse responseData = JsonUtils.<LoginResponse> safeEval(response.getText());
          loggedInUser = ParticipantId.ofUnsafe(responseData.getParticipantId());
          sessionId = responseData.getSessionId();

          String seed = SwellRTUtils.nextBase64(10);
          waveDomain = loggedInUser.getDomain();
        
          echo(new RequestCallback() {

            @Override
            public void onResponseReceived(Request request, Response response) {
              
              

              boolean sessionCookie = false;
              
              if (response.getStatusCode() == 200) {
              
                JavaScriptResponse echoResponseData =
                    ServiceCallback.JavaScriptResponse.success(response.getText());
                
                sessionCookie = echoResponseData.getBoolean("sessionCookie");                
              }
  
              JavaScriptObject sessionWeb =
                BrowserSession.setUserData(loggedInUser.getDomain(), loggedInUser.getAddress(), seed,
                    sessionId, BrowserSession.getWindowId(), sessionCookie);
              
              
              // Do this after window.__session is set
              TypeIdGenerator.get().initialize(ClientIdGenerator.create());   
              
              callback.onSuccess(sessionWeb);
            }

            @Override
            public void onError(Request request, Throwable exception) {
              
              // Fall into safe state, if we cannot ensure cookies are received in the server
              // force URL rewriting
              JavaScriptObject sessionWeb =
                  BrowserSession.setUserData(loggedInUser.getDomain(), loggedInUser.getAddress(), seed,
                      sessionId, BrowserSession.getWindowId(), false);
              
              // Do this after window.__session is set
              TypeIdGenerator.get().initialize(ClientIdGenerator.create());   
              
              callback.onSuccess(sessionWeb);
            }
            
          });
          
          

        } else if (response.getStatusCode() == 403) {
          loggedInUser = null;
          log.log(Level.SEVERE, "Error Login Servlet: " + response.getStatusText());
          callback.onFailure("ACCESS_FORBIDDEN_EXCEPTION");

        } else {
          loggedInUser = null;
          log.log(Level.SEVERE, "Error Login Servlet: " + response.getStatusText());
          callback.onFailure("SERVICE_EXCEPTION");
        }

      }

    });

  }


  /**
   * Initialize the infrastructure of communications with the wave server for
   * the logged in user. It seems we need a new Web socket client instance for
   * each user session. Not sure how to close a websocket.
   */
  private void startComms(final Callback<Void, Void> callback) {

    assert (loggedInUser != null);

    log.log(Level.INFO, "Starting wave session...");

    // this is needed to atmosphere to work
    setWebsocketAddress(this.baseServerUrl);


    String webSocketURL = baseServerUrl + "/";

    if (webSocketURL.startsWith("http://"))
      webSocketURL = webSocketURL.replace("http://", "ws://");
    else if (webSocketURL.startsWith("https://"))
      webSocketURL = webSocketURL.replace("https://", "wss://");

    // Use Model.MODEL_VERSION to get the client version
    websocket = new WaveWebSocketClient(webSocketURL, "1.0");
    websocket.connect(new WaveSocketStartCallback() {

      @Override
      public void onSuccess() {

        channel = new RemoteViewServiceMultiplexer(websocket, loggedInUser.getAddress());
        callback.onSuccess((Void) null);
      }

      @Override
      public void onFailure() {
        callback.onFailure((Void) null);

      }
    });


  }

  private void stopComms() {
    websocket.disconnect(true);
    channel = null;
  }


  private native boolean websocketNotAvailable() /*-{
   return !window.WebSocket;
  }-*/;

  private native void setWebsocketAddress(String address) /*-{
    $wnd.__websocket_address = address;
  }-*/;


  private boolean isSessionStarted() {
    return loggedInUser != null;
  }


  /*******************************************************************************************/


  public void useWebSocket(boolean enabled) {
    useWebSocket = enabled;
  }

  /**
   * Create a new Wave user.
   *
   * @param host The server hosting the user: http(s)://server.com
   * @param username user address including domain part or not:
   *        username@server.com
   * @param password the user password
   * @param callback
   * @throws RequestException
   */
  public void registerUser(final String host, final String username, final String password,
      final Callback<String, String> callback) throws RequestException {

    String url = host.endsWith("/") ? host + "auth" : host + "/" + "auth";
    url = BrowserSession.addSessionToUrl(url);
    String queryStr = "address=" + URL.encode(username) + "&password=" + URL.encode(password);

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);
      builder.setHeader("Accept-Charset", CHARSET);
      builder.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET);

      builder.sendRequest(queryStr, new RequestCallback() {

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onFailure(exception.getMessage());
      }

      @Override
      public void onResponseReceived(Request request, Response response) {

        // xmlHTTTPResquest object doesn't handle 302 properly
        if (response.getStatusCode() == 200) {
          callback.onSuccess(response.getStatusText());
        } else if (response.getStatusCode() == 403) {
          log.log(Level.WARNING,
              "Error registering new user " + username + ": " + response.getStatusText());
          callback.onFailure("INVALID_USERNAME_EXCEPTION");
        } else {
          log.log(Level.WARNING,
              "Error registering new user " + username + ": " + response.getStatusText());
          callback.onFailure("SERVICE_EXCEPTION");
        }

      }

    });


  }

  /**
   * Login the user in and start communications with Wave provider
   *
   * @param url to the Wave server, https://local.net:9898
   * @param user
   * @param password
   * @param callback
   * @return
   * @throws RequestException
   * @throws InvalidUrlException
   */
  @Deprecated
  public void startSession(String user, String password, final String url,
      final Callback<JavaScriptObject, String> callback) throws RequestException {


    login(user, password, new Callback<JavaScriptObject, String>() {

      @Override
      public void onFailure(String reason) {
        callback.onFailure(reason);
      }

      @Override
      public void onSuccess(final JavaScriptObject result) {

        startComms(new Callback<Void, Void>() {

          @Override
          public void onFailure(Void none) {
            callback.onFailure("NETWORK_EXCEPTION");

            // Clear user session
            BrowserSession.clearUserData();
            loggedInUser = null;
            sessionId = null;
          }

          @Override
          public void onSuccess(Void none) {
            callback.onSuccess(result);
          }
        });
      }

    });

  }

  /**
   * A temporary method to resume sessions. StartSession, StopSession and
   * ResumeSession methods will be replaced by login(), resume() and logut()
   *
   *
   * @param callback
   * @throws RequestException
   */
  @Deprecated
  public void resumeSession(final Callback<JavaScriptObject, String> callback)
      throws RequestException {

    String url = baseServerUrl + "/swell/auth";
    url = BrowserSession.addSessionToUrl(url);

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.GET, url);
    builder.setHeader("Content-Type", "text/plain; charset=utf-8");
    builder.sendRequest("{}", new RequestCallback() {

      @Override
      public void onResponseReceived(Request request, final Response response) {

        if (response.getStatusCode() != 200)
          callback.onFailure("ACCESS_FORBIDDEN_EXCEPTION");
        else {

          final JavaScriptResponse responseData =
              ServiceCallback.JavaScriptResponse.success(response.getText());

          loggedInUser = ParticipantId.ofUnsafe(responseData.getValue("id"));
          sessionId = responseData.getValue("sessionId");
          waveDomain = responseData.getValue("domain");
          seed = SwellRTUtils.nextBase64(10);

          // If session is resumed we can ensure that session cookie is received by the server
          // lets set isCookieEnabled = true
          BrowserSession.setUserData(loggedInUser.getDomain(), loggedInUser.getAddress(), seed,
              sessionId, BrowserSession.getWindowId(), true);

          // Init Id generator
          TypeIdGenerator.get().initialize(ClientIdGenerator.create());

          startComms(new Callback<Void, Void>() {

            @Override
            public void onFailure(Void none) {
              callback.onFailure("SERVICE_EXCEPTION");

              cleanChannelData();
              cleanSessionData();
            }

            @Override
            public void onSuccess(Void none) {
              callback.onSuccess(responseData);
            }
          });
        }
      }

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onFailure("SERVICE_EXCEPTION");
      }
    });

  }

  /**
   * Logout user and close communications with Wave provider
   *
   * @return
   * @throws SessionNotStartedException
   */
  @Deprecated
  public void stopSession() throws SessionNotStartedException {

    if (loggedInUser == null) throw new SessionNotStartedException();

    // Destroy all waves
    for (Entry<WaveId, WaveLoader> entry : waveRegistry.entrySet())
      entry.getValue().destroy();

    waveRegistry.clear();

    // Disconnect from Wave's websocket
    stopComms();

    // Clear user session
    sessionId = null;
    loggedInUser = null;
    BrowserSession.clearUserData();
  }

  protected WaveLoader getWaveWrapper(WaveId waveId, boolean isNew) {

    if (!waveRegistry.containsKey(waveId) || waveRegistry.get(waveId).isClosed()) {
      WaveLoader ww =
          new WaveLoader(WaveRef.of(waveId), channel, TypeIdGenerator.get()
              .getUnderlyingGenerator(), waveDomain, Collections.<ParticipantId> emptySet(),
              loggedInUser, this);
      waveRegistry.put(waveId, ww);
    }

    return waveRegistry.get(waveId);
  }


  public String createWave(final OnLoadCallback<WaveLoader> callback) throws NetworkException,
      SessionNotStartedException {

    if (!isSessionStarted()) {
      throw new SessionNotStartedException();
    }

    if (!websocket.isConnected()) {
      throw new NetworkException();
    }



    final WaveId waveId = TypeIdGenerator.get().newWaveId();
    final WaveLoader waveWrapper = getWaveWrapper(waveId, true);

    if (waveWrapper.isLoaded()) {
      callback.onLoad(waveWrapper);
    } else {
      waveWrapper.load(new Command() {
        @Override
        public void execute() {
          callback.onLoad(waveWrapper);
        }
      });
    }
    return waveId.serialise();
  }


  /**
   * Open an existing wave.
   *
   * @param strWaveId WaveId
   * @param callback
   * @return null if wave is not a valid WaveId. The WaveId otherwise.
   * @throws InvalidIdException
   * @throws NetworkException
   * @throws SessionNotStartedException
   * @throws RequestException
   */
  public String openWave(final String strWaveId, final OnLoadCallback<WaveLoader> callback)
      throws InvalidIdException, NetworkException, SessionNotStartedException, RequestException {

    final WaveId waveId = WaveId.deserialise(strWaveId);

    if (!isSessionStarted()) {
      throw new SessionNotStartedException();
    }

    if (!websocket.isConnected()) {
      throw new NetworkException();
    }


    final WaveLoader waveWrapper = getWaveWrapper(waveId, false);

    if (waveWrapper.isLoaded()) {
      callback.onLoad(waveWrapper);
    } else {
      waveWrapper.load(new Command() {
        @Override
        public void execute() {
          callback.onLoad(waveWrapper);
        }
      });

    }


    return strWaveId;
  }




  public void closeWave(String waveIdStr) throws InvalidIdException, SessionNotStartedException {

    if (!isSessionStarted()) {
      throw new SessionNotStartedException();
    }

    WaveId waveId = null;

    try {
      waveId = WaveId.deserialise(waveIdStr);
    } catch (Exception e) {
      throw new InvalidIdException();
    }

    WaveLoader waveWrapper = waveRegistry.get(waveId);

    if (waveWrapper == null) throw new InvalidIdException();

    waveWrapper.destroy();
    waveRegistry.remove(waveId);

  }


  protected WaveDocuments<? extends InteractiveDocument> getDocumentRegistry(WaveId waveId) {
    Preconditions.checkArgument(waveId != null,
        "Can't get document registry from null object id");
    Preconditions.checkArgument(waveRegistry.containsKey(waveId),
        "No object registered for the id. Can't get document registry.");

    return waveRegistry.get(waveId).getDocumentRegistry();
  }


  @Deprecated
  private static native void dirtyLog(String msg) /*-{
    $wnd.console.log(msg);
  }-*/;

  @Deprecated
  private static native void notifyException(Throwable e) /*-{
    if (typeof $wnd.onSwellRTException === "function")
      $wnd.onSwellRTException(e);
  }-*/;

  @Deprecated
  private static native void notifyOnUpdate(int inFlightSize, int notAckedSize,
      int unCommitedSize) /*-{
    if (typeof $wnd.onSwellRTUpdate === "function")
      $wnd.onSwellRTUpdate(inFlightSize, notAckedSize, unCommitedSize);
  }-*/;

  @Deprecated
  private static native void notifyOnClose(boolean everythingCommitted) /*-{
    if (typeof $wnd.onSwellRTClose === "function")
      $wnd.onSwellRTClose(everythingCommitted);
  }-*/;


  @Override
  public void onModuleLoad() {

    RootPanel.get();
    WaveClient.create(this); // Startup the SwellRT JS Client

    // Force to flow exceptions to the browser
    // WaveClientJS and WaveClient must capture exceptions properly
    GWT.setUncaughtExceptionHandler(null);

    // Capture and notify Network events. They are sent from
    // WaveWebSocketClient. We use the original Wave client event system.
    ClientEvents.get().addNetworkStatusEventHandler(new NetworkStatusEventHandler() {

      @Override
      public void onNetworkStatus(NetworkStatusEvent event) {

        // Don't fire events if user is not logged in
        if (listener == null || loggedInUser == null) return;

        switch (event.getStatus()) {
          case CONNECTED:
            listener.onNetworkConnected();
            break;
          case RECONNECTED:
            listener.onNetworkConnected();
            break;
          case DISCONNECTED:
            listener.onNetworkDisconnected(null);
            break;
          case RECONNECTING:
            listener.onNetworkDisconnected(null);
            break;
          case PROTOCOL_ERROR:

            if (event.getPayload() != null) {

              String message = event.getPayload().toString();

              Object payload = event.getPayload();

              if (payload instanceof RuntimeException) {
                RuntimeException rtException = (RuntimeException) payload;
                Throwable throwable = rtException.getCause();
                if (throwable instanceof CorruptionDetail) {
                  CorruptionDetail corruptionDetail = (CorruptionDetail) throwable;
                  message = corruptionDetail.getErrorCode().name();

                  // if (corruptionDetail.getCause() != null)
                  // message += ", " + corruptionDetail.getCause().getMessage();
                }
              }

              listener.onException(message);

            } else {
              listener.onException("PROTOCOL_EXCEPTION");
            }

            break;
          default:
            break;
        }

      }

    });

    // Notify the host page that client is already loaded
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        if (listener != null) listener.onReady();
      }
    });
  }


  @Override
  public void onUpdate(UnsavedDataInfo unsavedDataInfo) {
    notifyOnUpdate(unsavedDataInfo.inFlightSize(), unsavedDataInfo.estimateUnacknowledgedSize(),
        unsavedDataInfo.estimateUncommittedSize());
    if (listener != null) listener.onDataStatusChanged(unsavedDataInfo);

  }

  @Override
  public void onClose(boolean everythingCommitted) {
    notifyOnClose(everythingCommitted);
    if (listener != null) listener.onNetworkClosed(everythingCommitted);
  }


  public void query(String expr, String projExpr, String aggrExpr,
      final Callback<String, String> callback) throws RequestException, SessionNotStartedException {

    if (!isSessionStarted()) {
      throw new SessionNotStartedException();
    }

    String query = "";

    // query or query + projection case
    if (expr != null) {
      query += "q=" + URL.encodeQueryString(expr);
      if (projExpr != null) query += "&p=" + URL.encodeQueryString(projExpr);
    }

    // aggregate case:

    if (aggrExpr != null){
      query += "&a=" + URL.encodeQueryString(aggrExpr);
    }


    String url = baseServerUrl + "/swell/model";
    url = BrowserSession.addSessionToUrl(url);
    url += "?" + query;
    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.GET, url);

    builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
    builder.sendRequest(query, new RequestCallback() {

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onFailure(exception.getMessage());
      }

      @Override
      public void onResponseReceived(Request request, Response response) {

        if (response.getStatusCode() == 200) {
          callback.onSuccess(response.getText());
        } else {
          callback.onFailure("SERVICE_EXCEPTION");
        }
      }

    });


  }

  public void notificationRequest(String method, String param,
      final Callback<String, String> callback) throws RequestException, SessionNotStartedException {
    if (!isSessionStarted()) {
      throw new SessionNotStartedException();
    }

    String query;
    query = method + "=" + URL.encode(param);

    String url = baseServerUrl + "/swell/notification";
    url = BrowserSession.addSessionToUrl(url);
    url += "?" + query;
    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);

    builder.sendRequest(query, new RequestCallback() {

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onFailure(exception.getMessage());
      }

      @Override
      public void onResponseReceived(Request request, Response response) {
        int status = response.getStatusCode();
        System.out.println("Response" + response.toString());
        if (200 <= status && status < 300) {
          String responseText = response.getText();
          if (responseText == null) {
            responseText = "";
          }
          callback.onSuccess(responseText);
        } else {
          callback.onFailure("SERVICE_EXCEPTION");
        }
      }

    });

  }

  public void setUserEmail(String email, final Callback<String, String> callback)
      throws SessionNotStartedException, RequestException {

    if (!isSessionStarted()) {
      throw new SessionNotStartedException();
    }

    String query = "method=set&email=" + URL.encode(email);

    String url = baseServerUrl + "/swell/email";
    url = BrowserSession.addSessionToUrl(url);
    url += "?" + query;

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);

    builder.sendRequest(query, new RequestCallback() {

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onFailure(exception.getMessage());
      }

      @Override
      public void onResponseReceived(Request request, Response response) {

        int status = response.getStatusCode();

        if (200 <= status && status < 300) {
          String responseText = response.getText();
          if (responseText == null) {
            responseText = "";
          }
          callback.onSuccess(responseText);
        } else {
          callback.onFailure("SERVICE_EXCEPTION");
        }
      }

    });
  }

  public void setPassword(String id, String token, String newPassword,
      final Callback<String, String> callback) throws RequestException {


    String query = "id=" + URL.encode(id) + "&token-or-password="
        + URL.encode(token)
        + "&new-password=" + URL.encode(newPassword);

    String url = baseServerUrl + "/swell/password";
    url = BrowserSession.addSessionToUrl(url);
    url += "?" + query;


    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);

    builder.sendRequest(query, new RequestCallback() {

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onFailure(exception.getMessage());
      }

      @Override
      public void onResponseReceived(Request request, Response response) {

        int status = response.getStatusCode();

        if (200 <= status && status < 300) {
          String responseText = response.getText();
          if (responseText == null) {
            responseText = "";
          }
          callback.onSuccess(responseText);
        } else if (response.getStatusCode() == 403) {
          log.log(Level.SEVERE, "Error requesting write access: " + response.getStatusText());
          callback.onFailure("ACCESS_FORBIDDEN_EXCEPTION");

        } else {
          callback.onFailure("SERVICE_EXCEPTION");
        }
      }

    });

  }

  public void recoverPassword(String idOrEmail, String recoverUrl,
      final Callback<String, String> callback) throws RequestException {

    String query =
        "method=password-reset" + "&id-or-email=" + URL.encodeQueryString(idOrEmail)
            + "&recover-url=" + URL.encodeQueryString(recoverUrl);

    String url = baseServerUrl + "/swell/email";
    url = BrowserSession.addSessionToUrl(url);
    url += "?" + query;


    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);

    builder.sendRequest(query, new RequestCallback() {

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onFailure(exception.getMessage());
      }

      @Override
      public void onResponseReceived(Request request, Response response) {

        int status = response.getStatusCode();

        if (200 <= status && status < 300) {
          String responseText = response.getText();
          if (responseText == null) {
            responseText = "";
          }
          callback.onSuccess(responseText);
        } else {
          callback.onFailure("SERVICE_EXCEPTION " + response.getText());
        }
      }

    });

  }



  public void createUser(JavaScriptObject parameters, ServiceCallback _callback)
      throws RequestException {

    if (_callback == null)
      _callback = ServiceCallback.getVoidCallback();

    final ServiceCallback callback = _callback;

    String url = baseServerUrl + "/swell/account";
    url = BrowserSession.addSessionToUrl(url);

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);
    builder.setHeader("Content-Type", "text/plain; charset=utf-8");
    builder.sendRequest(ServiceParameters.toJSON(parameters), new RequestCallback() {

      @Override
      public void onResponseReceived(Request request, Response response) {

        if (response.getStatusCode() != 200)
          callback.onComplete(ServiceCallback.JavaScriptResponse.error(response.getText()));
        else
          callback.onComplete(ServiceCallback.JavaScriptResponse.success(response.getText()));


      }

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onComplete(ServiceCallback.JavaScriptResponse.error("SERVICE_EXCEPTION",
            exception.getMessage()));
      }
    });


  }

  public void updateUserProfile(JavaScriptObject parameters, ServiceCallback _callback)
      throws RequestException {

    if (_callback == null)
      _callback = ServiceCallback.getVoidCallback();

    final ServiceCallback callback = _callback;

    String url = baseServerUrl + "/swell/account/" + loggedInUser.getName();
    url = BrowserSession.addSessionToUrl(url);

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);
    builder.setHeader("Content-Type", "text/plain; charset=utf-8");
    builder.sendRequest(ServiceParameters.toJSON(parameters), new RequestCallback() {

      @Override
      public void onResponseReceived(Request request, Response response) {

        if (response.getStatusCode() != 200)
          callback.onComplete(ServiceCallback.JavaScriptResponse.error(response.getText()));
        else
          callback.onComplete(ServiceCallback.JavaScriptResponse.success(response.getText()));


      }

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onComplete(ServiceCallback.JavaScriptResponse.error("SERVICE_EXCEPTION",
            exception.getMessage()));
      }
    });

  }


  public void getUserProfile(ServiceCallback _callback)
      throws RequestException {

    if (_callback == null)
      _callback = ServiceCallback.getVoidCallback();

    final ServiceCallback callback = _callback;

    String url = baseServerUrl + "/swell/account/" + loggedInUser.getName();
    url = BrowserSession.addSessionToUrl(url);

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.GET, url);
    builder.sendRequest(null, new RequestCallback() {

      @Override
      public void onResponseReceived(Request request, Response response) {

        if (response.getStatusCode() != 200)
          callback.onComplete(ServiceCallback.JavaScriptResponse.error(response.getText()));
        else
          callback.onComplete(ServiceCallback.JavaScriptResponse.success(response.getText()));


      }

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onComplete(ServiceCallback.JavaScriptResponse.error("SERVICE_EXCEPTION",
            exception.getMessage()));
      }
    });
  }

  public void getUserProfile(JsArrayString participants, ServiceCallback _callback)
      throws RequestException {

    if (_callback == null)
      _callback = ServiceCallback.getVoidCallback();

    final ServiceCallback callback = _callback;

    String url = baseServerUrl + "/swell/account/";
    url = BrowserSession.addSessionToUrl(url);

    String query = "";
    for (int i = 0; i < participants.length(); i++) {
      if (!query.isEmpty()) query += ";";

      query += participants.get(i);
    }

    query = "p=" + URL.encode(query);
    url += "?" + query;

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.GET, url);
    builder.sendRequest(null, new RequestCallback() {

      @Override
      public void onResponseReceived(Request request, Response response) {

        if (response.getStatusCode() != 200)
          callback.onComplete(ServiceCallback.JavaScriptResponse.error(response.getText()));
        else
          callback.onComplete(ServiceCallback.JavaScriptResponse.success(response.getText()));


      }

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onComplete(ServiceCallback.JavaScriptResponse.error("SERVICE_EXCEPTION",
            exception.getMessage()));
      }
    });
  }

  public void invite(JsArrayString emails, String inviteUrl,
 String urlText,
      final Callback<String, String> callback)
      throws RequestException {
    String baseUrl = baseServerUrl + "/swell/invite/";
    baseUrl = BrowserSession.addSessionToUrl(baseUrl);

    for (int i = 0; i < emails.length(); i++) {

      String email = emails.get(i);

      String query = "id-or-email=" + URL.encodeQueryString(email);
      query += "&url=" + URL.encodeQueryString(inviteUrl);
      query += "&url_text=" + URL.encodeQueryString(urlText);
      String url = baseUrl + "?" + query;

      RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);

      builder.sendRequest(null, new RequestCallback() {

        @Override
        public void onResponseReceived(Request request, Response response) {

          if (response.getStatusCode() != 200)
            callback.onFailure("SERVICE_EXCEPTION " + response.getText());
          else
            callback.onSuccess(response.getText());
        }

        @Override
        public void onError(Request request, Throwable exception) {
          callback.onFailure("SERVICE_EXCEPTION " + exception.getMessage());

        }

      });


    }
  }

  public void join(String email, String inviteUrl, String urlText, String message, String admin,
    final Callback<String, String> callback)
    throws RequestException {

    String baseUrl = baseServerUrl + "/swell/join/";
    baseUrl = BrowserSession.addSessionToUrl(baseUrl);

    String query = "id-or-email=" + URL.encodeQueryString(email);
    query += "&url=" + URL.encodeQueryString(inviteUrl);
    query += "&url_text=" + URL.encodeQueryString(urlText);
    query += "&message=" + URL.encodeQueryString(message);
    query += "&admin=" + URL.encodeQueryString(admin);
    String url = baseUrl + "?" + query;

    RequestBuilder builder = SwellRTUtils.newRequestBuilder(RequestBuilder.POST, url);

    builder.sendRequest(null, new RequestCallback() {

      @Override
      public void onResponseReceived(Request request, Response response) {

        if (response.getStatusCode() != 200)
          callback.onFailure("SERVICE_EXCEPTION " + response.getText());
        else
          callback.onSuccess(response.getText());
      }

      @Override
      public void onError(Request request, Throwable exception) {
        callback.onFailure("SERVICE_EXCEPTION " + exception.getMessage());

      }

    });


  }

}
