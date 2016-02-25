/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.box.server.rpc;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;

import org.apache.commons.lang.StringUtils;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.AtmosphereResponse;
import org.atmosphere.guice.AtmosphereGuiceServlet;
import org.atmosphere.util.IOUtils;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.server.session.HashSessionManager;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.eclipse.jetty.servlets.GzipFilter;
import org.eclipse.jetty.util.resource.ResourceCollection;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
import org.swellrt.model.generic.Model;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolAuthenticate;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolAuthenticationResult;
import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.ClientServerExecutor;
import org.waveprotocol.box.server.persistence.file.FileUtils;
import org.waveprotocol.box.server.rpc.atmosphere.AtmosphereChannel;
import org.waveprotocol.box.server.rpc.atmosphere.AtmosphereClientInterceptor;
import org.waveprotocol.box.server.util.NetUtils;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import javax.annotation.Nullable;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContextListener;
import javax.servlet.SessionTrackingMode;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * ServerRpcProvider can provide instances of type Service over an incoming
 * network socket and service incoming RPCs to these services and their methods.
 *
 *
 */
public class ServerRpcProvider {
  private static final Log LOG = Log.get(ServerRpcProvider.class);
  /**
   * The buffer size is passed to implementations of {@link WaveWebSocketServlet} as init
   * param. It defines the response buffer size.
   */
  private static final int BUFFER_SIZE = 1024 * 1024;

  private final InetSocketAddress[] httpAddresses;
  private final Executor threadPool;
  private final SessionManager sessionManager;
  private final org.eclipse.jetty.server.SessionManager jettySessionManager;
  private Server httpServer = null;
  private final boolean sslEnabled;
  private final String sslKeystorePath;
  private final String sslKeystorePassword;



  private static ConcurrentHashMap<String, Connection> CONNECTIONS =
      new ConcurrentHashMap<String, Connection>();

  // Mapping from incoming protocol buffer type -> specific handler.
  private final Map<Descriptors.Descriptor, RegisteredServiceMethod> registeredServices =
      Maps.newHashMap();

  // List of webApp source directories ("./war", etc)
  private final String[] resourceBases;

  private final String sessionStoreDir;

  private int webSocketMaxIdleTime;
  private int webSocketMaxMessageSize;
  private int websocketHeartbeat;

  private final int sessionMaxInactiveTime;

  private final static String SESSION_URL_PARAM = "sid";
  private final static String SESSION_COOKIE_NAME = "WSESSIONID";

  /**
   * Internal, static container class for any specific registered service
   * method.
   */
  static class RegisteredServiceMethod {
    final Service service;
    final MethodDescriptor method;

    RegisteredServiceMethod(Service service, MethodDescriptor method) {
      this.service = service;
      this.method = method;
    }
  }

  static class WebSocketConnection extends Connection {
    private final WebSocketChannel socketChannel;

    WebSocketConnection(ParticipantId loggedInUser, ServerRpcProvider provider) {
      super(loggedInUser, provider);
      socketChannel = new WebSocketChannelImpl(this);
      LOG.info("New websocket connection set up for user " + loggedInUser);
      expectMessages(socketChannel);
    }

    @Override
    protected void sendMessage(int sequenceNo, Message message) {
      socketChannel.sendMessage(sequenceNo, message);
    }

    public WebSocketChannel getWebSocketServerChannel() {
      return socketChannel;
    }
  }

  /**
   * A Wave server's RPC connection with a remote client using the Atmosphere
   * transport protocol.
   *
   * It wraps an underlying {@AtmosphereChannel} instance
   * which is the actual Atmosphere's interface.
   *
   * @author pablojan@gmail.com (Pablo Ojanguren)
   *
   */
  static class AtmosphereConnection extends Connection {

    private final AtmosphereChannel atmosphereChannel;
    private final String sessionId;

    public AtmosphereConnection(String sessionId, ParticipantId loggedInUser,
        ServerRpcProvider provider) {
      super(loggedInUser, provider);

      this.sessionId = sessionId;
      atmosphereChannel = new AtmosphereChannel(this, sessionId);
      expectMessages(atmosphereChannel);

    }

    @Override
    protected void sendMessage(int sequenceNo, Message message) {
      atmosphereChannel.sendMessage(sequenceNo, message);
    }

    public AtmosphereChannel getAtmosphereChannel() {
      return atmosphereChannel;
    }


  }



  static abstract class Connection implements ProtoCallback {
    private final Map<Integer, ServerRpcController> activeRpcs =
        new ConcurrentHashMap<Integer, ServerRpcController>();

    // The logged in user.
    // Note: Due to this bug:
    // http://code.google.com/p/wave-protocol/issues/detail?id=119,
    // the field may be null on first connect and then set later using an RPC.
    private ParticipantId loggedInUser;

    private final ServerRpcProvider provider;

    private boolean isFirstRequest = true;

    private boolean isStatusOk = true;

    /**
     * @param loggedInUser The currently logged in user, or null if no user is
     *        logged in.
     * @param provider
     */
    public Connection(ParticipantId loggedInUser, ServerRpcProvider provider) {
      this.loggedInUser = loggedInUser;
      this.provider = provider;
    }

