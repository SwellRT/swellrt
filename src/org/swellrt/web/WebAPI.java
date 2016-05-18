package org.swellrt.web;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Command;

import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TypeIdGenerator;
import org.swellrt.model.js.ProxyAdapter;
import org.swellrt.web.wave.RemoteViewServiceMultiplexer;
import org.swellrt.web.wave.WaveLoader;
import org.swellrt.web.wave.WaveSocket.WaveSocketStartCallback;
import org.swellrt.web.wave.WaveWebSocketClient;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.IdGeneratorImpl.Seed;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The JavaScript Web API for SwellRT.
 *
 * This class presents some setupXXX() methods for adding different features to
 * the API's js object.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public final class WebAPI extends JavaScriptObject implements WaveWebSocketClient.StatusListener {


  protected WebAPI() {

  }

  protected static String getWebsocketAddress(String serverUrl) {
    String websocketUrl = !serverUrl.endsWith("/") ? serverUrl + "/" : serverUrl;

    if (websocketUrl.startsWith("http://"))
      websocketUrl = websocketUrl.replace("http://", "ws://");

    else if (websocketUrl.startsWith("https://"))
      websocketUrl = websocketUrl.replace("https://", "wss://");

    return websocketUrl;
  }

  public static WebAPI create(String server) {

    WebAPI wapi = (WebAPI) JavaScriptObject.createObject();
    wapi.setupWebsocket(getWebsocketAddress(server));
    wapi.setupHttp(server, WebAPIConstants.REST_SERVICE_CONTEXT);
    wapi.setupLoginMethods();
    wapi.setupDataMethods();
    wapi.setupWaveDependencies(new HashMap<WaveId, WaveLoader>());
    return wapi;

  }

  /**
   * Get updates from the websocket connection status.
   */
  @Override
  public void onStateChange(String state, String stateInfo) {
    this.setWebsocketState(state, stateInfo);

    // if state == ERROR clean all open waves because
    // we don't have hot reconnection
  }


  protected final native String getWebsocketState() /*-{
    return this._ws.status;
  }-*/;

  protected final native void setWebsocketState(String state, String stateInfo) /*-{
    this._ws.status = state;
    this._ws.statusInfo = stateInfo;
  }-*/;


  protected final native String getWebsocketServer() /*-{
    return this._ws.server;
  }-*/;

  protected final native String getUserId() /*-{
    return this._session.id;
  }-*/;

  protected final native String getDomain() /*-{
    return this._session.domain;
  }-*/;

  protected final native boolean isSessionUp() /*-{
    return this._session.sessionId != null;
  }-*/;

  protected final native RemoteViewServiceMultiplexer getWebsocketMultiplexer() /*-{
    return this._wave.websocketMultiplexer;
  }-*/;

  protected final native void setWebsocketMultiplexer(RemoteViewServiceMultiplexer m) /*-{
    this._wave.websocketMultiplexer = m;
  }-*/;

  protected final native WaveWebSocketClient getWebsocketClient() /*-{
    return this._wave.websocketClient;
  }-*/;

  protected final native void setWebsocketClient(WaveWebSocketClient c) /*-{
    this._wave.websocketClient = c;
  }-*/;

  protected final native Map<WaveId, WaveLoader> getLiveWaveRegistry() /*-{
    return this._wave.liveWaveRegistry;
  }-*/;

  protected final native WebAPI setupWaveDependencies(Map<WaveId, WaveLoader> waveRegistry) /*-{

    this._wave = {

      websocketMultiplexer : null,
      websocketClient: null,
      liveWaveRegistry: waveRegistry

    };
    return this;

  }-*/;


  /**
   * Opens a new connection to the server if websocketClient doesn't exist or it
   * has an ERROR state.
   *
   * It requires a user session already opened.
   */
  public static final void connect(final WebAPI webapi, final WebAPICallback callback) {


    if (webapi.getWebsocketClient() == null || webapi.getWebsocketState().equalsIgnoreCase("ERROR")) {

      //
      // WARNING: at this point, all previously opened wavelets are now invalid!
      //
      // TODO hot reconnection of waves
      // Can we reconnect them to the view multiplexer?
      // To investigate how to invalidate current open waves or reconnect them
      //

      final WaveWebSocketClient websocketClient =
          new WaveWebSocketClient(webapi.getWebsocketServer(),
              WebAPIConstants.SWELLRT_PROTOCOL_VERSION);
      websocketClient.attachStatusListener(webapi);

      webapi.setWebsocketClient(websocketClient);

      websocketClient.connect(new WaveSocketStartCallback() {

        @Override
        public void onSuccess() {

          webapi.setWebsocketMultiplexer(new RemoteViewServiceMultiplexer(websocketClient, webapi
              .getUserId()));
          callback.onSuccess(null);
        }

        @Override
        public void onFailure() {
          callback.onFailure(null);
        }
      });

    } else {
      callback.onSuccess(null);
    }


  }

  /**
   * TODO stop using all the old Session stuff. It's wired in the stagging
   * loader.
   */
  public static final native void setFakeSession(String address, String domain, String seed) /*-{

    $wnd.__session = new Object();
    $wnd.__session.address = address;
    $wnd.__session.domain = domain;
    $wnd.__session.id = seed;

  }-*/;

  /**
   * Open or create a wave with the provided Id. If Id is null, a new wave is
   * created with an auto generated Id.
   *
   */
  public static final void live(final WebAPI webapi, final String id, final WebAPICallback callback) {


    if (!webapi.isSessionUp()) {
      callback.onFailure(WebAPIUtils
          .createCallbackError(WebAPIConstants.ERR_CODE_SESSION_NOT_OPENED));
      return;
    }


    final String seed = WebAPIUtils.getRandomBase64(10);

    // TODO stop using this old Session stuff. It's wired in the stagging
    // loader.
    setFakeSession(webapi.getUserId(), webapi.getDomain(), seed);

    // TODO rethink Id generation policy
    if (TypeIdGenerator.get().getUnderlyingGenerator() == null) {
      TypeIdGenerator.get().initialize(new IdGeneratorImpl(webapi.getDomain(), new Seed() {
        @Override
        public String get() {
          return seed;
        }
      }));
    }

    WaveId waveId = null;
    try {
      if (id == null) {
        waveId = TypeIdGenerator.get().newWaveId();
      } else {

        if (id.contains("/"))
          waveId = WaveId.deserialise(id);
        else
          waveId = WaveId.of(webapi.getDomain(), id);
      }
    } catch (IllegalArgumentException e) {
      callback.onFailure(WebAPIUtils
          .createCallbackError(WebAPIConstants.ERR_CODE_INVALID_OBJECT_ID));
    }


    try {

      final WaveRef waveRef = WaveRef.of(waveId);
      final ParticipantId userId = ParticipantId.of(webapi.getUserId());

      final WaveLoader waveLoader =
          new WaveLoader(waveRef, webapi.getWebsocketMultiplexer(), TypeIdGenerator.get()
              .getUnderlyingGenerator(), webapi.getDomain(),
              Collections.<ParticipantId> emptySet(), userId, null);

      waveLoader.load(new Command() {

        @Override
        public void execute() {
          webapi.getLiveWaveRegistry().put(waveRef.getWaveId(), waveLoader);

          // TODO simplify the model static creator, pass just the WaveLoader
          Model model =
              Model.create(waveLoader.getWave().getWave(), webapi.getDomain(), userId, false,
                  waveLoader.getIdGenerator());

          // Inject the JS API
          ProxyAdapter proxyAdapter = new ProxyAdapter(model);
          JavaScriptObject proxyModel = proxyAdapter.getJSObject(model, model.getRoot());

          callback.onSuccess(WebAPIUtils.createCallbackSuccess(proxyModel));
        }
      });

    } catch (InvalidParticipantAddress e) {
      callback.onFailure(WebAPIUtils.createCallbackError(WebAPIConstants.ERR_CODE_INVALID_USER_ID));
    }

  }


  protected final native WebAPI setupWebsocket(String websocketUrl) /*-{

    var _instance = this;
    var _context = this._context;
    this._ws = {

      server: websocketUrl,
      status: "DISCONNECTED",
      statusInfo: null,
      connect: function() {

        var p = new Promise(function(resolve, reject) {

           try {
             @org.swellrt.web.WebAPI::connect(Lorg/swellrt/web/WebAPI;Lorg/swellrt/web/WebAPICallback;)(_instance, {

              onSuccess: function(o) {
                resolve();
              },

              onFailure: function(o) {
                reject();
              }

           });
         } catch (e) {
           reject(e);
         }

        });
        return p;

      }
    };

    return this;

  }-*/;


  protected final native WebAPI setupHttp(String serverUrl, String serviceContext) /*-{

       this._http = {

        server: serverUrl,

        context: serviceContext,

        //
        // Makes a HTTP call with JSON for request and response.
        // Sets all the required headers for the SwellRT server.
        //
        // Returns a promise.
        //
        call: function(method, op, payload) {

          var url = this.server + this.context + op;
          var p = new Promise(function(resolve, reject) {

            try {

              var r = new XMLHttpRequest();
              r.open(method, url);

              r.withCredentials = true;
              r.setRequestHeader("Content-Type", "text/plain; charset=utf-8");

              if ($wnd.sessionStorage) {
                r.setRequestHeader("X-window-id",  $wnd.sessionStorage.getItem("x-swellrt-window-id"));
              }

              r.onreadystatechange = function() {
                  if (r.readyState === 4) {

                    if (r.status === 200) {

                      var type = r.getResponseHeader("Content-Type");
                      resolve(JSON.parse(r.responseText));

                    } else {

                      var error = {
                          status: r.statusText,
                          code: r.status,
                          response: null,
                      };

                      var type = r.getResponseHeader("Content-Type");
                      error.response = JSON.parse(r.responseText);
                      reject(error);

                    }

                  }
               }

              r.send(JSON.stringify(payload));


            } catch(e) {
              reject(e);
            }

          });
         return p;
      }


      }

    return this;

  }-*/;



  protected final native void setupLoginMethods()  /*-{

    this._session = new Object();
    var _instance = this;
    var _http = this._http;

    this.login = function(credentials) {

      return _http.call("POST", "/auth", credentials).then(
            function(response) {
             _instance._session = response;

             try {
               if ($wnd.sessionStorage && $wnd.sessionStorage.getItem("x-swellrt-window-id") != null) {
                 _instance._session.sessionId+=":"+$wnd.sessionStorage.getItem("x-swellrt-window-id");
                }
             } catch(e) {
               console.log("Warning: session storage not available. Disabled per tab/window sessions.");
             }

             console.log(JSON.stringify(_instance.session));

             return _instance._session;
            });

    };


    this.logout = function() {

      return _http.call("POST", "/auth", {})
          .then(
            function(response) {
               _session = response;
               return _session;
            });

      };


  }-*/;


  protected final native void setupDataMethods()  /*-{

     var _instance = this;
     var _context = this._context;
     var _ws = this._ws;

     this.live = function(id) {

       return _ws.connect()
         .then(function() {

              var p = new Promise(function(resolve, reject) {
                  try {
                     @org.swellrt.web.WebAPI::live(Lorg/swellrt/web/WebAPI;Ljava/lang/String;Lorg/swellrt/web/WebAPICallback;)(_instance, id,
                      {
                        onSuccess: function(o) {
                          resolve(o);
                        },

                        onFailure: function(o) {
                          reject(o)
                        }
                     });
                  } catch (e) {
                    reject(e);
                  }
               });

              return p;
          });
     };

   }-*/;



}
