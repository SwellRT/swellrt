package org.swellrt.beta.client;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.ServiceFrontend.ConnectionHandler;
import org.swellrt.beta.client.operation.data.ProfileData;
import org.swellrt.beta.client.wave.RemoteViewServiceMultiplexer;
import org.swellrt.beta.client.wave.WaveWebSocketClient;
import org.swellrt.beta.client.wave.WaveSocket.WaveSocketStartCallback;
import org.swellrt.beta.client.wave.WaveWebSocketClient.ConnectState;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.remote.SObjectRemote;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.Recoverable;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gwt.user.client.Random;

/**
 * This class is the stateful part of a SwellRT client. It supports
 * the {@link ServiceFronted} and {@link Operation} instances.  
 * <p>
 * A Service context has the following responsibilities:
 * <li>Connect/Reconnect the Websocket</li>
 * <li>Keep a registry/cache of Waves/Objects</li>
 * <li>Provide a sanity check method for Wave contexts and the API</li>
 * <p>
 * This context handles exceptions coming from websocket.
 *  
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ServiceContext implements WaveWebSocketClient.StatusListener, ServiceStatus {

  
  public static final String SWELL_DATAMODEL_VERSION = "1.0";
  public static final String WAVEID_NAMESPACE_PREFIX = "s";
  
  // TODO move to utility class
  static final char[] WEB64_ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
      .toCharArray();
  
  static final int WAVE_ID_SEED_LENGTH = 10;
  
  private static String getRandomBase64(int length) {
    StringBuilder result = new StringBuilder(length);
    int bits = 0;
    int bitCount = 0;
    while (result.length() < length) {
      if (bitCount < 6) {
        bits = Random.nextInt();
        bitCount = 32;
      }
      result.append(WEB64_ALPHABET[bits & 0x3F]);
      bits >>= 6;
      bitCount -= 6;
    }
    return result.toString();
  }
  
  /**
   * @return  Websocket server address, e.g. ws://swellrt.acme.com:8080
   */
  private static String getWebsocketAddress(String httpAddress) {
    
    String websocketAddress = httpAddress;
    
    if (!websocketAddress.endsWith("/"))
      websocketAddress += "/";

    if (websocketAddress.startsWith("http://"))
      websocketAddress = websocketAddress.replace("http://", "ws://");
    else if (websocketAddress.startsWith("https://"))
      websocketAddress = websocketAddress.replace("https://", "wss://");

    return websocketAddress;
  }

  
  private Map<WaveId, WaveContext> waveRegistry = new HashMap<WaveId, WaveContext>();
  
  private IdGenerator legacyIdGenerator;
  private SessionManager sessionManager;
  private boolean sessionCookieAvailable = false;
  
  private final String httpAddress;
  private final String websocketAddress;  
  private WaveWebSocketClient websocketClient;
  private SettableFuture<RemoteViewServiceMultiplexer> serviceMultiplexerFuture = SettableFuture.<RemoteViewServiceMultiplexer>create(); 

  private CopyOnWriteSet<ServiceFrontend.ConnectionHandler> connectionHandlers = CopyOnWriteSet.createListSet();
  
  public ServiceContext(SessionManager sessionManager, String httpAddress) {
    this.sessionManager = sessionManager; 
    this.httpAddress = httpAddress;
    this.websocketAddress = getWebsocketAddress(httpAddress);
  }
 
  public void addConnectionHandler(ConnectionHandler h) {
    this.connectionHandlers.add(h);
  }
  
  public void removeConnectionHandler(ConnectionHandler h) {
    this.connectionHandlers.remove(h);
  }
  
  protected void setupIdGenerator() {
    final String seed = getRandomBase64(WAVE_ID_SEED_LENGTH);
    this.legacyIdGenerator = 
        new IdGeneratorImpl(sessionManager.getWaveDomain(), new IdGeneratorImpl.Seed() {
          @Override
          public String get() {
            return seed;
          }          
        });
  }
  
  
  /**
   * @return HTTP server address, e.g. http://swellrt.acme.com:8080
   */
  public String getHTTPAddress() {
    return httpAddress;
  }
 
  
  /**
   * @return true iff the HTTP client sends the session cookie to the server.
   */
  public boolean isSessionCookieAvailable() {
    return sessionCookieAvailable;
  }
  
  public void setSessionCookieAvailability(boolean isEnabled) {
    sessionCookieAvailable = isEnabled;
  }
  
  /**
   * Initializes the service context, resetting first if it is necessary
   * and setting a new session.
   * @param profile
   */
  public void init(ProfileData profile) {
    reset();
    sessionManager.setSession(profile);
    setupIdGenerator();  
  }
 
  public String getWindowId() {
    return sessionManager.getWindowId();
  }
  
  public String getSessionId() {
    return sessionManager.getSessionId();
  }
  
  /**
   * Clean up the internal state of this context.
   * This will normally happen on a session close
   */
  public void reset() {    
    
    // TODO clean text editor    
    for (WaveContext wc: waveRegistry.values())
      wc.close();
    
    waveRegistry.clear();
    
    if (websocketClient != null &&
        (websocketClient.getState() == WaveWebSocketClient.ConnectState.CONNECTED  ||
        websocketClient.getState() == WaveWebSocketClient.ConnectState.CONNECTING))
      websocketClient.disconnect(false);
    
    websocketClient = new WaveWebSocketClient(websocketAddress, SWELL_DATAMODEL_VERSION);
    
    serviceMultiplexerFuture = SettableFuture.<RemoteViewServiceMultiplexer>create();
    
    sessionManager.removeSession();
    
  }
  
  public boolean isSession() {
    return sessionManager.isSession();
  }
  
  public WaveId generateWaveId() {
    return WaveId.of(sessionManager.getWaveDomain(), legacyIdGenerator.newId(WAVEID_NAMESPACE_PREFIX));
  }
  
  
  /**
   * Returns a SObject instance supported by a Wave.
   * @param waveId
   * @param callback
   */
  public void getObject(WaveId waveId, FutureCallback<SObjectRemote> callback) {
    
    if (sessionManager == null || !sessionManager.isSession()) {
      callback.onFailure(new SException(ResponseCode.NOT_LOGGED_IN));
      return;
    }
    
    if (!waveRegistry.containsKey(waveId)) {
      waveRegistry.put(waveId, new WaveContext(waveId, sessionManager.getWaveDomain(), ParticipantId.ofUnsafe(sessionManager.getUserId()), this));
    }     

    WaveContext waveContext = waveRegistry.get(waveId);
    
    // a WaveContext must be reset if it was previously loaded and its state
    // is error
    boolean resetWaveContext = !startWebsocket() && waveContext.isError();

    if (resetWaveContext) {
      Futures.addCallback(serviceMultiplexerFuture,
          new FutureCallback<RemoteViewServiceMultiplexer>() {

            @Override
            public void onSuccess(RemoteViewServiceMultiplexer result) {
              waveContext.init(result, ServiceContext.this.legacyIdGenerator);
              waveContext.getSObject(callback);              
            }

            @Override
            public void onFailure(Throwable t) {
              callback.onFailure(t);
            }

          });

    } else {
      waveContext.getSObject(callback);
    }
    
  }
  
  /**
   * Try to connect the Websocket.
   * 
   * @return true if this call actually starts a new connection
   */
  private boolean startWebsocket() {
    
    if (websocketClient.getState() == WaveWebSocketClient.ConnectState.NEW ||
        websocketClient.getState() == WaveWebSocketClient.ConnectState.ERROR) {

      websocketClient.attachStatusListener(ServiceContext.this);
      websocketClient.connect(sessionManager.getSessionToken(), new WaveSocketStartCallback() {
  
        @Override
        public void onSuccess() {
  
          RemoteViewServiceMultiplexer serviceMultiplexer = new RemoteViewServiceMultiplexer(websocketClient, sessionManager.getUserId());
          for (WaveContext wc : waveRegistry.values())
            wc.init(serviceMultiplexer, ServiceContext.this.legacyIdGenerator);
          
          serviceMultiplexerFuture.set(serviceMultiplexer);
        }
  
        @Override
        public void onFailure(Throwable t) {
          handleWebsocketError(t);
        }
  
      });
    
      return true;
      
    }

    return false;
    
  }

  /**
   * Handle Websocket turbulences
   */
  @Override
  public void onStateChange(ConnectState state, Throwable e) {
    // At this moment we cannot get recovered from
    // a Websocket fatal error, so in that case, let's shutdown
    // all Wave contexts gracefully
    if (state.equals(ConnectState.ERROR)) {
      handleWebsocketError(e);      
    }
    
    SException se = null;
    if (e != null)
      se = new SException(ResponseCode.WEBSOCKET_ERROR.getValue(), e, "Websocket error");
      
    
    for (ServiceFrontend.ConnectionHandler ch: connectionHandlers)
      ch.exec(state.toString(), se);
  }
  
  /**
   * Handles a websocket's (fatal) error. Basic implementation just
   * set this context in a definitive erroneous state.
   * <p>
   * Future implementations could handle a reconnection policy.
   */
  private void handleWebsocketError(Throwable e) {
    
    if (!serviceMultiplexerFuture.isDone())
      serviceMultiplexerFuture.setException(new SException(ResponseCode.WEBSOCKET_ERROR));
    
    for (WaveContext ctx: waveRegistry.values()) {
      ctx.onFailure(new ChannelException(ResponseCode.WEBSOCKET_ERROR, e.getMessage(), e, Recoverable.NOT_RECOVERABLE, null, null));
    }
    
  }

  @Override
  public void check() throws SException {
    
    if (!websocketClient.isConnected()) {
      throw new SException(ResponseCode.WEBSOCKET_ERROR);
    }
    
  }
}