    protected void expectMessages(MessageExpectingChannel channel) {
      synchronized (provider.registeredServices) {
        for (RegisteredServiceMethod serviceMethod : provider.registeredServices.values()) {
          channel.expectMessage(serviceMethod.service.getRequestPrototype(serviceMethod.method));
          LOG.fine("Expecting: " + serviceMethod.method.getFullName());
        }
      }
      channel.expectMessage(Rpc.CancelRpc.getDefaultInstance());
    }

    protected abstract void sendMessage(int sequenceNo, Message message);

    private ParticipantId authenticate(String token) {
      HttpSession session = provider.sessionManager.getSessionFromToken(token);
      ParticipantId user = provider.sessionManager.getLoggedInUser(session);
      return user;
    }

    protected boolean isStatusOk() {
      return isStatusOk;
    }

    public ParticipantId getParticipantId() {
      return loggedInUser;
    }

    @Override
    public void message(final int sequenceNo, Message message) {
      final String messageName = "/" + message.getClass().getSimpleName();
      final Timer profilingTimer = Timing.startRequest(messageName);

      // Protocol hack allowing reconnection in the transport (atmosphere)
      // level:
      // Connection is now tied to the client's Http session. So we must
      // detect when a remote client makes a reconnection (page reload...)
      // without
      // destroying the session.

      if (isFirstRequest && sequenceNo != 0) {
        LOG.warning("Connection first request with sequence number not 0. Dirty reconnection?");
      }

      isFirstRequest = false;

      // Clean up ServerRpcControllers if remote client starts a new
      // sequence of protocol messages.
      if (sequenceNo == 0) {
        if (!activeRpcs.isEmpty()) {
          LOG.info("Detected new remote client connection. Cleaning up RPCs");
          for (ServerRpcController controller : activeRpcs.values()) {
            if (!controller.isCanceled()) {
              controller.cancel();
            }
          }
          activeRpcs.clear();
        }
      }

      if (message instanceof Rpc.CancelRpc) {
        final ServerRpcController controller = activeRpcs.get(sequenceNo);
        if (controller == null) {
          throw new IllegalStateException("Trying to cancel an RPC that is not active!");
        } else {
          LOG.info("Cancelling open RPC " + sequenceNo);
          controller.cancel();
        }
      } else if (message instanceof ProtocolAuthenticate) {
        // Workaround for bug: http://codereview.waveprotocol.org/224001/

        // When we get this message, either the connection will not be logged in
        // (loggedInUser == null) or the connection will have been authenticated
        // via cookies
        // (in which case loggedInUser must match the authenticated user, and
        // this message has no
        // effect).

        ProtocolAuthenticate authMessage = (ProtocolAuthenticate) message;
        ParticipantId authenticatedAs = authenticate(authMessage.getToken());
        Preconditions.checkArgument(authenticatedAs != null, "Auth token invalid");
        Preconditions.checkArgument(loggedInUser.equals(authenticatedAs),
            "Protocol user doesn't match session user");
        LOG.info("Protocol authenticated as " + loggedInUser);
        sendMessage(sequenceNo, ProtocolAuthenticationResult.getDefaultInstance());
      } else if (provider.registeredServices.containsKey(message.getDescriptorForType())) {
        if (activeRpcs.containsKey(sequenceNo)) {
          throw new IllegalStateException(
              "Can't invoke a new RPC with a sequence number already in use.");
        } else {
          final RegisteredServiceMethod serviceMethod =
              provider.registeredServices.get(message.getDescriptorForType());

          // Create the internal ServerRpcController used to invoke the call.
          final ServerRpcController controller =
              new ServerRpcControllerImpl(message, serviceMethod.service, serviceMethod.method,
                  loggedInUser, new RpcCallback<Message>() {
                    @Override
                    synchronized public void run(Message message) {
                      if (message instanceof Rpc.RpcFinished
                          || !serviceMethod.method.getOptions().getExtension(Rpc.isStreamingRpc)) {
                        // This RPC is over - remove it from the map.
                        boolean failed = message instanceof Rpc.RpcFinished
                            ? ((Rpc.RpcFinished) message).getFailed() : false;
                        LOG.fine("RPC " + sequenceNo + " is now finished, failed = " + failed);
                        if (failed) {
                          LOG.info("error = " + ((Rpc.RpcFinished) message).getErrorText());
                        }
                        activeRpcs.remove(sequenceNo);
                      }
                      sendMessage(sequenceNo, message);
                      if (profilingTimer != null) {
                        Timing.stop(profilingTimer);
                      }
                    }
                  });

          // Kick off a new thread specific to this RPC.
          activeRpcs.put(sequenceNo, controller);
          provider.threadPool.execute(controller);
        }
      } else {
        // Sent a message type we understand, but don't expect - erronous case!
        throw new IllegalStateException(
            "Got expected but unknown message  (" + message + ") for sequence: " + sequenceNo);
      }
    }
  }

