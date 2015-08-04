package org.swellrt.api;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
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
import org.swellrt.model.IdGeneratorGeneric;
import org.swellrt.model.generic.Model;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.box.webclient.client.ClientIdGenerator;
import org.waveprotocol.box.webclient.client.RemoteViewServiceMultiplexer;
import org.waveprotocol.box.webclient.client.Session;
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
import org.waveprotocol.wave.model.id.IdGenerator;
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

  private final static String SESSION_COOKIE_NAME = "WSESSIONID";
  private final static String REGISTER_CTX = "auth/register";
  private static String CHARSET = "utf-8";

  /* Components depending on the user session */
  private String seed;
  private ParticipantId loggedInUser = null;
  private RemoteViewServiceMultiplexer channel;
  private IdGenerator idGenerator;



  /* Components shared across sessions */
  private final SchemaProvider schemaProvider;


  private String waveDomain;
  private String waveServerURLSchema;
  private String waveServerURL;
  private WaveWebSocketClient websocket;
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
   */
  private void login(final String user, final String password,
      final Callback<JavaScriptObject, String> callback) {


    String query = URL.encodeQueryString("address") + "=" + user;
    query += "&password=" + URL.encodeQueryString(password);

    // redirect to the profile servlet (json output)
    // query += "&r=" + URL.encodeQueryString("/profile/?adresses=");

    String url = waveServerURLSchema + waveServerURL + "/auth/signin?r=none";
    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, url);

    try {
      // Allow cookie headers, and so Wave session can be set
      builder.setIncludeCredentials(true);

      builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
      builder.sendRequest(query, new RequestCallback() {

        public void onError(Request request, Throwable exception) {
          callback.onFailure(exception.getMessage());
        }

        @Override
        public void onResponseReceived(Request request, Response response) {

          // xmlHTTTPResquest object doesn't handle 302 properly
          if (response.getStatusCode() == 200) {

            // Get complete user address from the server
            loggedInUser = ParticipantId.ofUnsafe(response.getText());

            // This fakes the former Wave Client JS session object.
            String sessionId = Cookies.getCookie(SESSION_COOKIE_NAME);
            // oh yes, Session doesn't work. Wiab implementation does the same.
            String seed = SwellRTUtils.nextBase64(10);
            waveDomain = loggedInUser.getDomain();
            callback.onSuccess(createWebClientSession(loggedInUser.getDomain(),
                loggedInUser.getAddress(), seed));

          } else {

            loggedInUser = null;

            log.log(Level.SEVERE, "Error calling wave login servlet: " + response.getStatusText());
            callback.onFailure("Error calling wave login servlet: " + response.getStatusText());
          }

        }

      });

    } catch (RequestException e) {
      log.log(Level.SEVERE, "Error calling wave login servlet", e);
      callback.onFailure(e.getMessage());
    }

  }

  /**
   * Set up Wave's webclient old session object (Session).
   *
   * @param localDomain
   * @param userAddress
   * @param sessionId
   */
  private native JavaScriptObject createWebClientSession(String localDomain, String userAddress,
      String sessionId) /*-{
    $wnd.__session = new Object();
    $wnd.__session['domain'] = localDomain;
    $wnd.__session['address'] = userAddress;
    $wnd.__session['id'] = sessionId;
    return $wnd.__session;
  }-*/;


  /**
   * Initialize the infrastructure of communications with the wave server for
   * the logged in user. It seems we need a new Web socket client instance for
   * each user session. Not sure how to close a websocket.
   */
  private void startComms() {

    assert (loggedInUser != null);

    log.log(Level.INFO, "Starting wave session...");

    // this is needed to atmosphere to work
    setWebsocketAddress(this.waveServerURL);

    String webSocketURL = waveServerURLSchema.equals("http://") ? "ws://" : "wss://";
    webSocketURL += waveServerURL + "/";

    websocket = new WaveWebSocketClient(websocketNotAvailable() || !useWebSocket, webSocketURL);
    websocket.connect();

    channel = new RemoteViewServiceMultiplexer(websocket, loggedInUser.getAddress());
    idGenerator = ClientIdGenerator.create();
    seed = Session.get().getIdSeed();


  }

  public void stopComms() {
    websocket.disconnect(true);
    channel = null;
    seed = null;
  }

  /**
   * Retrieves a list of wave digests for the user logged in the wave server.
   *
   * @param userName
   * @param startIndex
   * @param numResults
   * @param callback
   */
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


  /*******************************************************************************************/


  public void useWebSocket(boolean enabled) {
    useWebSocket = enabled;
  }

  /**
   * Create a new Wave user.
   *
   * @param host The server hosting the user: http(s)://server.com
   * @param username user address including domain part: username@server.com
   * @param password the user password
   * @param callback
   */
  public void registerUser(final String host, final String username, final String password,
      final Callback<String, String> callback) {


    String urlStr = host.endsWith("/") ? host + REGISTER_CTX : host + "/" + REGISTER_CTX;

    String queryStr = "address=" + URL.encode(username) + "&password=" + URL.encode(password);

    RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, urlStr);

    try {
      // Allow cookie headers, and so Wave session can be set
      builder.setIncludeCredentials(true);

      builder.setHeader("Accept-Charset", CHARSET);
      builder.setHeader("Content-Type", "application/x-www-form-urlencoded;charset=" + CHARSET);

      builder.sendRequest(queryStr, new RequestCallback() {

        public void onError(Request request, Throwable exception) {
          callback.onFailure(exception.getMessage());
        }

        @Override
        public void onResponseReceived(Request request, Response response) {

          // xmlHTTTPResquest object doesn't handle 302 properly
          if (response.getStatusCode() == 200) {
            callback.onSuccess(response.getStatusText());
          } else {
            log.log(Level.WARNING,
                "Error registering new user " + username + ": " + response.getStatusText());
            callback.onFailure(response.getStatusText());
          }

        }

      });

    } catch (RequestException e) {
      log.log(Level.WARNING, "Error registering new user " + username, e);
      callback.onFailure(e.getMessage());
    }


  }

  /**
   * Login the user in and start communications with Wave provider
   *
   * @param url to the Wave server, https://local.net:9898
   * @param user
   * @param password
   * @param callback
   * @return
   */
  public boolean startSession(String user, String password, final String url,
      final Callback<JavaScriptObject, String> callback) {

    // TODO validate url, if it fails return false

    waveServerURLSchema = url.startsWith("http://") ? "http://" : "https://";
    waveServerURL = url.replace(waveServerURLSchema, "");

    login(user, password, new Callback<JavaScriptObject, String>() {

      @Override
      public void onFailure(String reason) {
        callback.onFailure(reason);
      }

      @Override
      public void onSuccess(JavaScriptObject result) {

        searchBuilder =
            CustomJsoSearchBuilderImpl.create(waveServerURLSchema + waveServerURLSchema);
        startComms();
        callback.onSuccess(result);

      }
    });

    return true;

  }




  /**
   * Logout user and close communications with Wave provider
   *
   * @return
   */
  public boolean stopSession() {

    // Destroy all waves
    for (Entry<WaveId, WaveWrapper> entry : waveWrappers.entrySet())
      entry.getValue().destroy();

    waveWrappers.clear();

    // Disconnect from Wave's websocket
    stopComms();

    // Clear user session
    Cookies.removeCookie(SESSION_COOKIE_NAME);
    loggedInUser = null;
    searchBuilder = null;

    return true;
  }

  protected WaveWrapper getWaveWrapper(WaveId waveId, boolean isNew) {


    if (isNew) {
      Preconditions.checkArgument(!waveWrappers.containsKey(waveId),
          "Trying to create an existing Wave");
      WaveWrapper ww =
          new WaveWrapper(WaveRef.of(waveId), channel, idGenerator, waveDomain,
              Collections.EMPTY_SET, loggedInUser, isNew, this);
      waveWrappers.put(waveId, ww);
    } else {
      if (!waveWrappers.containsKey(waveId) || waveWrappers.get(waveId).isClosed()) {
        WaveWrapper ww =
            new WaveWrapper(WaveRef.of(waveId), channel, idGenerator, waveDomain,
                Collections.EMPTY_SET, loggedInUser, isNew, this);
        waveWrappers.put(waveId, ww);
      }
    }

    return waveWrappers.get(waveId);
  }


  public String createWave(IdGeneratorGeneric idGenerator,
      final Callback<WaveWrapper, String> callback) {

    idGenerator.initialize(this.idGenerator); // TODO what???

    final WaveId waveId = idGenerator.newWaveId();
    final WaveWrapper waveWrapper = getWaveWrapper(waveId, true);

    if (waveWrapper.isLoaded()) {
      callback.onSuccess(waveWrapper);
    } else {
      waveWrapper.load(new Command() {
        @Override
        public void execute() {
          callback.onSuccess(waveWrapper);
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
   */
  public String openWave(final String strWaveId, final Callback<WaveWrapper, String> callback) {

    WaveId waveId = null;
    try {

      waveId = WaveId.deserialise(strWaveId);

    } catch (Exception e) {
      callback.onFailure(e.getMessage());
      return null;
    }

    final WaveWrapper waveWrapper = getWaveWrapper(waveId, false);

    if (waveWrapper.isLoaded()) {
      callback.onSuccess(waveWrapper);
    } else {
      waveWrapper.load(new Command() {
        @Override
        public void execute() {
          callback.onSuccess(waveWrapper);
        }
      });

    }
    return strWaveId;
  }




  public boolean closeWave(String waveIdStr) {

    WaveId waveId = WaveId.deserialise(waveIdStr);

    WaveWrapper waveWrapper = waveWrappers.get(waveId);

    if (waveWrapper == null) return false;

    waveWrapper.destroy();
    waveWrappers.remove(waveId);

    return true;

  }

  protected WaveDocuments<? extends InteractiveDocument> getDocumentRegistry(Model model) {
    Preconditions.checkArgument(waveWrappers.containsKey(model.getWaveId()),
        "Wave wrapper is not aviable for the model");
    WaveWrapper ww = waveWrappers.get(model.getWaveId());
    return ww.getDocumentRegistry();
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


  public void onModuleLoad() {

    RootPanel.get();
    WaveClient.create(this); // Startup the SwellRT JS Client


    GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {

      @Override
      public void onUncaughtException(Throwable e) {
        log.log(Level.SEVERE, "Uncaught Exception: " + e.getMessage());
        GWT.log(e.getMessage(), e);
        if (listener != null) listener.onException("UNKNOWN_EXCEPTION");
      }
    });

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


  public void query(String expr, final Callback<String, String> callback) {

    String query = "q=" + URL.encodeQueryString(expr);
    String url = waveServerURLSchema + waveServerURL + "/swell/model?" + query;

    RequestBuilder builder = new RequestBuilder(RequestBuilder.GET, url);

    try {
      // Allow cookie headers, and so Wave session can be set
      builder.setIncludeCredentials(true);

      builder.setHeader("Content-Type", "application/x-www-form-urlencoded");
      builder.sendRequest(query, new RequestCallback() {

        public void onError(Request request, Throwable exception) {
          callback.onFailure(exception.getMessage());
        }

        @Override
        public void onResponseReceived(Request request, Response response) {

          if (response.getStatusCode() == 200) {
            callback.onSuccess(response.getText());
          } else {
            callback.onFailure(response.getStatusText());
          }
        }

      });

    } catch (RequestException e) {
      callback.onFailure(e.getMessage());
    }

  }

}
