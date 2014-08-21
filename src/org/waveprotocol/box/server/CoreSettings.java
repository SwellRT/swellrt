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

import org.waveprotocol.wave.util.settings.Setting;

import java.util.List;

/**
 * Core Wave in a Box settings
 */
public class CoreSettings {
  public static final String WAVE_SERVER_DOMAIN = "wave_server_domain";
  public static final String HTTP_FRONTEND_PUBLIC_ADDRESS = "http_frontend_public_address";
  public static final String HTTP_WEBSOCKET_PUBLIC_ADDRESS = "http_websocket_public_address";
  public static final String HTTP_WEBSOCKET_PRESENTED_ADDRESS = "http_websocket_presented_address";
  public static final String HTTP_FRONTEND_ADDRESSES = "http_frontend_addresses";
  public static final String RESOURCE_BASES = "resource_bases";
  public static final String WAVESERVER_DISABLE_VERIFICATION = "waveserver_disable_verification";
  public static final String WAVESERVER_DISABLE_SIGNER_VERIFICATION =
      "waveserver_disable_signer_verification";
  public static final String ENABLE_FEDERATION = "enable_federation";
  public static final String SIGNER_INFO_STORE_TYPE = "signer_info_store_type";
  public static final String SIGNER_INFO_STORE_DIRECTORY = "signer_info_store_directory";
  public static final String ATTACHMENT_STORE_TYPE = "attachment_store_type";
  public static final String ATTACHMENT_STORE_DIRECTORY = "attachment_store_directory";
  public static final String ACCOUNT_STORE_TYPE = "account_store_type";
  public static final String ACCOUNT_STORE_DIRECTORY = "account_store_directory";
  public static final String DELTA_STORE_TYPE = "delta_store_type";
  public static final String DELTA_STORE_DIRECTORY = "delta_store_directory";
  public static final String SESSIONS_STORE_DIRECTORY = "sessions_store_directory";
  public static final String SESSION_COOKIE_MAX_AGE = "session_cookie_max_age";
  public static final String WEBSOCKET_MAX_MESSAGE_SIZE = "websocket_max_message_size";
  public static final String WEBSOCKET_MAX_IDLE_TIME = "websocket_max_idle_time";
  public static final String GADGET_SERVER_HOSTNAME = "gadget_server_hostname";
  public static final String GADGET_SERVER_PORT = "gadget_server_port";
  public static final String GADGET_SERVER_PATH = "gadget_server_path";
  public static final String ADMIN_USER = "admin_user";
  public static final String WELCOME_WAVE_ID = "welcome_wave_id";
  public static final String LISTENER_EXECUTOR_THREAD_COUNT = "listener_executor_thread_count";
  public static final String WAVELET_LOAD_EXECUTOR_THREAD_COUNT = "wavelet_load_executor_thread_count";
  public static final String DELTA_PERSIST_EXECUTOR_THREAD_COUNT = "delta_persist_executor_thread_count";
  public static final String STORAGE_CONTINUATION_EXECUTOR_THREAD_COUNT = "storage_continuation_executor_thread_count";
  public static final String LOOKUP_EXECUTOR_THREAD_COUNT = "lookup_executor_thread_count";
  public static final String ROBOT_CONNECTION_THREAD_COUNT = "robot_connection_thread_count";
  public static final String ROBOT_GATEWAY_THREAD_COUNT = "robot_gateway_thread_count";
  public static final String SOLR_THREAD_COUNT = "solr_thread_count";
  public static final String DISABLE_REGISTRATION = "disable_registration";
  public static final String ENABLE_SSL = "enable_ssl";
  public static final String SSL_KEYSTORE_PATH = "ssl_keystore_path";
  public static final String SSL_KEYSTORE_PASSWORD = "ssl_keystore_password";
  public static final String ENABLE_CLIENTAUTH = "enable_clientauth";
  public static final String CLIENTAUTH_CERT_DOMAIN = "clientauth_cert_domain";
  public static final String DISABLE_LOGINPAGE = "disable_loginpage";
  public static final String SEARCH_TYPE = "search_type";
  public static final String INDEX_DIRECTORY = "index_directory";
  public static final String ANALYTICS_ACCOUNT = "analytics_account";
  public static final String THUMBNAIL_PATTERNS_DIRECTORY = "thumbnail_patterns_directory";
  public static final String PROFILE_FETCHER_TYPE = "profile_fetcher_type";
  public static final String ENABLE_PROFILING  = "enable_profiling";
  public static final String MONGODB_HOST = "mongodb_host";
  public static final String MONGODB_PORT = "mongodb_port";
  public static final String MONGODB_DATABASE = "mongodb_database";
  public static final String SOLR_BASE_URL = "solr_base_url";