  /**
   * Construct a new ServerRpcProvider, hosting on the specified
   * WebSocket addresses.
   *
   * Also accepts an ExecutorService for spawning managing threads.
   */
  public ServerRpcProvider(InetSocketAddress[] httpAddresses,
      String[] resourceBases, Executor threadPool, SessionManager sessionManager,
      org.eclipse.jetty.server.SessionManager jettySessionManager, String sessionStoreDir,
      boolean sslEnabled, String sslKeystorePath, String sslKeystorePassword,
      int webSocketMaxIdleTime, int webSocketMaxMessageSize, int websocketHeartbeat,
      int sessionMaxInactiveTime) {
    this.httpAddresses = httpAddresses;
    this.resourceBases = resourceBases;
    this.threadPool = threadPool;
    this.sessionManager = sessionManager;
    this.jettySessionManager = jettySessionManager;
    this.sessionStoreDir = sessionStoreDir;
    this.sslEnabled = sslEnabled;
    this.sslKeystorePath = sslKeystorePath;
    this.sslKeystorePassword = sslKeystorePassword;
    this.webSocketMaxIdleTime = webSocketMaxIdleTime;
    this.webSocketMaxMessageSize = webSocketMaxMessageSize;
    this.websocketHeartbeat = websocketHeartbeat;
    this.sessionMaxInactiveTime = sessionMaxInactiveTime;
  }

  /**
   * Constructs a new ServerRpcProvider with a default ExecutorService.
   */
  public ServerRpcProvider(InetSocketAddress[] httpAddresses, String[] resourceBases,
      SessionManager sessionManager, org.eclipse.jetty.server.SessionManager jettySessionManager,
      String sessionStoreDir, boolean sslEnabled, String sslKeystorePath,
      String sslKeystorePassword, Executor executor, int webSocketMaxIdleTime,
      int webSocketMaxMessageSize, int websocketHeartbeat, int sessionMaxInactiveTime) {
    this(httpAddresses, resourceBases, executor, sessionManager, jettySessionManager,
        sessionStoreDir, sslEnabled, sslKeystorePath, sslKeystorePassword, webSocketMaxIdleTime,
        webSocketMaxMessageSize, websocketHeartbeat, sessionMaxInactiveTime);
  }

  @Inject
  public ServerRpcProvider(@Named(CoreSettings.HTTP_FRONTEND_ADDRESSES) List<String> httpAddresses,
      @Named(CoreSettings.HTTP_WEBSOCKET_PUBLIC_ADDRESS) String websocketAddress,
      @Named(CoreSettings.RESOURCE_BASES) List<String> resourceBases,
      SessionManager sessionManager, org.eclipse.jetty.server.SessionManager jettySessionManager,
      @Named(CoreSettings.SESSIONS_STORE_DIRECTORY) String sessionStoreDir,
      @Named(CoreSettings.ENABLE_SSL) boolean sslEnabled,
      @Named(CoreSettings.SSL_KEYSTORE_PATH) String sslKeystorePath,
      @Named(CoreSettings.SSL_KEYSTORE_PASSWORD) String sslKeystorePassword,
      @ClientServerExecutor Executor executorService,
      @Named(CoreSettings.WEBSOCKET_MAX_IDLE_TIME) int webSocketMaxIdleTime,
      @Named(CoreSettings.WEBSOCKET_MAX_MESSAGE_SIZE) int webSocketMaxMessageSize,
      @Named(CoreSettings.WEBSOCKET_HEARTBEAT) int websocketHeartbeat,
      @Named(CoreSettings.SESSION_SERVER_MAX_INACTIVE_TIME) int sessionMaxInactiveTime) {
    this(parseAddressList(httpAddresses, websocketAddress), resourceBases
        .toArray(new String[0]), sessionManager, jettySessionManager, sessionStoreDir,
 sslEnabled, sslKeystorePath,
        sslKeystorePassword, executorService, webSocketMaxIdleTime, webSocketMaxMessageSize,
        websocketHeartbeat, sessionMaxInactiveTime);
  }

