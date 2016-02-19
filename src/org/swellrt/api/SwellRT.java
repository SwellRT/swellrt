package org.swellrt.api;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.ui.RootPanel;

import org.swellrt.client.WaveWrapper;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TypeIdGenerator;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.box.webclient.client.ClientIdGenerator;
import org.waveprotocol.box.webclient.client.RemoteViewServiceMultiplexer;
import org.waveprotocol.box.webclient.client.WaveSocket.WaveSocketStartCallback;
import org.waveprotocol.box.webclient.client.WaveWebSocketClient;
import org.waveprotocol.box.webclient.search.SearchBuilder;
import org.waveprotocol.box.webclient.search.SearchService;
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
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
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


  private final static String SESSION_COOKIE_NAME = "WSESSIONID";
  private final static String SESSION_PATH_PARAM = "sid";
  private final static String REGISTER_CTX = "auth/register";
  private static String CHARSET = "utf-8";

  /* Components depending on the user session */
  private ParticipantId loggedInUser = null;
  private String sessionId = null;
  private RemoteViewServiceMultiplexer channel;


  /* Components shared across sessions */
  private final SchemaProvider schemaProvider;


  private String waveDomain;
  private String waveServerURLSchema;
  private String waveServerURL;
  private WaveWebSocketClient websocket;
  @Deprecated
  private SearchBuilder searchBuilder;

  /** List of living waves for the active session. */
  private Map<WaveId, WaveWrapper> waveWrappers = CollectionUtils.newHashMap();;

  /** A listener to global data/network/runtime events */
  private SwellRT.Listener listener = null;

  private boolean useWebSocket = true;


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
  private void login(final String user, final String password,
      final Callback<JavaScriptObject, String> callback) throws RequestException {


    String query = URL.encodeQueryString("address") + "=" + user;
    query += "&password=" + URL.encodeQueryString(password);

    // redirect to the profile servlet (json output)
    // query += "&r=" + URL.encodeQueryString("/profile/?adresses=");

    String url = waveServerURLSchema + waveServerURL + "/auth/signin?r=none";
    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);
    builder.setIncludeCredentials(true);
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
          // Use the browser __session object instead of setting cookie
          JavaScriptObject sessionWeb =
              createWebClientSession(loggedInUser.getDomain(), loggedInUser.getAddress(), seed,
                  sessionId);

          // Init Id generator
          TypeIdGenerator.get().initialize(ClientIdGenerator.create());

          callback.onSuccess(sessionWeb);

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
   * Set up Wave's webclient old session object (Session).
   *
   * @param localDomain
   * @param userAddress
   * @param sessionId
   */
  private native JavaScriptObject createWebClientSession(String localDomain, String userAddress,
      String seed, String sessionId) /*-{
    $wnd.__session = new Object();
    $wnd.__session['domain'] = localDomain;
    $wnd.__session['address'] = userAddress;
    $wnd.__session['id'] = seed; // 'id' is used in Session.java/ClientIdGenerator to get the seed
    $wnd.__session['sessionid'] = sessionId; //
    return $wnd.__session;
  }-*/;


  private native void destroyWebClientSession() /*-{
     $wnd.__session = null;
  }-*/;


  /**
   * Initialize the infrastructure of communications with the wave server for
   * the logged in user. It seems we need a new Web socket client instance for
   * each user session. Not sure how to close a websocket.
   */
  private void startComms(final Callback<Void, Void> callback) {

    assert (loggedInUser != null);

    log.log(Level.INFO, "Starting wave session...");

    // this is needed to atmosphere to work
    setWebsocketAddress(this.waveServerURL);

    String webSocketURL = waveServerURLSchema.equals("http://") ? "ws://" : "wss://";
    webSocketURL += waveServerURL + "/";

    websocket = new WaveWebSocketClient(websocketNotAvailable() || !useWebSocket, webSocketURL);
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

  public void stopComms() {
    websocket.disconnect(true);
    channel = null;
  }

  /**
   * Retrieves a list of wave digests for the user logged in the wave server.
   *
   * @param userName
   * @param startIndex
   * @param numResults
   * @param callback
   */
  @Deprecated
  public void getWaves(int startIndex, int numResults,
      SearchService.Callback callback) {

    searchBuilder.newSearch().setQuery("").setIndex(startIndex).setNumResults(numResults)
        .search(callback);

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

  /**
   * Add an extra path token with the session id in case of session cookie is
   * not available. The format is specific for the Jetty server.
   *
   * @param url The url where to add the session id
   * @return
   */
  private String addSessionToUrl(String url) {
    if (Cookies.getCookie(SESSION_COOKIE_NAME) == null && sessionId != null) {
      url += ";" + SESSION_PATH_PARAM + "=" + sessionId;
    }
    return url;
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

    String url = host.endsWith("/") ? host + REGISTER_CTX : host + "/" + REGISTER_CTX;
    url = addSessionToUrl(url);
    String queryStr = "address=" + URL.encode(username) + "&password=" + URL.encode(password);

    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);


      // Allow cookie headers, and so Wave session can be set
      builder.setIncludeCredentials(true);

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
  public void startSession(String user, String password, final String url,
      final Callback<JavaScriptObject, String> callback) throws RequestException {


    waveServerURLSchema = url.startsWith("http://") ? "http://" : "https://";
    waveServerURL = url.replace(waveServerURLSchema, "");


    login(user, password, new Callback<JavaScriptObject, String>() {

      @Override
      public void onFailure(String reason) {
        callback.onFailure(reason);
      }

      @Override
      public void onSuccess(final JavaScriptObject result) {

        searchBuilder =
            CustomJsoSearchBuilderImpl.create(waveServerURLSchema + waveServerURLSchema);

        startComms(new Callback<Void, Void>() {

          @Override
          public void onFailure(Void none) {
            callback.onFailure("NETWORK_EXCEPTION");

            // Clear user session
            Cookies.removeCookie(SESSION_COOKIE_NAME);
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
   * Logout user and close communications with Wave provider
   *
   * @return
   * @throws SessionNotStartedException
   */
  public void stopSession() throws SessionNotStartedException {

    if (loggedInUser == null) throw new SessionNotStartedException();

    // Destroy all waves
    for (Entry<WaveId, WaveWrapper> entry : waveWrappers.entrySet())
      entry.getValue().destroy();

    waveWrappers.clear();

    // Disconnect from Wave's websocket
    stopComms();

    // Clear user session
    Cookies.removeCookie(SESSION_COOKIE_NAME);
    sessionId = null;
    loggedInUser = null;
    destroyWebClientSession();
    searchBuilder = null;

  }

  protected WaveWrapper getWaveWrapper(WaveId waveId, boolean isNew) {


    if (isNew) {
      Preconditions.checkArgument(!waveWrappers.containsKey(waveId),
          "Trying to create an existing Wave");
      WaveWrapper ww =
          new WaveWrapper(WaveRef.of(waveId), channel, TypeIdGenerator.get()
              .getUnderlyingGenerator(), waveDomain,
              Collections.<ParticipantId> emptySet(), loggedInUser, isNew, this);
      waveWrappers.put(waveId, ww);
    } else {
      if (!waveWrappers.containsKey(waveId) || waveWrappers.get(waveId).isClosed()) {
        WaveWrapper ww =
            new WaveWrapper(WaveRef.of(waveId), channel, TypeIdGenerator.get()
                .getUnderlyingGenerator(), waveDomain,
                Collections.<ParticipantId> emptySet(), loggedInUser, isNew, this);
        waveWrappers.put(waveId, ww);
      }
    }

    return waveWrappers.get(waveId);
  }


  public String createWave(final OnLoadCallback<WaveWrapper> callback) throws NetworkException,
      SessionNotStartedException {

    if (!isSessionStarted()) {
      throw new SessionNotStartedException();
    }

    if (!websocket.isConnected()) {
      throw new NetworkException();
    }



    final WaveId waveId = TypeIdGenerator.get().newWaveId();
    final WaveWrapper waveWrapper = getWaveWrapper(waveId, true);

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
  public String openWave(final String strWaveId, final OnLoadCallback<WaveWrapper> callback)
      throws InvalidIdException, NetworkException, SessionNotStartedException, RequestException {

    final WaveId waveId = WaveId.deserialise(strWaveId);

    if (!isSessionStarted()) {
      throw new SessionNotStartedException();
    }

    if (!websocket.isConnected()) {
      throw new NetworkException();
    }


    final WaveWrapper waveWrapper = getWaveWrapper(waveId, false);

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

    WaveWrapper waveWrapper = waveWrappers.get(waveId);

    if (waveWrapper == null) throw new InvalidIdException();

    waveWrapper.destroy();
    waveWrappers.remove(waveId);

  }

  protected WaveDocuments<? extends InteractiveDocument> getDocumentRegistry(Model model) {
    Preconditions.checkArgument(model != null,
        "Unable to get document registry from a null data model");
    Preconditions.checkArgument(waveWrappers.containsKey(model.getWaveId()),
        "Wave wrapper is not avaiable for the model");
    return waveWrappers.get(model.getWaveId()).getDocumentRegistry();
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

        if (listener == null) return;

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


    String url = waveServerURLSchema + waveServerURL + "/swell/model";
    url = addSessionToUrl(url);
    url += "?" + query;
    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);

    // Allow cookie headers, and so Wave session can be set
    builder.setIncludeCredentials(true);

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

    String url = waveServerURLSchema + waveServerURL + "/swell/notification";
    url = addSessionToUrl(url);
    url += "?" + query;
    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);

    // Allow cookie headers, and so Wave session can be set
    builder.setIncludeCredentials(true);

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

    String url = waveServerURLSchema + waveServerURL + "/swell/email?" + query;

    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);

    // Allow cookie headers
    builder.setIncludeCredentials(true);

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

    String url = waveServerURLSchema + waveServerURL + "/swell/password?" + query;

    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);

    // Allow cookie headers
    builder.setIncludeCredentials(true);

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

    String url = waveServerURLSchema + waveServerURL + "/swell/email?" + query;

    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);

    // Allow cookie headers
    builder.setIncludeCredentials(true);

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



  public void createUser(String serverUrl, JavaScriptObject parameters, final ServiceCallback callback) throws RequestException {

    String url = serverUrl + "/swell/account";

    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);
    builder.setIncludeCredentials(true);
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

  public void updateUserProfile(JavaScriptObject parameters, final ServiceCallback callback)
      throws RequestException {

    String url = waveServerURLSchema + waveServerURL + "/swell/account/" + loggedInUser.getName();

    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);
    builder.setIncludeCredentials(true);
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


  public void getUserProfile(final ServiceCallback callback)
      throws RequestException {

    String url = waveServerURLSchema + waveServerURL + "/swell/account/" + loggedInUser.getName();

    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);
    builder.setIncludeCredentials(true);
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


}
