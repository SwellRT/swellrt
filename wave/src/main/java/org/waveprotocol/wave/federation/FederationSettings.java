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

package org.waveprotocol.wave.federation;

import org.waveprotocol.wave.util.settings.Setting;

import java.util.List;

/**
 * Settings specific to federation.
 */
@SuppressWarnings("unused") // We inject them by the name of their flag
public class FederationSettings {
  public static final String XMPP_SERVER_HOSTNAME = "xmpp_server_hostname";
  public static final String XMPP_SERVER_SECRET = "xmpp_server_secret";
  public static final String XMPP_COMPONENT_NAME = "xmpp_component_name";
  public static final String XMPP_SERVER_COMPONENT_PORT = "xmpp_server_component_port";
  public static final String XMPP_SERVER_IP = "xmpp_server_ip";
  public static final String XMPP_SERVER_DESCRIPTION = "xmpp_server_description";
  public static final String XMPP_DISCO_FAILED_EXPIRY_SECS = "xmpp_disco_failed_expiry_secs";
  public static final String XMPP_DISCO_SUCCESSFUL_EXPIRY_SECS = "xmpp_disco_successful_expiry_secs";
  public static final String XMPP_JID = "xmpp_jid";

  public static final String CERTIFICATE_PRIVATE_KEY = "certificate_private_key";
  public static final String CERTIFICATE_FILES = "certificate_files";
  public static final String CERTIFICATE_DOMAIN = "certificate_domain";

  @Setting(name = XMPP_SERVER_HOSTNAME)
  private static String xmppServerHostname;

  @Setting(name = XMPP_SERVER_SECRET)
  private static String xmppServerSecret;

  @Setting(name = XMPP_COMPONENT_NAME)
  private static String xmppComponentName;

  @Setting(name = XMPP_SERVER_COMPONENT_PORT)
  private static int xmppServerPort;

  @Setting(name = XMPP_SERVER_IP)
  private static String xmppServerIp;

  @Setting(name = XMPP_SERVER_DESCRIPTION)
  private static String xmppServerDescription;

  // default value is 5 minutes
  @Setting(name = XMPP_DISCO_FAILED_EXPIRY_SECS, defaultValue = "300")
  private static int xmppDiscoFailedExpirySecs;

  // default value is 2 hours
  @Setting(name = XMPP_DISCO_SUCCESSFUL_EXPIRY_SECS, defaultValue = "7200")
  private static int xmppDiscoSuccessfulExpirySecs;

  @Setting(name = XMPP_JID)
  private static String xmppJid;

  @Setting(name = CERTIFICATE_PRIVATE_KEY)
  private static String certificatePrivKey;

  @Setting(name = CERTIFICATE_FILES, description = "comma separated WITH NO SPACES.")
  private static List<String> certificateFiles;

  @Setting(name = CERTIFICATE_DOMAIN)
  private static String certificateDomain;
}