  public void startWebSocketServer(final Injector injector) {
    httpServer = new Server();

    List<Connector> connectors = getSelectChannelConnectors(httpAddresses);
    if (connectors.isEmpty()) {
      LOG.severe("No valid http end point address provided!");
    }


    for (Connector connector : connectors) {
      httpServer.addConnector(connector);
    }

    final WebAppContext context = new WebAppContext();

    context.setParentLoaderPriority(true);

    if (jettySessionManager != null) {
      context.getSessionHandler().setSessionManager(jettySessionManager);
      context.getSessionHandler().getSessionManager()
          .setMaxInactiveInterval(sessionMaxInactiveTime);

      Set<SessionTrackingMode> sessionTrackingModes = new HashSet<SessionTrackingMode>();
      sessionTrackingModes.add(SessionTrackingMode.URL);
      sessionTrackingModes.add(SessionTrackingMode.COOKIE);
      context.getSessionHandler().getSessionManager().setSessionTrackingModes(sessionTrackingModes);
      context.getSessionHandler().getSessionManager()
          .setSessionIdPathParameterName(SESSION_URL_PARAM);

    }
    final ResourceCollection resources = new ResourceCollection(resourceBases);
    context.setBaseResource(resources);

    context.setInitParameter("org.eclipse.jetty.servlet.SessionCookie", SESSION_COOKIE_NAME);

    FilterHolder corsFilterHolder = new FilterHolder(CrossOriginFilter.class);
    corsFilterHolder.setInitParameter("allowedOrigins", "*");
    // Set explicit methods to allow CORS with DELETE
    corsFilterHolder.setInitParameter("allowedMethods", "GET,POST,DELETE,PUT,HEAD");
    corsFilterHolder.setInitParameter("allowedHeaders", "*");
    context.addFilter(corsFilterHolder, "/*", EnumSet.allOf(DispatcherType.class));

    addWebSocketServlets();

    try {
      final Injector parentInjector = injector;

      final ServletModule servletModule = getServletModule(parentInjector);

      ServletContextListener contextListener = new GuiceServletContextListener() {

        private final Injector childInjector = parentInjector.createChildInjector(servletModule);

        @Override
        protected Injector getInjector() {
          return childInjector;
        }
      };

      context.addEventListener(contextListener);
      context.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
      context.addFilter(GzipFilter.class, "/webclient/*", EnumSet.allOf(DispatcherType.class));

      /*
       * String[] hosts = new String[httpAddresses.length]; for (int i = 0; i <
       * httpAddresses.length; i++) { hosts[i] =
       * httpAddresses[i].getHostString(); hosts[i] =
       * httpAddresses[i].getHostString(); } context.addVirtualHosts(hosts);
       */

      httpServer.setHandler(context);

      httpServer.start();

      restoreSessions();

    } catch (Exception e) { // yes, .start() throws "Exception"
      LOG.severe("Fatal error starting http server.", e);
      return;
    }
    LOG.fine("WebSocket server running.");
  }

  private void restoreSessions() {
    try {
      HashSessionManager hashSessionManager = (HashSessionManager) jettySessionManager;
      hashSessionManager.setStoreDirectory(FileUtils.createDirIfNotExists(sessionStoreDir,
          "Session persistence"));
      hashSessionManager.setSavePeriod(60);
      hashSessionManager.restoreSessions();
    } catch (Exception e) {
      LOG.warning("Cannot restore sessions");
    }
  }
  public void addWebSocketServlets() {
    // Servlet where the websocket connection is served from.
    ServletHolder wsholder = addServlet("/socket", WaveWebSocketServlet.class);
    // TODO(zamfi): fix to let messages span frames.
    wsholder.setInitParameter("bufferSize", "" + BUFFER_SIZE);

    // Atmosphere framework. Replacement of Socket.IO
    // See https://issues.apache.org/jira/browse/WAVE-405
    ServletHolder atholder = addServlet("/atmosphere*", AtmosphereGuiceServlet.class);
    // Avoid loading defualt CORS interceptor which is in conflict with general
    // jetty CORS filter
    atholder.setInitParameter("org.atmosphere.cpr.AtmosphereInterceptor.disableDefaults", "true");
    // Setting a low HeartBeat frequency to avoid network issues. Let clients to
    // set a hihger value.
    atholder.setInitParameter(
        "org.atmosphere.interceptor.HeartbeatInterceptor.heartbeatFrequencyInSeconds", ""
            + websocketHeartbeat);

    // Setting all buffers at least to 2MB
    atholder.setInitParameter("org.atmosphere.websocket.maxTextMessageSize", ""
        + (BUFFER_SIZE * webSocketMaxMessageSize));
    atholder.setInitParameter("org.atmosphere.websocket.maxBinaryMessageSize", ""
        + (BUFFER_SIZE * webSocketMaxMessageSize));
    atholder.setInitParameter("org.atmosphere.websocket.webSocketBufferingMaxSize", ""
        + (BUFFER_SIZE * webSocketMaxMessageSize));

    // Enable guice. See
    // https://github.com/Atmosphere/atmosphere/wiki/Configuring-Atmosphere%27s-Classes-Creation-and-Injection
    atholder.setInitParameter("org.atmosphere.cpr.objectFactory",
        "org.waveprotocol.box.server.rpc.atmosphere.GuiceAtmosphereFactory");
    atholder.setAsyncSupported(true);
    atholder.setInitOrder(0);

    jettySessionManager.addEventListener(new HttpSessionListener() {

      @Override
      public void sessionCreated(HttpSessionEvent arg0) {
        LOG.info("Session created: " + arg0.getSession().getId());
        arg0.getSession().setMaxInactiveInterval(sessionMaxInactiveTime);
      }

      @Override
      public void sessionDestroyed(HttpSessionEvent arg0) {
        LOG.info("Session destroyed: " + arg0.getSession().getId());
      }

    });

    // Serve the static content and GWT web client with the default servlet
    // (acts like a standard file-based web server).
    // addServlet("/static/*", DefaultServlet.class);
    // addServlet("/webclient/*", DefaultServlet.class);
  }

