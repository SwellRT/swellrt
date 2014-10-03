package org.waveprotocol.wavejs;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
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

import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.box.webclient.client.ClientIdGenerator;
import org.waveprotocol.box.webclient.client.RemoteViewServiceMultiplexer;
import org.waveprotocol.box.webclient.client.Session;
import org.waveprotocol.box.webclient.client.SimpleWaveStore;
import org.waveprotocol.box.webclient.client.WaveWebSocketClient;
import org.waveprotocol.box.webclient.search.SearchBuilder;
import org.waveprotocol.box.webclient.search.SearchService;
import org.waveprotocol.box.webclient.search.WaveStore;
import org.waveprotocol.wave.client.extended.WaveContentManager;
import org.waveprotocol.wave.client.extended.WaveContentWrapper;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.extended.WaveType;
import org.waveprotocol.wave.model.extended.id.IdGeneratorExtended;
import org.waveprotocol.wave.model.extended.id.IdGeneratorExtendedImpl;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.schema.SchemaProvider;
import org.waveprotocol.wave.model.schema.conversation.ConversationSchemas;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WaveJS implements EntryPoint {

  private static final Logger log = Logger.getLogger(WaveJS.class.getName());


  public class UnsavedDataListenerProxy implements UnsavedDataListener {

    private UnsavedDataListener actualListener = null;

    public void setActualUnsavedDataListener(UnsavedDataListener actualListener) {
      this.actualListener = actualListener;
    }

    @Override
    public void onUpdate(UnsavedDataInfo unsavedDataInfo) {
      if (actualListener != null)
        this.actualListener.onUpdate(unsavedDataInfo);

    }

    @Override
    public void onClose(boolean everythingCommitted) {
      if (actualListener != null)
        this.actualListener.onClose(everythingCommitted);
    }

  }


  private final static String SESSION_COOKIE_NAME = "WSESSIONID";

  /* Components depending on the user session */
  private String seed;
  private ParticipantId loggedInUser = null;
  private RemoteViewServiceMultiplexer channel;
  private IdGeneratorExtended idGenerator;

  /* Components shared across sessions */
  private final SchemaProvider schemaProvider;

  private final WaveStore waveStore;


  private String waveServerDomain;
  private String waveServerURLSchema;
  private String waveServerURL;
  private WaveWebSocketClient websocket;
  private SearchBuilder searchBuilder;

  private WaveContentManager waveContentManager = null;
  private Map<String, WaveContentWrapper> activeWaveMap = null;




  /**
   * Default constructor
   */
  public WaveJS() {

    this.activeWaveMap = new HashMap<String, WaveContentWrapper>();
    this.schemaProvider = new ConversationSchemas();
    this.waveStore = new SimpleWaveStore();

    Timing.setEnabled(false);
  }

  /**
   * Performs a login against Wave's /auth servlet. This method doesn't start a
   * web socket session. Wave server will set a session cookie.
   *
   * CORS and XHR is taken into account:
   * http://stackoverflow.com/questions/10977058
   * /xmlhttprequest-and-set-cookie-cookie
   *
   * @param user name of the user without domain part
   * @param callback
   */
  private void login(final String user, final String password,
      final Callback<String, String> callback) {

    final ParticipantId participantId = ParticipantId.ofUnsafe(user + "@" + this.waveServerDomain);

    String query = URL.encodeQueryString("address") + "=" + participantId.getAddress();
    query += "&password=" + URL.encodeQueryString(password);

    // redirect to the profile servlet (json output)
    // query += "&r=" + URL.encodeQueryString("/profile/?adresses=");

    String url =
        "http://" + waveServerURL + "/auth/signin?r="
        + URL.encodeQueryString("/profile/?addresses=" + participantId.getAddress());
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

          log.log(Level.INFO, "Wave login response headers: " + response.getHeadersAsString());
          log.log(Level.INFO, "Wave login response status: " + response.getStatusCode() + ","
              + response.getStatusText());

          // TODO fix Wave auth server to do not make redirects
          // xmlHTTTPResquest object doesn't handle 302 properly
          if (response.getStatusCode() == 200) {

            log.log(Level.INFO, "Wave login succesfull for: " + user);
            log.log(Level.INFO, "Wave user info: " + response.getText());

            loggedInUser = participantId;

            // This fakes the former Wave Client JS session object.
            String sessionId = Cookies.getCookie(SESSION_COOKIE_NAME);
            createWebClientSession(WaveJS.this.waveServerDomain, participantId.getAddress(),
                sessionId);

            callback.onSuccess(sessionId);

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
  private native void createWebClientSession(String localDomain, String userAddress,
      String sessionId) /*-{
                        $wnd.__session = new Object();
                        $wnd.__session['domain'] = localDomain;
                        $wnd.__session['address'] = userAddress;
                        $wnd.__session['id'] = sessionId;
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

    websocket = new WaveWebSocketClient(websocketNotAvailable(), webSocketURL);
    websocket.connect();

    channel = new RemoteViewServiceMultiplexer(websocket, loggedInUser.getAddress());
    idGenerator = new IdGeneratorExtendedImpl(ClientIdGenerator.create());
    seed = Session.get().getIdSeed();

    waveContentManager =
        WaveContentManager.create(waveStore, waveServerDomain, idGenerator,
        loggedInUser, seed,
        channel);

  }

  public void stopComms() {
    waveContentManager = null;
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
                                                 return !window.WebSocket
                                                 }-*/;

  private native void setWebsocketAddress(String address) /*-{
                                                          $wnd.__websocket_address = address;
                                                          }-*/;


  /*******************************************************************************************/




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
      final Callback<String, String> callback) {

    // TODO validate url, if it fails return false

    waveServerURLSchema = url.startsWith("http://") ? "http://" : "https://";
    // TODO extract domain from URL
    waveServerDomain = "local.net";
    waveServerURL = url.replace(waveServerURLSchema, "");

    login(user, password, new Callback<String, String>() {

      @Override
      public void onFailure(String reason) {
        callback.onFailure(reason);
      }

      @Override
      public void onSuccess(String result) {

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
    for (Entry<String, WaveContentWrapper> entry : activeWaveMap.entrySet())
      entry.getValue().destroy();

    activeWaveMap.clear();

    // Disconnect from Wave's websocket
    stopComms();

    // Clear user session
    Cookies.removeCookie(SESSION_COOKIE_NAME);
    loggedInUser = null;
    searchBuilder = null;


    return true;

  }



  public String createWave(String type, final Callback<WaveContentWrapper, String> callback) {

    WaveType waveType = WaveType.valueOf(type);
    if (WaveType.UNKNOWN == waveType) return null;

    final WaveId waveId = idGenerator.newWaveId(waveType);
    final WaveContentWrapper waveWrapper =
        waveContentManager.getWaveContentWrapper(WaveRef.of(waveId), true);

    activeWaveMap.put(waveId.toString(), waveWrapper);


    waveWrapper.load(new Command() {
      @Override
      public void execute() {

        callback.onSuccess(waveWrapper);

      }
    });

    return waveId.toString();


  }


  /**
   * Open an existing wave.
   *
   * @param wave WaveId
   * @param callback
   * @return null if wave is not a valid WaveId. The WaveId otherwise.
   */
  public String openWave(final String wave, final Callback<WaveContentWrapper, String> callback) {

    // if (activeWaveMap.containsKey(wave)) return wave;

    WaveId waveId = null;
    try {

      waveId = WaveId.deserialise(wave);

    } catch (Exception e) {
      callback.onFailure(e.getMessage());
      return null;
    }

    final WaveContentWrapper waveWrapper =
        waveContentManager.getWaveContentWrapper(WaveRef.of(waveId), false);

    activeWaveMap.put(wave, waveWrapper);

    waveWrapper.load(new Command() {
      @Override
      public void execute() {

        callback.onSuccess(waveWrapper);

      }
    });

    return wave;

  }




  public boolean closeWave(String wave) {

    WaveContentWrapper waveWrapper = activeWaveMap.get(wave);

    if (waveWrapper == null) return false;

    waveWrapper.destroy();
    activeWaveMap.remove(wave);

    return true;

  }



  private static native void notifyLoaded() /*-{
    $wnd.onWaveJSReady();
  }-*/;

  public void onModuleLoad() {

    RootPanel.get();
    WaveClient.create(this); // Startup the WaveJS Client

    // Notify the host page that client is already loaded
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        notifyLoaded();
      }
    });


  }

}
