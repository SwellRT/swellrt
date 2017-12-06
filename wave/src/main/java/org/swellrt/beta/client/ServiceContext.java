package org.swellrt.beta.client;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.ServiceConnection.ConnectionHandler;
import org.swellrt.beta.client.wave.RemoteViewServiceMultiplexer;
import org.swellrt.beta.client.wave.WaveFactories;
import org.swellrt.beta.client.wave.WaveWebSocketClient;
import org.swellrt.beta.client.wave.WaveWebSocketClient.ConnectState;
import org.swellrt.beta.client.wave.WaveWebSocketClient.StartCallback;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.waveprotocol.wave.client.account.ServerAccountData;
import org.waveprotocol.wave.client.wave.DiffProvider;
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

/**
 * This class is the stateful part of a SwellRT client. It supports the
 * {@link ServiceFronted} and {@link Operation} instances.
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
  public static final String DEFAULT_WAVEID_PREFIX = "s";

  // TODO move to utility class
  static final char[] WEB64_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
      .toCharArray();

  static final int WAVE_ID_SEED_LENGTH = 10;


  private static String getRandomBase64(int length) {
    StringBuilder result = new StringBuilder(length);
    int bits = 0;
    int bitCount = 0;
    while (result.length() < length) {
      if (bitCount < 6) {
        bits = WaveFactories.randomGenerator.nextInt();
        bitCount = 32;
      }
      result.append(WEB64_ALPHABET[bits & 0x3F]);
      bits >>= 6;
      bitCount -= 6;
    }
    return result.toString();
  }

  /**
   * @return Websocket server address, e.g. ws://swellrt.acme.com:8080
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

  private static String WINDOW_ID = getRandomBase64(4);

  static {
    String now = String.valueOf(System.currentTimeMillis());
    WINDOW_ID += now.substring(now.length()-4, now.length());
  }



  private Map<WaveId, WaveContext> waveRegistry = new HashMap<WaveId, WaveContext>();

  private IdGenerator legacyIdGenerator;
  private SessionManager sessionManager;
  private boolean sessionCookieAvailable = false;

  private final String httpAddress;
  private final String websocketAddress;
  private WaveWebSocketClient websocketClient;
  private SettableFuture<RemoteViewServiceMultiplexer> serviceMultiplexerFuture = SettableFuture
      .<RemoteViewServiceMultiplexer> create();

  private final CopyOnWriteSet<DefaultFrontend.ConnectionHandler> connectionHandlers = CopyOnWriteSet
      .createListSet();

  private ConnectState connectState = null;
  private SException exception = null;

  private final DiffProvider.Factory diffProviderFactory;

  public ServiceContext(SessionManager sessionManager, String httpAddress,
      DiffProvider.Factory diffProviderFactory) {
    this.sessionManager = sessionManager;
    this.httpAddress = httpAddress;
    this.websocketAddress = getWebsocketAddress(httpAddress);
    this.diffProviderFactory = diffProviderFactory;
  }

  public void addConnectionHandler(ConnectionHandler h) {
    connectionHandlers.add(h);
  }

  public void removeConnectionHandler(ConnectionHandler h) {
    connectionHandlers.remove(h);
  }

  protected void setupIdGenerator() {
    final String seed = getRandomBase64(WAVE_ID_SEED_LENGTH);
    this.legacyIdGenerator = new IdGeneratorImpl(sessionManager.getWaveDomain(),
        new IdGeneratorImpl.Seed() {
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
   * Initializes the service context, resetting first if it is necessary and
   * setting a new session.
   *
   * @param accountData
   */
  public void init(ServerAccountData accountData) {
    reset();
    sessionManager.setSession(accountData);
    setupIdGenerator();
  }

  public String getSessionId() {
    return sessionManager.getSessionId();
  }

  public String getTransientSessionId() {
    return sessionManager.getTransientSessionId();
  }

  public String getWindowId() {
    return WINDOW_ID;
  }

  public String getParticipantId() {
    return sessionManager.getUserId();
  }

  /**
   * Clean up the internal state of this context. This will normally happen on a
   * session close
   */
  public void reset() {

    // TODO clean text editor
    for (WaveContext wc : waveRegistry.values())
      wc.close();

    waveRegistry.clear();

    if (websocketClient != null) {
      websocketClient.stop(false);
      websocketClient = null;
    }

    serviceMultiplexerFuture = SettableFuture.<RemoteViewServiceMultiplexer> create();
    sessionManager.removeSession();

  }

  public boolean isSession() {
    return sessionManager.isSession();
  }

  public WaveId generateWaveId(String prefix) {

    if (prefix == null)
      prefix = DEFAULT_WAVEID_PREFIX;

    return WaveId.of(sessionManager.getWaveDomain(),
        legacyIdGenerator.newId(prefix));
  }

  /**
   * Returns a SObject instance supported by a Wave.
   *
   * @param waveId
   * @param callback
   */
  public void getObject(WaveId waveId, FutureCallback<SWaveObject> callback) {

    if (sessionManager == null || !sessionManager.isSession()) {
      callback.onFailure(new SException(ResponseCode.NOT_LOGGED_IN));
      return;
    }

    if (!waveRegistry.containsKey(waveId)) {
      waveRegistry.put(waveId, new WaveContext(waveId, sessionManager.getWaveDomain(),
          ParticipantId.ofUnsafe(sessionManager.getUserId()), this,
          diffProviderFactory.get(waveId)));
    }

    WaveContext waveContext = waveRegistry.get(waveId);

    lazyWebsocketStart();

    Futures.addCallback(serviceMultiplexerFuture,
        new FutureCallback<RemoteViewServiceMultiplexer>() {

          @Override
          public void onSuccess(RemoteViewServiceMultiplexer multiplexer) {

            if (!waveContext.isActive())
              waveContext.init(multiplexer, ServiceContext.this.legacyIdGenerator);

            waveContext.getSObject(callback);
          }

          @Override
          public void onFailure(Throwable t) {
            callback.onFailure(t);
          }

        });


  }

  /**
   * Try to connect the Websocket.
   *
   * @return true if this call actually starts a new connection
   */
  private boolean lazyWebsocketStart() {

    if (websocketClient == null) {

      String sessionToken = sessionManager.getSessionId()+":"+getTransientSessionId()+":"+getWindowId();
      websocketClient = new WaveWebSocketClient(sessionToken, websocketAddress);
      websocketClient.attachStatusListener(ServiceContext.this);

      RemoteViewServiceMultiplexer serviceMultiplexer = new RemoteViewServiceMultiplexer(
          websocketClient, sessionManager.getUserId());

      websocketClient.start(new StartCallback() {

        @Override
        public void onStart() {
          serviceMultiplexerFuture.set(serviceMultiplexer);
        }

        @Override
        public void onFailure(String e) {
          serviceMultiplexerFuture
              .setException(new SException(SException.WEBSOCKET_ERROR, null, e));
          onStateChange(ConnectState.ERROR, e);
        }
      });
      return true;
    }

    return false;

  }

  /**
   * Handle WebSocket status
   */
  @Override
  public void onStateChange(ConnectState state, String e) {

    if (connectState == ConnectState.ERROR) {
      // ignore further error messages
      return;
    }

    // At this moment we cannot get recovered from
    // a Websocket fatal error, so in that case, let's shutdown
    // all Wave contexts gracefully
    SException sexception = null;

    if (state.equals(ConnectState.ERROR)) {
      sexception = new SException(SException.WEBSOCKET_ERROR, null, e);

      if (!serviceMultiplexerFuture.isDone())
        serviceMultiplexerFuture.setException(new SException(SException.WEBSOCKET_ERROR));

      for (WaveContext ctx : waveRegistry.values()) {
        ctx.onFailure(new ChannelException(ResponseCode.WEBSOCKET_ERROR, e, null,
            Recoverable.NOT_RECOVERABLE, null, null));
      }
    }

    connectState = state;
    exception = sexception;

    for (DefaultFrontend.ConnectionHandler ch : connectionHandlers)
      ch.exec(state.toString(), sexception);
  }

  @Override
  public void check() throws SException {
    if (connectState == ConnectState.ERROR) {

      if (exception != null)
        throw exception;
      else
        throw new SException(ResponseCode.UNKNOWN);

    } else if (!websocketClient.isConnected()) {
      throw new SException(ResponseCode.WEBSOCKET_ERROR);
    }
  }

  @Override
  public void raise(String waveId, SException ex) {
    if (connectState == ConnectState.ERROR)
      return;

    connectState = ConnectState.ERROR;

    for (DefaultFrontend.ConnectionHandler ch : connectionHandlers)
      ch.exec(connectState.toString(), ex);
  }

  public String getWaveDomain() {
    return sessionManager.getWaveDomain();
  }

  public void closeObject(WaveId waveId) throws SException {

    WaveContext context = waveRegistry.get(waveId);
    if (context == null) {
      throw new SException(SException.INVALID_OPERATION, null, "Object is not opened");
    }
    context.close();
    waveRegistry.remove(waveId);

  }

}
