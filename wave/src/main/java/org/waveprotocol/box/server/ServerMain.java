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

package org.waveprotocol.box.server;

import java.io.File;
import java.util.Collection;

import org.apache.commons.configuration.ConfigurationException;
import org.swellrt.server.box.events.DeltaBasedEventSource;
import org.swellrt.server.box.events.EventDispatcher;
import org.swellrt.server.box.events.EventRule;
import org.swellrt.server.box.events.EventsModule;
import org.swellrt.server.box.events.dummy.DummyDispatcher;
import org.swellrt.server.box.events.gcm.GCMDispatcher;
import org.swellrt.server.box.events.http.HttpDispatcher;
import org.swellrt.server.box.index.ModelIndexerDispatcher;
import org.swellrt.server.box.index.ModelIndexerModule;
import org.swellrt.server.box.servlet.EmailModule;
import org.swellrt.server.box.servlet.SwellRtServlet;
import org.waveprotocol.box.common.comms.WaveClientRpc.ProtocolWaveClientRpc;
import org.waveprotocol.box.server.authentication.AccountStoreHolder;
import org.waveprotocol.box.server.authentication.SessionManager;
import org.waveprotocol.box.server.executor.ExecutorsModule;
import org.waveprotocol.box.server.frontend.ClientFrontend;
import org.waveprotocol.box.server.frontend.WaveClientRpcImpl;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.PersistenceModule;
import org.waveprotocol.box.server.persistence.SignerInfoStore;
import org.waveprotocol.box.server.robots.agent.passwd.PasswordAdminRobot;
import org.waveprotocol.box.server.robots.agent.passwd.PasswordRobot;
import org.waveprotocol.box.server.robots.agent.registration.RegistrationRobot;
import org.waveprotocol.box.server.robots.agent.welcome.WelcomeRobot;
import org.waveprotocol.box.server.robots.passive.RobotsGateway;
import org.waveprotocol.box.server.rpc.AttachmentInfoServlet;
import org.waveprotocol.box.server.rpc.AttachmentServlet;
import org.waveprotocol.box.server.rpc.AuthenticationServlet;
import org.waveprotocol.box.server.rpc.WindowIdFilter;
import org.waveprotocol.box.server.rpc.ServerRpcProvider;
import org.waveprotocol.box.server.shutdown.ShutdownManager;
import org.waveprotocol.box.server.shutdown.ShutdownPriority;
import org.waveprotocol.box.server.shutdown.Shutdownable;
import org.waveprotocol.box.server.stat.RequestScopeFilter;
import org.waveprotocol.box.server.stat.StatuszServlet;
import org.waveprotocol.box.server.stat.TimingFilter;
import org.waveprotocol.box.server.waveserver.PerUserWaveViewBus;
import org.waveprotocol.box.server.waveserver.PerUserWaveViewDistpatcher;
import org.waveprotocol.box.server.waveserver.WaveBus;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.box.stat.StatService;
import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.federation.FederationTransport;
import org.waveprotocol.wave.federation.noop.NoOpFederationModule;
import org.waveprotocol.wave.model.wave.ParticipantIdUtil;
import org.waveprotocol.wave.util.logging.Log;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * Wave Server entrypoint.
 */
public class ServerMain {

  private static final Log LOG = Log.get(ServerMain.class);

  public static void main(String... args) {
    try {
      Module coreSettings = new AbstractModule() {

        @Override
        protected void configure() {
          Config config =
              ConfigFactory.load().withFallback(
                  ConfigFactory.parseFile(new File("config/application.conf")).withFallback(
                      ConfigFactory.parseFile(new File("config/reference.conf"))));
          bind(Config.class).toInstance(config);
          bind(Key.get(String.class, Names.named(CoreSettingsNames.WAVE_SERVER_DOMAIN)))
              .toInstance(config.getString("core.wave_server_domain"));
        }
      };
      run(coreSettings);
    } catch (PersistenceException e) {
      LOG.severe("PersistenceException when running server:", e);
    } catch (ConfigurationException e) {
      LOG.severe("ConfigurationException when running server:", e);
    } catch (WaveServerException e) {
      LOG.severe("WaveServerException when running server:", e);
    }
  }

