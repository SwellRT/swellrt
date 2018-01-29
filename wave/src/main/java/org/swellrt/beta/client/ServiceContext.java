package org.swellrt.beta.client;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.ServiceConnection.ConnectionHandler;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.wave.RemoteViewServiceMultiplexer;
import org.swellrt.beta.client.wave.WaveDeps;
import org.swellrt.beta.client.wave.WaveWebSocketClient;
import org.swellrt.beta.client.wave.WaveWebSocketClient.ConnectState;
import org.swellrt.beta.client.wave.WaveWebSocketClient.StartCallback;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.presence.SSession;
import org.swellrt.beta.model.presence.SSessionProvider;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.wave.DiffProvider;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.Recoverable;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.CopyOnWriteSet;
import org.waveprotocol.wave.model.util.Preconditions;
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


  static final int WAVE_ID_SEED_LENGTH = 10;

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

  /**
   * Adapts an {@link Account} object to {@link SSession}
   *
   * @param sessionId
   * @param account
   * @return
   */
  private static SSession toSSession(String sessionId, Account account) {

    ParticipantId participantId = ParticipantId.ofUnsafe(account.getId());
    RgbColor color = WaveDeps.colorGeneratorInstance.getColor(sessionId);


    return new SSession(sessionId, participantId, color, account.getName(),
        participantId.getName());
  }

  private Map<WaveId, WaveContext> waveRegistry = new HashMap<WaveId, WaveContext>();

  private IdGenerator legacyIdGenerator;
  private ServiceSession session;

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

  private final SSessionProvider ssessionProvider;

  public ServiceContext(String httpAddress,
      DiffProvider.Factory diffProviderFactory) {
    this.httpAddress = httpAddress;
    this.websocketAddress = getWebsocketAddress(httpAddress);
    this.diffProviderFactory = diffProviderFactory;
    this.ssessionProvider = new SSessionProvider();
  }

  public void addConnectionHandler(ConnectionHandler h) {
    connectionHandlers.add(h);
  }

  public void removeConnectionHandler(ConnectionHandler h) {
    connectionHandlers.remove(h);
  }

  protected void setupIdGenerator() {
    final String seed = WaveDeps.getRandomBase64(WAVE_ID_SEED_LENGTH);
    this.legacyIdGenerator = new IdGeneratorImpl(session.getWaveDomain(),
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
   * Initializes the service context, resetting first if it is necessary and
   * setting a new session.
   *
   * @param accountData
   */
  public void init(Account account) {
    reset();
    session = ServiceSession.create(account);
    ssessionProvider.update(toSSession(session.getSessionToken(), account));
    setupIdGenerator();
  }

  /**
   * Updates the account information. This can't change session id or
   * participant id.
   *
   * @param account
   */
  public void update(Account account) {
    Preconditions.checkNotNull(account, "Can't update null account object");
    Preconditions.checkArgument(account.getId().equals(session.getParticipantId().getAddress()) , "Account update can't change participant id");

    ssessionProvider.update(toSSession(session.getSessionToken(), account));
  }

  /** Gets the session regarding to http */
  public ServiceSession getServiceSession() {
    return session;
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

    if (session != null) {
      session.destroy();
      session = null;
    }

  }

  public boolean hasSession() {
    return session != null;
  }

  public WaveId generateWaveId(String prefix) {

    if (prefix == null)
      prefix = DEFAULT_WAVEID_PREFIX;

    return WaveId.of(session.getWaveDomain(),
        legacyIdGenerator.newId(prefix));
  }

  /**
   * Returns a SObject instance supported by a Wave.
   *
   * @param waveId
   * @param callback
   */
  public void getObject(WaveId waveId, FutureCallback<SWaveObject> callback) {

    if (!hasSession()) {
      callback.onFailure(new SException(ResponseCode.NOT_LOGGED_IN));
      return;
    }

    if (!waveRegistry.containsKey(waveId)) {
      waveRegistry.put(waveId,
          new WaveContext(waveId,
              session.getWaveDomain(),
              ssessionProvider,
              this,
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

      websocketClient = new WaveWebSocketClient(session.getSessionToken(), websocketAddress);
      websocketClient.attachStatusListener(ServiceContext.this);

      RemoteViewServiceMultiplexer serviceMultiplexer = new RemoteViewServiceMultiplexer(
          websocketClient, session.getParticipantId().getAddress());

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

  public void closeObject(WaveId waveId) throws SException {

    WaveContext context = waveRegistry.get(waveId);
    if (context == null) {
      throw new SException(SException.INVALID_OPERATION, null, "Object is not opened");
    }
    context.close();
    waveRegistry.remove(waveId);

  }

  public SSessionProvider getSession() {
    return ssessionProvider;
  }

}
