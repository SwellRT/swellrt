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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.lang.StringUtils;
import org.atmosphere.cache.UUIDBroadcasterCache;
import org.atmosphere.config.service.AtmosphereHandlerService;
import org.atmosphere.cpr.AtmosphereHandler;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResource.TRANSPORT;
import org.atmosphere.cpr.AtmosphereResourceEvent;
import org.atmosphere.cpr.Broadcaster;
import org.atmosphere.cpr.BroadcasterFactory;
import org.atmosphere.guice.AtmosphereGuiceServlet;
import org.eclipse.jetty.proxy.ProxyServlet;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
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
import org.waveprotocol.box.server.authentication.HttpWindowSession;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.ClientServerExecutor;
import org.waveprotocol.box.server.persistence.file.FileUtils;
import org.waveprotocol.box.server.rpc.atmosphere.AtmosphereChannel;
import org.waveprotocol.box.server.rpc.atmosphere.AtmosphereClientInterceptor;
import org.waveprotocol.box.server.rpc.atmosphere.AtmosphereUtil;
import org.waveprotocol.box.server.util.NetUtils;
import org.waveprotocol.box.stat.Timer;
import org.waveprotocol.box.stat.Timing;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.util.logging.Log;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.servlet.GuiceFilter;
import com.google.inject.servlet.GuiceServletContextListener;
import com.google.inject.servlet.ServletModule;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.MethodDescriptor;
import com.google.protobuf.Message;
import com.google.protobuf.RpcCallback;
import com.google.protobuf.Service;
import com.typesafe.config.Config;

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



  private static ConcurrentHashMap<HttpWindowSession, Connection> ATMOSPHERE_CONNECTIONS =
      new ConcurrentHashMap<HttpWindowSession, Connection>();

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
  private final int sessionCookieMaxAge;

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
   * The glue class between the Atmoshphere Framework (communication layer) and
   * the RPCs for each remote client.
   *
   * A connection is associated with an unique pair (HTTP session, browser
   * window/tab), the {@link HttpWindowSession}
   *
   * This implementation supports WebSockets and Long-Polling transports over
   * Atmosphere.
   *
   *
   * @author pablojan@gmail.com (Pablo Ojanguren)
   *
   */
  static class AtmosphereConnection extends Connection {


    private final AtmosphereChannel atmosphereChannel;

    private HttpWindowSession id;
    private Broadcaster broadcaster;

    private TRANSPORT transport;

    public AtmosphereConnection(HttpWindowSession id,
        ServerRpcProvider provider) {

      super(null, provider);

      this.id = id;
      this.broadcaster = BroadcasterFactory.getDefault().get(this.id.toString());

      atmosphereChannel = new AtmosphereChannel(this, broadcaster);
      expectMessages(atmosphereChannel);


    }

    /**
     * Handle an incoming request through an {@link AtmosphereResource}.
     *
     * @param resource the resource with the incoming message or new connection
     *        signal
     */
    public void onRequest(AtmosphereResource resource, ParticipantId loggedInUser) {

      // Update current transport
      this.transport = resource.transport();


      if (isWebSocket(this.transport)) {


        if (isGET(resource)) {
          // New connection
          resource.setBroadcaster(broadcaster);
          resource.suspend();
        }


        if (isPOST(resource)) {
          String data = AtmosphereUtil.readMessage(resource);
          atmosphereChannel.handleMessageString(data, loggedInUser);
        }


      }


      /*
       * Responses are always sent back through the GET request (resource). POST
       * requests are released as soon as the request has been delivered
       */
      if (isLongPolling(this.transport)) {

        if (isGET(resource)) {
          // New connection
          resource.suspend();
          broadcaster.addAtmosphereResource(resource);
        }


        if (isPOST(resource)) {
          String data = AtmosphereUtil.readMessage(resource);
          atmosphereChannel.handleMessageString(data, loggedInUser);
          resource.resume();
        }
      }

    }


    /**
     * Closes the connection and its RPCs. This method must be invoked when
     * client closes the communication.
     */
    public void close() {
      this.cancelRpcs();
      for (AtmosphereResource r : this.broadcaster.getAtmosphereResources()) {
        try {
          r.close();
        } catch (IOException e) {
          LOG.warning("Error closing resource on session " + id, e);
        }
      }
      this.broadcaster.destroy();
    }

    //
    // Send back
    //


    @Override
    protected void sendMessage(int sequenceNo, Message message) {
      atmosphereChannel.sendMessage(sequenceNo, message);
      if (isLongPolling(transport)) {
        broadcaster.resumeAll();
      }
    }

    //
    // Utility methods
    //

    protected static boolean isWebSocket(TRANSPORT transport) {
      return transport.equals(TRANSPORT.WEBSOCKET);
    }

    protected static boolean isLongPolling(TRANSPORT transport) {
      return transport.equals(TRANSPORT.LONG_POLLING) || transport.equals(TRANSPORT.POLLING);
    }


    protected static boolean isGET(AtmosphereResource r) {
      return r.getRequest().getMethod().equalsIgnoreCase("GET");
    }

    protected static boolean isPOST(AtmosphereResource r) {
      return r.getRequest().getMethod().equalsIgnoreCase("POST");
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
     * @param provider the provider
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

    protected void cancelRpcs() {
      if (!activeRpcs.isEmpty()) {
        LOG.info("Cleaning up RPCs");
        for (ServerRpcController controller : activeRpcs.values()) {
          if (!controller.isCanceled()) {
            controller.cancel();
          }
        }
        activeRpcs.clear();
      }
    }

    @Override
    public void message(final int sequenceNo, Message message, ParticipantId loggedInUser) {
      final String messageName = "/" + message.getClass().getSimpleName();
      final Timer profilingTimer = Timing.startRequest(messageName);

      if (isFirstRequest && sequenceNo != 0) {
        // On server's reboot, clients can get reconnected (sequenceNo !=0) but
        // server won't be able to process the request.
        LOG.info("First RPC request but sequence number >0");
        throw new IllegalStateException("First RPC request nut sequence number >0");
      }

      isFirstRequest = false;

      // Clean up ServerRpcControllers if remote client starts a new
      // sequence of protocol messages.
      // This allows dirty reconnections, resuming current sessions...
      // This should been handle before, detecting new logins for the same
      if (sequenceNo == 0) {

        // Don't update on null to keep backwards compat.
        if (loggedInUser != null) this.loggedInUser = loggedInUser;
        cancelRpcs();
      } else {
        if (!this.loggedInUser.equals(loggedInUser)) {
          LOG.info("Request's participant doesn't match connection's participant. Spoofing?");
          throw new IllegalStateException(
              "Request's participant doesn't match connection's participant.");
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
                        boolean failed = message instanceof Rpc.RpcFinished && ((Rpc.RpcFinished) message).getFailed();
                        if (failed) {
                          LOG.fine("error = " + ((Rpc.RpcFinished) message).getErrorText());
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
      int sessionMaxInactiveTime, int sessionCookieMaxAge) {
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
    this.sessionCookieMaxAge = sessionCookieMaxAge;
  }

  /**
   * Constructs a new ServerRpcProvider with a default ExecutorService.
   */
  public ServerRpcProvider(InetSocketAddress[] httpAddresses,
      String[] resourceBases, SessionManager sessionManager,
      org.eclipse.jetty.server.SessionManager jettySessionManager, String sessionStoreDir,
      boolean sslEnabled, String sslKeystorePath, String sslKeystorePassword,
      Executor executor, int webSocketMaxIdleTime, int webSocketMaxMessageSize, int websocketHeartbeat,
      int sessionMaxInactiveTime, int sessionCookieMaxAge) {
    this(httpAddresses, resourceBases, executor,
        sessionManager, jettySessionManager, sessionStoreDir, sslEnabled, sslKeystorePath,
        sslKeystorePassword, webSocketMaxIdleTime, webSocketMaxMessageSize, websocketHeartbeat,
        sessionMaxInactiveTime, sessionCookieMaxAge);
  }

  @Inject
  public ServerRpcProvider(Config config,
                           SessionManager sessionManager, org.eclipse.jetty.server.SessionManager jettySessionManager,
                           @ClientServerExecutor Executor executorService) {
    this(parseAddressList(config.getStringList("core.http_frontend_addresses"),
                    config.getString("core.http_websocket_public_address")),
            config.getStringList("core.resource_bases").toArray(new String[0]),
            sessionManager,
            jettySessionManager,
            config.getString("core.sessions_store_directory"),
            config.getBoolean("security.enable_ssl"),
            config.getString("security.ssl_keystore_path"),
            config.getString("security.ssl_keystore_password"),
            executorService,
            config.getInt("network.websocket_max_idle_time"),
            config.getInt("network.websocket_max_message_size"),
            config.getInt("network.websocket_heartbeat"),
            config.getInt("network.session_max_inactive_time"),
            config.getInt("network.session_cookie_max_age")
            );
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
    	
		jettySessionManager.addEventListener(new HttpSessionListener() {
		
			@Override
			public void sessionCreated(HttpSessionEvent arg0) {
				LOG.info("Session created, id=" + arg0.getSession().getId() + " t=" + System.currentTimeMillis());
				if (sessionMaxInactiveTime == 0)
					arg0.getSession().setMaxInactiveInterval(Integer.MAX_VALUE);
			      else
			    	arg0.getSession().setMaxInactiveInterval(sessionMaxInactiveTime);
			}
		
			@Override
			public void sessionDestroyed(HttpSessionEvent arg0) {
				LOG.info("Session destroyed, id=" + arg0.getSession().getId() + " t=" + System.currentTimeMillis());
			}
		
		});
      
      context.getSessionHandler().setSessionManager(jettySessionManager);
      
      Set<SessionTrackingMode> sessionTrackingModes = new HashSet<SessionTrackingMode>();
      sessionTrackingModes.add(SessionTrackingMode.URL);
      sessionTrackingModes.add(SessionTrackingMode.COOKIE);
      context.getSessionHandler().getSessionManager().setSessionTrackingModes(sessionTrackingModes);
      context.getSessionHandler().getSessionManager()
          .setSessionIdPathParameterName(SessionManager.SESSION_URL_PARAM);

    }
    final ResourceCollection resources = new ResourceCollection(resourceBases);
    context.setBaseResource(resources);

    context.setInitParameter("org.eclipse.jetty.servlet.SessionCookie", SessionManager.SESSION_COOKIE_NAME);
    if (sessionCookieMaxAge == 0)
    	context.setInitParameter("org.eclipse.jetty.servlet.MaxAge", String.valueOf(Integer.MAX_VALUE));
    else
    	context.setInitParameter("org.eclipse.jetty.servlet.MaxAge", String.valueOf(sessionCookieMaxAge));
    FilterHolder corsFilterHolder = new FilterHolder(CrossOriginFilter.class);
    corsFilterHolder.setInitParameter(CrossOriginFilter.ALLOWED_ORIGINS_PARAM, "*");
    // Set explicit methods to allow CORS with DELETE
    corsFilterHolder.setInitParameter(CrossOriginFilter.ALLOWED_METHODS_PARAM, "GET,POST,DELETE,PUT,HEAD");
    corsFilterHolder.setInitParameter(CrossOriginFilter.ALLOWED_HEADERS_PARAM,
        "X-Requested-With,Content-Type,Accept,Origin,X-window-id");
    context.addFilter(corsFilterHolder, "/*", EnumSet.allOf(DispatcherType.class));

    // Set cache header rewriter filter
    FilterHolder cacheFilterHolder = new FilterHolder(CacheHeaderFilter.class);
    context.addFilter(cacheFilterHolder, "/*", EnumSet.allOf(DispatcherType.class));

    addWebSocketServlets();

    try {

      final ServletModule servletModule = getServletModule();

      ServletContextListener contextListener = new GuiceServletContextListener() {

        private final Injector childInjector = injector.createChildInjector(servletModule);

        @Override
        protected Injector getInjector() {
          return childInjector;
        }
      };

      context.addEventListener(contextListener);
      context.addFilter(GuiceFilter.class, "/*", EnumSet.allOf(DispatcherType.class));
      context.addFilter(GzipFilter.class, "/webclient/*", EnumSet.allOf(DispatcherType.class));
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

    //
    // Atmosphere framework. Replacement of Socket.IO
    // See https://issues.apache.org/jira/browse/WAVE-405
    //
    ServletHolder atholder = addServlet("/atmosphere*", AtmosphereGuiceServlet.class);

    // Avoid loading defualt CORS interceptor which is in conflict with general
    // jetty CORS filter
    atholder.setInitParameter("org.atmosphere.cpr.AtmosphereInterceptor.disableDefaults", "true");

    // Setting a low HeartBeat frequency to avoid network issues. Let clients to
    // set a hihger value.
    atholder.setInitParameter(
        "org.atmosphere.interceptor.HeartbeatInterceptor.heartbeatFrequencyInSeconds", ""
            + websocketHeartbeat);

    // Set websocket max idle time
    if (webSocketMaxIdleTime == 0)
    	atholder.setInitParameter("org.atmosphere.websocket.maxIdleTime", String.valueOf(Integer.MAX_VALUE));
    else
    	atholder.setInitParameter("org.atmosphere.websocket.maxIdleTime", String.valueOf(webSocketMaxIdleTime));
    	

    //
    // Configure Atmosphere Broadcaster class
    //
    // Comment out following line to enable Threaded broadcasters
    atholder.setInitParameter("org.atmosphere.cpr.broadcasterClass",
        "org.atmosphere.util.SimpleBroadcaster");

    //
    // Configuring Atmosphere threads: this config is considered if
    // SimpleBroadcaster is not used
    //

    // Use same thread pool for all brodcaster (AtmosphereConnection)
    atholder.setInitParameter("org.atmosphere.cpr.broadcaster.shareableThreadPool", "true");
    // Set number of threads for atmosphere
    atholder.setInitParameter("org.atmosphere.cpr.broadcaster.maxProcessingThreads", "4");


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

    // Serve the static content and GWT web client with the default servlet
    // (acts like a standard file-based web server).
    // addServlet("/static/*", DefaultServlet.class);
    // addServlet("/webclient/*", DefaultServlet.class);
  }

  public ServletModule getServletModule() {

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
      ArrayList<String> mergedAddressList = new ArrayList<>(addressList);
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
      return addresses.toArray(new InetSocketAddress[addresses.size()]);
    }
  }

  /**
   * @return a list of {@link Connector} each bound to a host:port
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
    public WaveWebSocketServlet(ServerRpcProvider provider, Config config) {
      super();
      this.provider = provider;
      this.websocketMaxIdleTime = config.getInt("network.websocket_max_idle_time");
      this.websocketMaxMessageSize = config.getInt("network.websocket_max_message_size");
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
   * The Atmosphere handler. Dispatchs incoming requests from remote clients to
   * the right {@link AtmosphereConnection}.
   *
   * It handles transport-level events:
   *
   * <ul>
   * <li>Session expiration: close remote clients properly.</li>
   * <li>Server reboot refusing further operations. Close remote clientes
   * properly.</li>
   * <li>Allow reconnection of remote clients if HTTP session is still active
   * despite the transport failure.</li>
   * <li>Keep connections opened sending heartbeat signal</li>
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

    private static final String EXT_CLIENT_VERSION_HEADER = "X-client-version";

    /** Header name for extensions of the wave socket protocol */
    private static final String EXT_RESPONSE_HEADER = "X-RESPONSE:";

    private static final String EXT_RESPONSE_CLIENT_NOT_SUPPORTED = "CLIENT_NOT_SUPPORTED";
    private static final String EXT_RESPONSE_INVALID_SESSION = "INVALID_SESSION";
    private static final String EXT_RESPONSE_SERVER_EXCEPTION = "SERVER_EXCEPTION";

    @Inject
    public ServerRpcProvider provider;


    /**
     * Get the session for the Atmosphere resource based on the session token
     * passes as path part.
     * 
     * We ignore cookies explicity here. If request come from an iframe, the
     * websocket request can send parent's window cookie session causing a
     * mismatch with session id given in a login request.
     * 
     * @param resource
     * @return
     */
    private HttpSession getSession(AtmosphereResource resource) {

      /*
       * Cookie[] cookies = resource.getRequest().getCookies(); String
       * cookieSessionId = null; if (cookies != null) for (Cookie c : cookies) {
       * if (c.getName().equals(SESSION_COOKIE_NAME)) cookieSessionId =
       * c.getValue(); }
       */

      // Get the session URL path /atmosphere/<sessionId>:<windowId>
      String urlToken = null;

      int lastPathSeparatorIndex = resource.getRequest().getPathInfo().lastIndexOf("/");
      if (lastPathSeparatorIndex >= 0) {
        urlToken = resource.getRequest().getPathInfo().substring(lastPathSeparatorIndex + 1);
      }

      // Trust first in the session token passed as URL path.
      // Inside IFrames, we could get the wrong cookie!
      if (urlToken != null) {
        return provider.sessionManager.getSessionFromToken(urlToken);
      }


      return null;
    }


    @Override
    public void onRequest(AtmosphereResource resource) throws IOException {

      // Check if SwellRT client is compatible with server
      String clientVersionHeader = resource.getRequest().getHeader(EXT_CLIENT_VERSION_HEADER);

      if ((clientVersionHeader == null) || (!clientVersionHeader.equals(Model.MODEL_VERSION))) {
        AtmosphereUtil.writeMessage(resource, EXT_RESPONSE_HEADER
            + EXT_RESPONSE_CLIENT_NOT_SUPPORTED);
        resource.close();
        return;
      }

      // Atmosphere requires specialized HttpWindowSession instances. See
      // SessionServerImpl class.

      HttpSession httpSession = getSession(resource);
      HttpWindowSession windowSession = null;
      if (httpSession != null && httpSession instanceof HttpWindowSession)
        windowSession = (HttpWindowSession) httpSession;

      //
      if (windowSession == null) {
        AtmosphereUtil.writeMessage(resource, EXT_RESPONSE_HEADER + EXT_RESPONSE_INVALID_SESSION);
        resource.close();
        return;
      }


      ParticipantId participantId = provider.sessionManager.getLoggedInUser(httpSession);


      if (!ATMOSPHERE_CONNECTIONS.containsKey(windowSession)) {
        ATMOSPHERE_CONNECTIONS
            .put(windowSession, new AtmosphereConnection(windowSession, provider));
      }

      AtmosphereConnection connection =
          (AtmosphereConnection) ATMOSPHERE_CONNECTIONS.get(windowSession);

      try {
        connection.onRequest(resource, participantId);
      } catch (Exception e) {
        LOG.warning("Error handling request for session " + windowSession, e);
        AtmosphereUtil.writeMessage(resource, EXT_RESPONSE_HEADER + EXT_RESPONSE_SERVER_EXCEPTION);
        resource.close();
        return;
      }
    }



    @Override
    public void onStateChange(AtmosphereResourceEvent event) throws IOException {

      AtmosphereResource resource = event.getResource();
      HttpSession session = getSession(resource);

      if (event.isSuspended()) {
        AtmosphereUtil.writeMessage(resource, event.getMessage());

      } else if (event.isCancelled()) {
        LOG.info("Resource cancelled by remote client, session " + session);

      } else if (event.isResumedOnTimeout()) {
        LOG.fine("Resource resumed on timeout, session " + session);

      } else if (event.isClosedByClient()) {
        LOG.info("Resource closed by client, session " + session);

        AtmosphereConnection connection =
            (AtmosphereConnection) ATMOSPHERE_CONNECTIONS.get(session);

        if (connection != null) connection.close();

      } else if (event.isResumedOnTimeout()) {
        LOG.info("Resource resumed by timeout, session " + session);
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
    servletRegistry.add(Pair.of(urlPattern, servletHolder));
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

  /**
   * Add a transparent proxy to the servlet registry. The servlet will proxy to the
   * specified URL pattern.
   * For example: addTransparentProxy("/gadgets/*","http://gmodules:80/gadgets", "/gadgets")
   * @param urlPattern the URL pattern for paths. Eg, '/foo', '/foo/*'.
   * @param proxyTo the URL to proxy to.
   * @param prefix the prefix that should be proxied.
   */
  public void addTransparentProxy(String urlPattern, String proxyTo, String prefix) {
    Preconditions.checkNotNull(urlPattern);
    Preconditions.checkNotNull(proxyTo);
    Preconditions.checkNotNull(prefix);

    ServletHolder proxy = new ServletHolder(ProxyServlet.Transparent.class);
    proxy.setInitParameter("proxyTo", proxyTo);
    proxy.setInitParameter("prefix", prefix);
    servletRegistry.add(Pair.of(urlPattern, proxy));
  }
}