  public ServletModule getServletModule(final Injector injector) {

    return new ServletModule() {
      @Override
      protected void configureServlets() {
        // We add servlets here to override the DefaultServlet automatic registered by WebAppContext
        // in path "/" with our WaveClientServlet. Any other way to do this?
        // Related question (unanswered 08-Apr-2011)
        // http://web.archiveorange.com/archive/v/d0LdlXf1kN0OXyPNyQZp
        for (Pair<String, ServletHolder> servlet : servletRegistry) {
          String url = servlet.getFirst();
          @SuppressWarnings("unchecked")
          Class<HttpServlet> clazz = (Class<HttpServlet>) servlet.getSecond().getHeldClass();
          Map<String,String> params = servlet.getSecond().getInitParameters();
          serve(url).with(clazz,params);
          bind(clazz).in(Singleton.class);
        }
        for (Pair<String, Class<? extends Filter>> filter : filterRegistry) {
          filter(filter.first).through(filter.second);
        }
      }
    };
  }

  private static InetSocketAddress[] parseAddressList(List<String> addressList, String websocketAddress) {
    if (addressList == null || addressList.size() == 0) {
      return new InetSocketAddress[0];
    } else {
      Set<InetSocketAddress> addresses = Sets.newHashSet();
      // We add the websocketAddress as another listening address.
      ArrayList<String> mergedAddressList = new ArrayList<String>(addressList);
      if (!StringUtils.isEmpty(websocketAddress)) {
        mergedAddressList.add(websocketAddress);
      }
      for (String str : mergedAddressList) {
        if (str.length() == 0) {
          LOG.warning("Encountered empty address in http addresses list.");
        } else {
          try {
            InetSocketAddress address = NetUtils.parseHttpAddress(str);
            if (!addresses.contains(address)) {
              addresses.add(address);
            } else {
              LOG.warning(
                  "Ignoring duplicate address in http addresses list: Duplicate entry '" + str
                      + "' resolved to " + address.getAddress().getHostAddress());
            }
          } catch (IOException e) {
            LOG.severe("Unable to process address " + str, e);
          }
        }
      }
      return addresses.toArray(new InetSocketAddress[0]);
    }
  }

  /**
   * @return a list of {@link SelectChannelConnector} each bound to a host:port
   *         pair form the list addresses.
   */
  private List<Connector> getSelectChannelConnectors(
      InetSocketAddress[] httpAddresses) {
    List<Connector> list = Lists.newArrayList();
    String[] excludeCiphers = {"SSL_RSA_EXPORT_WITH_RC4_40_MD5", "SSL_RSA_EXPORT_WITH_DES40_CBC_SHA",
                               "SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA", "SSL_RSA_WITH_DES_CBC_SHA",
                               "SSL_DHE_RSA_WITH_DES_CBC_SHA", "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
                               "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA"};
    SslContextFactory sslContextFactory = null;

    if (sslEnabled) {
      Preconditions.checkState(sslKeystorePath != null && !sslKeystorePath.isEmpty(),
          "SSL Keystore path left blank");
      Preconditions.checkState(sslKeystorePassword != null && !sslKeystorePassword.isEmpty(),
          "SSL Keystore password left blank");

      sslContextFactory = new SslContextFactory(sslKeystorePath);
      sslContextFactory.setKeyStorePassword(sslKeystorePassword);
      sslContextFactory.setRenegotiationAllowed(false);
      sslContextFactory.setExcludeCipherSuites(excludeCiphers);

      // Note: we only actually needed client auth for AuthenticationServlet.
      // Using Need instead of Want prevents web-sockets from working on
      // Chrome.
      sslContextFactory.setWantClientAuth(true);
    }

    for (InetSocketAddress address : httpAddresses) {
      ServerConnector connector;
      if (sslEnabled) {
        connector = new ServerConnector(httpServer, sslContextFactory);
      } else {
        connector = new ServerConnector(httpServer);
      }
      // Allow access by IP and Hostname
      connector.setHost(address.getAddress().getHostAddress());
      connector.setPort(address.getPort());
      connector.setIdleTimeout(0);
      list.add(connector);
    }

    return list;
  }

  @SuppressWarnings("serial")
  @Singleton
  public static class WaveWebSocketServlet extends WebSocketServlet {

    final ServerRpcProvider provider;
    final int websocketMaxIdleTime;
    final int websocketMaxMessageSize;

    @Inject
    public WaveWebSocketServlet(ServerRpcProvider provider,
        @Named(CoreSettings.WEBSOCKET_MAX_IDLE_TIME) int websocketMaxIdleTime,
        @Named(CoreSettings.WEBSOCKET_MAX_MESSAGE_SIZE) int websocketMaxMessageSize) {
      super();
      this.provider = provider;
      this.websocketMaxIdleTime = websocketMaxIdleTime;
      this.websocketMaxMessageSize = websocketMaxMessageSize;
    }