  public static void run(Module coreSettings) throws PersistenceException,
      ConfigurationException, WaveServerException {
    Injector injector = Guice.createInjector(coreSettings);
    Module profilingModule = injector.getInstance(StatModule.class);
    ExecutorsModule executorsModule = injector.getInstance(ExecutorsModule.class);
    injector = injector.createChildInjector(profilingModule, executorsModule);

    Config config = injector.getInstance(Config.class);

    Module serverModule = injector.getInstance(ServerModule.class);
    Module federationModule = buildFederationModule(injector);
    // Module robotApiModule = new RobotApiModule();
    PersistenceModule persistenceModule = injector.getInstance(PersistenceModule.class);
    // Module searchModule = injector.getInstance(SearchModule.class);
    Module modelIndexerModule = injector.getInstance(ModelIndexerModule.class); // SwellRT
    Module eventsModule = injector.getInstance(EventsModule.class); // SwellRT
    // Module profileFetcherModule = injector.getInstance(ProfileFetcherModule.class);
    Module emailModule = injector.getInstance(EmailModule.class); // SwellRT
    // injector = injector.createChildInjector(serverModule, persistenceModule, robotApiModule,
    //    federationModule, searchModule, profileFetcherModule);
    injector = injector.createChildInjector(serverModule, persistenceModule, federationModule, eventsModule, modelIndexerModule, emailModule);
    
    ServerRpcProvider server = injector.getInstance(ServerRpcProvider.class);
    WaveBus waveBus = injector.getInstance(WaveBus.class);

    String domain = config.getString("core.wave_server_domain");
    if (!ParticipantIdUtil.isDomainAddress(ParticipantIdUtil.makeDomainAddress(domain))) {
      throw new WaveServerException("Invalid wave domain: " + domain);
    }

    initializeServer(injector, domain);
    initializeServlets(server, config);
    // initializeRobotAgents(server);
    // initializeRobots(injector, waveBus);
    initializeFrontend(injector, server);
    initializeFederation(injector);
    // initializeSearch(injector, waveBus);
    initializeShutdownHandler(server);
	initializeSwellRt(injector, waveBus);

    LOG.info("Starting server");
    server.startWebSocketServer(injector);
  }

  private static Module buildFederationModule(Injector settingsInjector)
      throws ConfigurationException {
    return settingsInjector.getInstance(NoOpFederationModule.class);
  }

  private static void initializeServer(Injector injector, String waveDomain)
      throws PersistenceException, WaveServerException {
    AccountStore accountStore = injector.getInstance(AccountStore.class);
    accountStore.initializeAccountStore();
    AccountStoreHolder.init(accountStore, waveDomain);

    // Initialize the SignerInfoStore.
    CertPathStore certPathStore = injector.getInstance(CertPathStore.class);
    if (certPathStore instanceof SignerInfoStore) {
      ((SignerInfoStore)certPathStore).initializeSignerInfoStore();
    }

    // Initialize the server.
    WaveletProvider waveServer = injector.getInstance(WaveletProvider.class);
    waveServer.initialize();
  }

  private static void initializeServlets(ServerRpcProvider server, Config config) {
    // server.addServlet("/gadget/gadgetlist", GadgetProviderServlet.class);

    server.addServlet(AttachmentServlet.ATTACHMENT_URL + "/*", AttachmentServlet.class);
    server.addServlet(AttachmentServlet.THUMBNAIL_URL + "/*", AttachmentServlet.class);
    server.addServlet(AttachmentInfoServlet.ATTACHMENTS_INFO_URL, AttachmentInfoServlet.class);

    server.addServlet(SessionManager.SIGN_IN_URL, AuthenticationServlet.class);
    // server.addServlet("/auth/signout*", SignOutServlet.class);
    // server.addServlet("/auth/register*", UserRegistrationServlet.class);

    // server.addServlet("/locale/*", LocaleServlet.class);
    // server.addServlet("/fetch/*", FetchServlet.class);
    // server.addServlet("/search/*", SearchServlet.class);
    // server.addServlet("/notification/*", NotificationServlet.class);

    // server.addServlet("/robot/dataapi", DataApiServlet.class);
    // server.addServlet(DataApiOAuthServlet.DATA_API_OAUTH_PATH + "/*",
    // DataApiOAuthServlet.class);
    // server.addServlet("/robot/dataapi/rpc", DataApiServlet.class);
    // server.addServlet("/robot/register/*", RobotRegistrationServlet.class);
    // server.addServlet("/robot/rpc", ActiveApiServlet.class);
    // server.addServlet("/webclient/remote_logging",
    // RemoteLoggingServiceImpl.class);
    // server.addServlet("/profile/*", FetchProfilesServlet.class);
    // server.addServlet("/waveref/*", WaveRefServlet.class);

	//    String gadgetServerHostname = config.getString("core.gadget_server_hostname");
	//    int gadgetServerPort = config.getInt("core.gadget_server_port");
	//    LOG.info("Starting GadgetProxyServlet for " + gadgetServerHostname + ":" + gadgetServerPort);
	//    server.addTransparentProxy("/gadgets/*",
	//        "http://" + gadgetServerHostname + ":" + gadgetServerPort + "/gadgets", "/gadgets");
	//
	//    server.addServlet("/", WaveClientServlet.class);

    // Profiling
    server.addFilter("/*", RequestScopeFilter.class);
    boolean enableProfiling = config.getBoolean("core.enable_profiling");
    if (enableProfiling) {
      server.addFilter("/*", TimingFilter.class);
      server.addServlet(StatService.STAT_URL, StatuszServlet.class);
    }

    // DSWG experimental
    // server.addServlet("/shared/*", DSFileServlet.class);

    // SwellRt
    server.addServlet("/swell/*", SwellRtServlet.class);
  }

