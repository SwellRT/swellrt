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

package org.waveprotocol.box.server.robots.agent;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import com.google.wave.api.AbstractRobot;

import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.persistence.AccountStore;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.robots.register.RobotRegistrar;
import org.waveprotocol.box.server.robots.util.RobotsUtil.RobotRegistrationException;
import org.waveprotocol.wave.model.id.TokenGenerator;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The base for robot agents that run on the WIAB server.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
@SuppressWarnings("serial")
public abstract class AbstractBaseRobotAgent extends AbstractRobot {

  public static class ServerFrontendAddressHolder {

    private final List<String> addresses;

    @Inject
    ServerFrontendAddressHolder(
        @Named(CoreSettings.HTTP_FRONTEND_ADDRESSES) List<String> addresses) {
      this.addresses = addresses;
    }

    public List<String> getAddresses() {
      return addresses;
    }
  }

  public static final String AGENT_PREFIX_URI = "/agent";
  private static final Logger LOG = Logger.getLogger(AbstractBaseRobotAgent.class.getName());

  /** The wave server domain. */
  private final String waveDomain;

  /** Account store with user and robot accounts. */
  private final AccountStore accountStore;

  /** SSL enabled flag? */
  private final Boolean isSSLEnabled;

  /** The robot registrar. */
  private final RobotRegistrar robotRegistrar;

  private final ServerFrontendAddressHolder frontendAddressHolder;

  /**
   * Constructor. Initializes the agent to serve on the URI provided by
   * {@link #getRobotUri()} and ensures that the agent is registered in the
   * Account store.
   *
   * @param injector the injector instance.
   */
  public AbstractBaseRobotAgent(Injector injector) {
    this(injector.getInstance(Key.get(String.class, Names.named(CoreSettings.WAVE_SERVER_DOMAIN))),
        injector.getInstance(TokenGenerator.class), injector
            .getInstance(ServerFrontendAddressHolder.class), injector
            .getInstance(AccountStore.class), injector.getInstance(RobotRegistrar.class),
        injector.getInstance(Key.get(Boolean.class, Names.named(CoreSettings.ENABLE_SSL))));
  }

  /**
   * Constructor. Initializes the agent to serve on the URI provided by
   * {@link #getRobotUri()} and ensures that the agent is registered in the
   * Account store.
   */
  AbstractBaseRobotAgent(String waveDomain, TokenGenerator tokenGenerator,
      ServerFrontendAddressHolder frontendAddressHolder, AccountStore accountStore,
      RobotRegistrar robotRegistator, Boolean sslEnabled) {
    this.waveDomain = waveDomain;
    this.frontendAddressHolder = frontendAddressHolder;
    this.robotRegistrar = robotRegistator;
    this.accountStore = accountStore;
    this.isSSLEnabled = sslEnabled;
    ensureRegistered(tokenGenerator, getFrontEndAddress());
  }

  /**
   * Ensures that the robot agent is registered in the {@link AccountStore}.
   */
  private void ensureRegistered(TokenGenerator tokenGenerator, String serverFrontendAddress) {
    ParticipantId robotId = null;
    try {
      robotId = ParticipantId.of(getRobotId() + "@" + waveDomain);
    } catch (InvalidParticipantAddress e) {
      LOG.log(Level.SEVERE, "Failed to register the agent:" + getRobotId(), e);
      return;
    }
    try {
      String location = serverFrontendAddress + getRobotUri();
      // In order to re-register the agents if the server frontend address has changed.
      robotRegistrar.registerOrUpdate(robotId, location);

    } catch (RobotRegistrationException e) {
      LOG.log(Level.SEVERE, "Failed to register the agent:" + getRobotId(), e);
    } catch (PersistenceException e) {
      LOG.log(Level.SEVERE, "Failed to register the agent:" + getRobotId(), e);
    }
  }

  @Override
  protected String getRobotProfilePageUrl() {
    return null;
  }

  /**
   * Returns the wave domain.
   */
  public String getWaveDomain() {
    return waveDomain;
  }

  /**
   * Returns the front end address with correct prefix.
   */
  public String getFrontEndAddress() {
    //TODO(alown): should this really only get the first one?
    return (this.isSSLEnabled ? "https://" : "http://") + frontendAddressHolder.getAddresses().get(0);
  }

  /**
   * Returns the account store.
   */
  protected AccountStore getAccountStore() {
    return accountStore;
  }

  /**
   * Returns the robot URI.
   */
  public abstract String getRobotUri();

  /**
   * Returns the robot participant id.
   */
  public abstract String getRobotId();
}