    @SuppressWarnings("cast")
    @Override
    public void configure(WebSocketServletFactory factory) {
      if (websocketMaxIdleTime == 0) {
        // Jetty does not allow to set infinite timeout.
        factory.getPolicy().setIdleTimeout(Integer.MAX_VALUE);
      } else {
        factory.getPolicy().setIdleTimeout(websocketMaxIdleTime);
      }
      factory.getPolicy().setMaxTextMessageSize(websocketMaxMessageSize * 1024 * 1024);
      factory.setCreator(new WebSocketCreator() {
        @Override
        public Object createWebSocket(ServletUpgradeRequest req, ServletUpgradeResponse resp) {
          ParticipantId loggedInUser =
              provider.sessionManager.getLoggedInUser((HttpSession) req.getSession());

          return new WebSocketConnection(loggedInUser, provider).getWebSocketServerChannel();
        }
      });
    }
  }

  /**
   * Manange atmosphere connections and dispatch messages to wave channels.
   * 
   * This Atmosphere handler supports both WebSocket and Long-polling
   * connections with the following features:
   * 
   * <ul>
   * <li>Detect session expiration and close remote clients properly.</li>
   * <li>Detect server reboot refusing further operations. Close remote clientes
   * properly.</li>
   * <li>Allow reconnection of remote clients if HTTP session is still active
   * despite the transport failure.</li>
   * <li>Keep connections opened sending heartbeat signasl</li>
   * <li>Remote clients reconnection on timeout to avoid silent network
   * failures.</li>
   * </ul>
   * 
   * Atmosphere interceptors are set manually here, to avoid duplicated CORS
   * response headers.
   * 
   * About session tracking: The session token is expected to be stored as a
   * cookie by default. In some cases where cookies are not available (Browser
   * previnting 3rd party cookies,...) the session token can be propagated as a
   * path element /atmosphere/sessionId.
   * 
   * 
   * @author pablojan@gmail.com <Pablo Ojanguren>
   * 
   */
  @Singleton
  @AtmosphereHandlerService(path = "/atmosphere",
 interceptors = {
      AtmosphereClientInterceptor.class, org.atmosphere.interceptor.CacheHeadersInterceptor.class,
      org.atmosphere.interceptor.PaddingAtmosphereInterceptor.class,
      org.atmosphere.interceptor.AndroidAtmosphereInterceptor.class,
      org.atmosphere.interceptor.HeartbeatInterceptor.class,
      org.atmosphere.interceptor.SSEAtmosphereInterceptor.class,
      org.atmosphere.interceptor.JSONPAtmosphereInterceptor.class,
      org.atmosphere.interceptor.JavaScriptProtocol.class,
      org.atmosphere.interceptor.OnDisconnectInterceptor.class,
      org.atmosphere.interceptor.IdleResourceInterceptor.class,
      org.atmosphere.interceptor.WebSocketMessageSuspendInterceptor.class,
      org.atmosphere.interceptor.TrackMessageSizeB64Interceptor.class},
      broadcasterCache = UUIDBroadcasterCache.class)
  public static class WaveAtmosphereService implements AtmosphereHandler {

    private static final Log LOG = Log.get(WaveAtmosphereService.class);

    private static final String CHARSET = "UTF-8";
    private static final String SEPARATOR = "|";


    @Inject
    public ServerRpcProvider provider;


    private HttpSession getSession(AtmosphereResource resource) {
      HttpSession session = null;

      Cookie[] cookies = resource.getRequest().getCookies();

      if (cookies != null)
        for (Cookie c : cookies) {
          if (c.getName().equals(SESSION_COOKIE_NAME))
            session = provider.sessionManager.getSessionFromToken(c.getValue());
        }

      // Try with the session URL path /atmosphere/<sessionId>
      if (session == null) {
        int lastPathSeparatorIndex = resource.getRequest().getPathInfo().lastIndexOf("/");
        if (lastPathSeparatorIndex >= 0) {
          String sessionToken =
              resource.getRequest().getPathInfo().substring(lastPathSeparatorIndex + 1);
          session = provider.sessionManager.getSessionFromToken(sessionToken);
        }
      }

      return session;
    }


    protected void flushResponse(AtmosphereResource resource) {

      try {

        resource.getResponse().flushBuffer();

        switch (resource.transport()) {
          case JSONP:
          case LONG_POLLING:
            resource.resume();
            break;
          case WEBSOCKET:
          case STREAMING:
          case SSE:
            resource.getResponse().getOutputStream().flush();
            break;
          default:
            LOG.info("Unknown transport");
            break;
        }
      } catch (IOException e) {
        LOG.warning("Error resuming resource response", e);
      }
    }