  @Setting(name = WAVE_SERVER_DOMAIN)
  private static String waveServerDomain;

  @Setting(name = HTTP_FRONTEND_PUBLIC_ADDRESS, defaultValue = "localhost:9898",
      description = "The server's public address.")
  private static String httpFrontEndPublicAddress;

  @Setting(name = HTTP_WEBSOCKET_PUBLIC_ADDRESS, defaultValue = "localhost:9898",
      description = "The server's websocket public address.")
  private static String httpWebsocketPublicAddress;

  @Setting(name = HTTP_WEBSOCKET_PRESENTED_ADDRESS, defaultValue = "localhost:9898",
      description = "The presented server's websocket address.")
  private static String httpWebsocketPresentedAddress;

  @Setting(name = HTTP_FRONTEND_ADDRESSES, defaultValue = "localhost:9898",
      description = "A comman seperated list of address on which to listen for connections."
          + " Each address is a host or ip and port seperated by a colon.")
  private static List<String> httpFrontEndAddresses;

  @Setting(name = RESOURCE_BASES, defaultValue = "./war",
      description = "The server's resource base directory list.")
  private static List<String> resourceBases;

  @Setting(name = WAVESERVER_DISABLE_VERIFICATION)
  private static boolean waveserverDisableVerification;

  @Setting(name = WAVESERVER_DISABLE_SIGNER_VERIFICATION)
  private static boolean waveserverDisableSignerVerification;

  @Setting(name = ENABLE_FEDERATION, defaultValue = "false")
  private static boolean enableFederation;

  @Setting(name = SIGNER_INFO_STORE_TYPE,
      description = "Type of persistence to use for the SignerInfo Storage",
      defaultValue = "memory")
  private static String signerInfoStoreType;

  @Setting(name = SIGNER_INFO_STORE_DIRECTORY,
      description = "Location on disk where the signer info store lives. Must be writeable by the "
          + "wave-in-a-box process. Only used by file-based signer info store.",
      defaultValue = "_certificates")
  private static String signerInfoStoreDirectory;

  @Setting(name = ATTACHMENT_STORE_TYPE,
      description = "Type of persistence store to use for attachments", defaultValue = "disk")
  private static String attachmentStoreType;

  @Setting(name = ATTACHMENT_STORE_DIRECTORY,
      description = "Location on disk where the attachment store lives. Must be writeable by the "
          + "fedone process. Only used by disk-based attachment store.",
      defaultValue = "_attachments")
  private static String attachmentStoreDirectory;

  @Setting(name = ACCOUNT_STORE_TYPE,
      description = "Type of persistence to use for the accounts", defaultValue = "memory")
  private static String accountStoreType;

  @Setting(name = ACCOUNT_STORE_DIRECTORY,
      description = "Location on disk where the account store lives. Must be writeable by the "
          + "wave-in-a-box process. Only used by file-based account store.",
      defaultValue = "_accounts")
  private static String accountStoreDirectory;

  @Setting(name = DELTA_STORE_TYPE,
      description = "Type of persistence to use for the deltas", defaultValue = "memory")
  private static String deltaStoreType;

  @Setting(name = DELTA_STORE_DIRECTORY,
      description = "Location on disk where the delta store lives. Must be writeable by the "
          + "wave-in-a-box process. Only used by file-based account store.",
      defaultValue = "_deltas")
  private static String deltaStoreDirectory;

  @Setting(name = SESSIONS_STORE_DIRECTORY,
      description = "Location on disk where the user sessions are persisted. Must be writeable by the "
          + "wave-in-a-box process.",
 defaultValue = "_sessions")
  private static String sessionsStoreDirectory;

  @Setting(name = SESSION_COOKIE_MAX_AGE,
      description = "Max age of session cookie in seconds. "
          + "-1 means cookie lives in the browser current session only", defaultValue = "-1")
  private static int sessionCookieMaxAge;

  @Setting(name = WEBSOCKET_MAX_IDLE_TIME,
      description = "The time in ms that the websocket connection can be idle before closing", defaultValue = "0")
  private static int websocketMaxIdleTime;

  @Setting(name = WEBSOCKET_MAX_MESSAGE_SIZE,
      description = "Maximum websocket message size to be received in MB", defaultValue = "2")
  private static int websocketMaxMessageSize;

  @Setting(name = GADGET_SERVER_HOSTNAME, description = "The hostname of the gadget server.",
      defaultValue = "gmodules.com")
  private static String gadgetServerHostname;

  @Setting(name = GADGET_SERVER_PORT, description = "The port of the gadget server.",
      defaultValue = "80")
  private static int gadgetServerPort;

  @Setting(name = GADGET_SERVER_PATH, description = "The URL path of the gadget server.",
      defaultValue = "/gadgets")
  private static String gadgetServerPath;