  private static void initializeRobots(Injector injector, WaveBus waveBus) {
    RobotsGateway robotsGateway = injector.getInstance(RobotsGateway.class);
    waveBus.subscribe(robotsGateway);
  }

  private static void initializeRobotAgents(ServerRpcProvider server) {
    server.addServlet(PasswordRobot.ROBOT_URI + "/*", PasswordRobot.class);
    server.addServlet(PasswordAdminRobot.ROBOT_URI + "/*", PasswordAdminRobot.class);
    server.addServlet(WelcomeRobot.ROBOT_URI + "/*", WelcomeRobot.class);
    server.addServlet(RegistrationRobot.ROBOT_URI + "/*", RegistrationRobot.class);
  }

  private static void initializeFrontend(Injector injector, ServerRpcProvider server) throws WaveServerException {
    ClientFrontend frontend = injector.getInstance(ClientFrontend.class);
    ProtocolWaveClientRpc.Interface rpcImpl = WaveClientRpcImpl.create(frontend, false);
    server.registerService(ProtocolWaveClientRpc.newReflectiveService(rpcImpl));
  }

  private static void initializeFederation(Injector injector) {
    FederationTransport federationManager = injector.getInstance(FederationTransport.class);
    federationManager.startFederation();
  }

  private static void initializeSearch(Injector injector, WaveBus waveBus)
      throws WaveServerException {
    PerUserWaveViewDistpatcher waveViewDistpatcher =
        injector.getInstance(PerUserWaveViewDistpatcher.class);
    PerUserWaveViewBus.Listener listener = injector.getInstance(PerUserWaveViewBus.Listener.class);
    waveViewDistpatcher.addListener(listener);
    waveBus.subscribe(waveViewDistpatcher);

    // WaveIndexer waveIndexer = injector.getInstance(WaveIndexer.class);
    // waveIndexer.remakeIndex();
  }

  private static void initializeShutdownHandler(final ServerRpcProvider server) {
    ShutdownManager.getInstance().register(new Shutdownable() {

      @Override
      public void shutdown() throws Exception {
        server.stopServer();
      }
    }, ServerMain.class.getSimpleName(), ShutdownPriority.Server);
  }

  private static void initializeSwellRt(Injector injector, WaveBus waveBus) {

    // Initialize Indexer

    ModelIndexerDispatcher indexerDispatcher =
        injector.getInstance(ModelIndexerDispatcher.class);

//    try {
//      indexerDispatcher.initialize();
//    } catch (WaveServerException e) {
//      LOG.warning("Error initializating SwellRtIndexerDispatcher", e);
    // }
    waveBus.subscribe(indexerDispatcher);
    
    // Initialize Events
    // TODO get rules as inject. dependency
    Collection<EventRule> rules =
            EventRule.fromFile(System.getProperty("event-rules.config.file", "config/event-rules.config"));
    
    EventDispatcher eventDispatcher = injector.getInstance(EventDispatcher.class);
    eventDispatcher.setRules(rules);
    eventDispatcher.subscribe(injector.getInstance(DummyDispatcher.class), DummyDispatcher.NAME);
    eventDispatcher.subscribe(injector.getInstance(GCMDispatcher.class), GCMDispatcher.NAME);
    eventDispatcher.subscribe(injector.getInstance(HttpDispatcher.class), HttpDispatcher.NAME);

    
    DeltaBasedEventSource eventSource = injector.getInstance(DeltaBasedEventSource.class);
    waveBus.subscribe(eventSource);
  }

}