    @Override
    public void onRequest(AtmosphereResource resource) throws IOException {



      // If it's the old client then force the connection close
      if (!resource.getRequest().headersMap().containsKey("X-client-version")) {
        resource.getResponse().sendError(500, "client needs upgrade");
        resource.close();
        return;
      }

      String clientVersionHeader = resource.getRequest().getHeader("X-client-version");
      boolean needClientUpgrade =
          (clientVersionHeader == null) || (!clientVersionHeader.equals(Model.MODEL_VERSION));


      HttpSession httpSession = getSession(resource);

      if (httpSession == null) {
        resource.getResponse().sendError(500, "session has expired");
        resource.close();
        return;
      }

      ParticipantId loggedInUser = provider.sessionManager.getLoggedInUser(httpSession);



      // WebSocket Transport:
      //
      // A base resource is the WebSocket connection
      // A new resource for each remote client request
      // Wave Connections are associated to the user session to
      // share it across request's resources

      if (resource.transport().equals(TRANSPORT.WEBSOCKET)) {

        // Cache connectios by session id and transport to allow reconnections
        // of the same user with different transport
        String key = httpSession.getId() + ":WS";

        if (!CONNECTIONS.containsKey(key))
          CONNECTIONS.put(key,
              new AtmosphereConnection(httpSession.getId(),
              loggedInUser, provider));

        AtmosphereConnection connection = (AtmosphereConnection) CONNECTIONS.get(key);

        // A connection exists for a different participant:
        // This happens when different users shares the same browser session.
        // Ensure that connections are cleaned up.
        if (!connection.getParticipantId().equals(loggedInUser)) {
          connection = new AtmosphereConnection(httpSession.getId(), loggedInUser, provider);
          CONNECTIONS.put(key, connection);
        }



        if (resource.getRequest().getMethod().equalsIgnoreCase("GET")) {

          // LOG.info("Websocket suspending request " + resource.uuid());

          resource.suspend();

          if (!connection.getAtmosphereChannel().hasResources()) {
            connection.getAtmosphereChannel().bindResource(resource);
          }


          if (needClientUpgrade) {
            connection.getAtmosphereChannel().onClientNeedUpgrade();
            connection.getAtmosphereChannel().unbindResource();
            return;
          }


        } else if (resource.getRequest().getMethod().equalsIgnoreCase("POST")) {

          // LOG.info("Websocket proccessing request " + resource.uuid());

          StringBuilder b = IOUtils.readEntirely(resource);
          try {
            connection.getAtmosphereChannel().onMessage(b.toString());
          } catch (RuntimeException e) {
            LOG.info("Exception on channel.", e);
          }

        }

      } else if (resource.transport().equals(TRANSPORT.LONG_POLLING)
          || resource.transport().equals(TRANSPORT.POLLING)) {

        // Cache connectios by session id and transport to allow reconnections
        // of the same user with different transport
        String key = httpSession.getId() + ":LP";

        if (!CONNECTIONS.containsKey(key))
          CONNECTIONS.put(key,
              new AtmosphereConnection(httpSession.getId(),
              loggedInUser, provider));

        AtmosphereConnection connection = (AtmosphereConnection) CONNECTIONS.get(key);

        // A connection exists for a different participant:
        // This happens when different users shares the same browser session.
        // Ensure that connections are cleaned up.
        if (!connection.getParticipantId().equals(loggedInUser)) {
          connection = new AtmosphereConnection(httpSession.getId(), loggedInUser, provider);
          CONNECTIONS.put(key, connection);
        }


        if (resource.getRequest().getMethod().equalsIgnoreCase("GET")) {

          // LOG.info("Long-polling suspending request " + resource.uuid());

          resource.suspend();
          // In long polling we bind any new resource to the channel
          connection.getAtmosphereChannel().bindResource(resource);


          if (needClientUpgrade) {
            connection.getAtmosphereChannel().onClientNeedUpgrade();
            connection.getAtmosphereChannel().unbindResource();
            return;
          }

        } else if (resource.getRequest().getMethod().equalsIgnoreCase("POST")) {

          // LOG.info("Long-polling proccessing request " + resource.uuid());

          StringBuilder b = IOUtils.readEntirely(resource);
          try {
            connection.getAtmosphereChannel().onMessage(b.toString());
            resource.resume();
          } catch (RuntimeException e) {
            LOG.info("Exception on channel.", e);
          }

        }

      }



    }

    /**
     * Packing messages ensures that a Wave message order is preserved when
     * server is delivering messages through atmosphere long-polling. I wasn't
     * able to avoid this forcing the flush/resuming of atmosphere's output
     * stream.
     *
     * @param messages
     * @return
     * @throws UnsupportedEncodingException
     */
    private byte[] packWaveMessages(List<Object> messages) throws UnsupportedEncodingException {

      StringBuilder sb = new StringBuilder();

      sb.append(SEPARATOR);
      for (Object obj : messages) {
        LOG.fine("Sending Wave message: " + (String) obj);
        sb.append((String) obj).append(SEPARATOR);
      }

      return sb.toString().getBytes(CHARSET);
    }