  @Setting(name = ADMIN_USER, description = "The admin user id for this server.",
      defaultValue = "@example.com")
  private static String adminUser;

  @Setting(name = WELCOME_WAVE_ID, description = "The welcome wave id.",
      defaultValue = "")
  private static String welcomeWaveId;

  @Setting(name = LISTENER_EXECUTOR_THREAD_COUNT,
      description = "The number of threads to process wavelet updates.",
      defaultValue = "1")
  private static int listenerExecutorThreadCount;

  @Setting(name = WAVELET_LOAD_EXECUTOR_THREAD_COUNT,
      description = "The number of threads for loading wavelets.",
      defaultValue = "1")
  private static int waveletLoadExecutorThreadCount;

  @Setting(name = DELTA_PERSIST_EXECUTOR_THREAD_COUNT,
      description = "The number of threads to persist deltas.",
      defaultValue = "1")
  private static int deltaPersistExecutorThreadCount;

  @Setting(name = STORAGE_CONTINUATION_EXECUTOR_THREAD_COUNT,
      description = "The number of threads to perform post wavelet loading logic.",
      defaultValue = "1")
  private static int storageContinuationExecutorThreadCount;

  @Setting(name = LOOKUP_EXECUTOR_THREAD_COUNT,
      description = "The number of threads to perform post wavelet loading logic.",
      defaultValue = "1")
  private static int lookupExecutorThreadCount;

  @Setting(name = ROBOT_CONNECTION_THREAD_COUNT,
      description = "The number of threads to perform robot client requests.",
      defaultValue = "10")
  private static int robotConnectionThreadCount;

  @Setting(name = ROBOT_GATEWAY_THREAD_COUNT,
      description = "The number of threads to handle any updates for robots.",
      defaultValue = "10")
  private static int robotGatewayThreadCount;

  @Setting(name = SOLR_THREAD_COUNT,
      description = "The number of threads to make search/update requests to Solr.",
      defaultValue = "1")
  private static int solrThreadCount;

  @Setting(name = DISABLE_REGISTRATION,
      description = "Prevents the register page from being available to anyone", defaultValue = "false")
  private static boolean disableRegistration;

  @Setting(name = ENABLE_SSL,
      description = "Enables SSL protocol on all address/port combinations", defaultValue = "false")
  private static boolean enableSsl;

  @Setting(name = SSL_KEYSTORE_PATH,
      description = "Path to the keystore containing SSL certificase to server", defaultValue = "./wiab.ks")
  private static String sslKeystorePath;

  @Setting(name = SSL_KEYSTORE_PASSWORD,
      description = "Password to the SSL keystore", defaultValue = "")
  private static String sslKeystorePassword;

  @Setting(name = ENABLE_CLIENTAUTH,
      description = "Enable x509 certificated based authentication", defaultValue = "false")
  private static boolean enableClientAuth;

  @Setting(name = CLIENTAUTH_CERT_DOMAIN,
      description = "Domain of email address in x509 cert", defaultValue = "")
  private static String clientAuthCertDomain;

  @Setting(name = DISABLE_LOGINPAGE,
      description = "Disable login page to force x509 only", defaultValue = "false")
  private static boolean disableLoginPage;

  @Setting(name = INDEX_DIRECTORY,
      description = "Location on disk where the index is persisted", defaultValue = "_indexes")
  private static String indexDirectory;

  @Setting(name = SEARCH_TYPE,
      description = "The wave search type", defaultValue = "lucene")
  private static String searchType;

  @Setting(name = ANALYTICS_ACCOUNT, description = "Google analytics id")
  private static String analyticsAccount;

  @Setting(name = THUMBNAIL_PATTERNS_DIRECTORY,
      description = "Thumbnail patterns directory",
      defaultValue = "")
  private static String thumbnailPatternsDirectory;

  @Setting(name = PROFILE_FETCHER_TYPE,
      description = "The wave profile fetcher used", defaultValue = "gravatar")
  private static String profileFetcherType;

  @Setting(name = ENABLE_PROFILING,
      description = "Enable profiling statistic",
      defaultValue = "false")
  private static boolean enableProfiling;

  @Setting(name = MONGODB_HOST, description = "The host address for the MongoDB server",
      defaultValue = "127.0.0.1")
  private static String mongoDBhost;

  @Setting(name = MONGODB_PORT, description = "The port number of the MongoDB server",
      defaultValue = "27017")
  private static String mongoDBport;

  @Setting(name = MONGODB_DATABASE, description = "The database name used in the MongoDB server",
      defaultValue = "wiab")
  private static String mongoDBdatabase;

  @Setting(name = SOLR_BASE_URL, description = "The Solr base url",
      defaultValue = "http://localhost:8983/solr")
  private static String solrBaseUrl;
}