    @Override
    public void onStateChange(AtmosphereResourceEvent event) throws IOException {

      AtmosphereResponse response = event.getResource().getResponse();
      AtmosphereResource resource = event.getResource();

      HttpSession httpSession = getSession(resource);
      ParticipantId loggedInUser = provider.sessionManager.getLoggedInUser(httpSession);

      if (event.isSuspended()) {

        LOG.fine("Atmosphere response for suspended resource " + resource.uuid()
            + " response is "
            + event.getMessage() != null ? "not empty" : "EMPTY!");

        // Set content type before do response.getWriter()
        // http://docs.oracle.com/javaee/5/api/javax/servlet/ServletResponse.html#setContentType(java.lang.String)
        response.setContentType("text/plain; charset=UTF-8");
        response.setCharacterEncoding("UTF-8");

        if (event.getMessage().getClass().isArray()) {

          List<Object> list = Arrays.asList(event.getMessage());
          response.getOutputStream().write(packWaveMessages(list));

        } else if (event.getMessage() instanceof List) {

          @SuppressWarnings("unchecked")
          List<Object> list = List.class.cast(event.getMessage());
          response.getOutputStream().write(packWaveMessages(list));

        } else if (event.getMessage() instanceof String) {

          String message = (String) event.getMessage();
          response.getOutputStream().write(message.getBytes(CHARSET));
        }


        flushResponse(resource);


      } else if (event.isCancelled()) {

        LOG.info("Resource cancelled by remote client: " + event.getResource().uuid() + " / "
            + loggedInUser.getAddress());

        AtmosphereConnection connection =
            (AtmosphereConnection) CONNECTIONS.get(httpSession.getId());

        if (connection != null) connection.getAtmosphereChannel().unbindResource();

      } else if (event.isResumedOnTimeout()) {

        LOG.fine("Resource resumed on timeout: " + event.getResource().uuid());

      } else if (event.isResuming()) {

        LOG.fine("Resource resumed: " + event.getResource().uuid());

      } else if (event.isClosedByApplication() || event.isClosedByClient()) {

        LOG.info("Resource closed " + ": " + event.getResource().uuid() + " / "
            + loggedInUser.getAddress());

        AtmosphereConnection connection =
            (AtmosphereConnection) CONNECTIONS.get(httpSession.getId());

        if (connection != null) connection.getAtmosphereChannel().unbindResource();

      } else if (event.isResuming()) {

        LOG.info("Resuming resource " + event.getResource().uuid() + " / "
            + loggedInUser.getAddress());

      } else if (event.isResumedOnTimeout()) {

        LOG.info("Resuming resource in Timeout " + event.getResource().uuid() + " / "
            + loggedInUser.getAddress());

      } else {

        LOG.info("Unknown atmosphere event for resource " + event.getResource().uuid() + " / "
            + loggedInUser.getAddress());
      }

    }

    @Override
    public void destroy() {
      // Nothing to do

    }


  }

  /**
   * Returns the socket the WebSocket server is listening on.
   */
  public SocketAddress getWebSocketAddress() {
    if (httpServer == null) {
      return null;
    } else {
      ServerConnector c = (ServerConnector)httpServer.getConnectors()[0];
      return new InetSocketAddress(c.getHost(), c.getLocalPort());
    }
  }

  /**
   * Stops this server.
   */
  public void stopServer() throws IOException {
    try {
      httpServer.stop(); // yes, .stop() throws "Exception"
    } catch (Exception e) {
      LOG.warning("Fatal error stopping http server.", e);
    }
    LOG.fine("server shutdown.");
  }

  /**
   * Register all methods provided by the given service type.
   */
  public void registerService(Service service) {
    synchronized (registeredServices) {
      for (MethodDescriptor methodDescriptor : service.getDescriptorForType().getMethods()) {
        registeredServices.put(methodDescriptor.getInputType(),
            new RegisteredServiceMethod(service, methodDescriptor));
      }
    }
  }

  /**
   * List of servlets
   */
  List<Pair<String, ServletHolder>> servletRegistry = Lists.newArrayList();

  /**
   * List of filters
   */
  List<Pair<String, Class<? extends Filter>>> filterRegistry = Lists.newArrayList();

  /**
   * Add a servlet to the servlet registry. This servlet will be attached to the
   * specified URL pattern when the server is started up.
   *
   * @param urlPattern the URL pattern for paths. Eg, '/foo', '/foo/*'.
   * @param servlet the servlet class to bind to the specified paths.
   * @param initParams the map with init params, can be null or empty.
   * @return the {@link ServletHolder} that holds the servlet.
   */
  public ServletHolder addServlet(String urlPattern, Class<? extends HttpServlet> servlet,
      @Nullable Map<String, String> initParams) {
    ServletHolder servletHolder = new ServletHolder(servlet);
    if (initParams != null) {
      servletHolder.setInitParameters(initParams);
    }
    servletRegistry.add(new Pair<String, ServletHolder>(urlPattern, servletHolder));
    return servletHolder;
  }

  /**
   * Add a servlet to the servlet registry. This servlet will be attached to the
   * specified URL pattern when the server is started up.
   * @param urlPattern the URL pattern for paths. Eg, '/foo', '/foo/*'.
   * @param servlet the servlet class to bind to the specified paths.
   * @return the {@link ServletHolder} that holds the servlet.
   */
  public ServletHolder addServlet(String urlPattern, Class<? extends HttpServlet> servlet) {
    return addServlet(urlPattern, servlet, null);
  }

  /**
   * Add a filter to the filter registry. This filter will be attached to the
   * specified URL pattern when the server is started up.
   *
   * @param urlPattern the URL pattern for paths. Eg, '/foo', '/foo/*'.
   *
   */
  public void addFilter(String urlPattern, Class<? extends Filter> filter) {
    filterRegistry.add(new Pair<String, Class<? extends Filter>>(urlPattern, filter));
  }
}
