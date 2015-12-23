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

package org.waveprotocol.box.webclient.client.atmosphere;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.ScriptInjector;

import org.swellrt.model.generic.Model;
import org.waveprotocol.box.webclient.client.WaveSocket;

import java.util.logging.Logger;

/**
 * A wrapper implementation of the atmosphere javascript client. Websocket
 * transport will be used first by default. If not avaiable or a fatal error
 * occurs, long-polling will be tried.
 *
 *
 * The atmosphere client/server configuration avoids network issues with:
 *
 * <ul>
 * <li>Server Heartbeat frecuency t = 60s. Greater values didn't avoid cuts in
 * some environments</li>
 * <li>Client will start reconnection if no data is received in t = 70s
 * (timeout)</li>
 * </ul>
 *
 * Both settings try to keep the communication alive.
 *
 * AtmosphereConnectionListener.onDisconnect() will be invoked when
 * communications is stopped caused by an error or not.
 *
 * Exceptions handling server messages must be caught because JSNI code
 * receiving messages is not wrapped with the $entry() method.
 *
 *
 * More info about Atmosphere:
 * https://github.com/Atmosphere/atmosphere/wiki/jQuery.atmosphere.js-atmosphere
 * .js-API
 *
 * More info about transports:
 * https://github.com/Atmosphere/atmosphere/wiki/Supported
 * http://stackoverflow.com/questions/9397528/server-sent-events-vs-polling
 *
 *
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WaveSocketAtmosphere implements WaveSocket {

      private static final Logger log = Logger.getLogger(WaveSocketAtmosphere.class.getName());

      private static final class AtmosphereSocket extends JavaScriptObject {

        public static native AtmosphereSocket create(WaveSocketAtmosphere impl, String urlBase, String transport, String fallback, String clientVersion) /*-{


          // Atmoshpere client
          var atmosphere = $wnd.atmosphere;

          // Atmosphere socket
          var socket = {
            request: null,
            socket: null
          };

          $wnd.responses = new Array();

          var socketURL = urlBase;
          if (socketURL.charAt(socketURL.length-1) != "/")
            socketURL+="/";
          socketURL += 'atmosphere';

          // Set up atmosphere connection properties
          socket.request = new atmosphere.AtmosphereRequest();
          socket.request.uuid = 0;

          // It's true by default. Just a reminder.
          socket.request.enableProtocol = true;
          socket.request.async = true;

          socket.request.url = socketURL;
          socket.request.contenType = 'text/plain;charset=UTF-8';

          socket.request.logLevel = 'debug';


          socket.request.transport = transport;
          socket.request.fallbackTransport = fallback;

          // Track Message Lenght
          // Used with the server's TrackMessageSizeB64Interceptor
          socket.request.trackMessageLength = true;

          // CORS
          socket.request.enableXDR = true;
          socket.request.readResponsesHeaders = false;
          socket.request.withCredentials = true;
          socket.request.dropHeaders = true;

          // This value assumes that server sends hearbeat messages in less than 70s (timeout value)
          // This allows to detect network cuts
          socket.request.timeout = 70000;

          // Reconnection policy

          // The connect timeout. If the client fails to connect, the fallbackTransport will be used
          socket.request.connectTimeout = -1;

          // Time between reconnection attempts
          socket.request.reconnectInterval = 5000;

          // Number of reconnect attempts before throw an error
          socket.request.maxReconnectOnClose = 5;

          // Try to reconnect on server's errors
          socket.request.reconnectOnServerError = true;

          // Server<->client version control
          socket.request.headers = {
            'X-client-version' : clientVersion
          };


          // OPEN
          socket.request.onOpen = function(response) {
            impl.@org.waveprotocol.box.webclient.client.atmosphere.WaveSocketAtmosphere::onConnect()();
          };

          // REOPEN
          socket.request.onReopen = function() {
            impl.@org.waveprotocol.box.webclient.client.atmosphere.WaveSocketAtmosphere::onConnect()();
          };

          // MESSAGE
          socket.request.onMessage = function(response) {
            impl.@org.waveprotocol.box.webclient.client.atmosphere.WaveSocketAtmosphere::onMessage(Ljava/lang/String;)(response.responseBody);

          };

          // CLOSE
          socket.request.onClose = function(response) {
            impl.@org.waveprotocol.box.webclient.client.atmosphere.WaveSocketAtmosphere::onDisconnect()();
          };

          // TRANSPORT FAILURE
          socket.request.onTransportFailure = function(error, request) {
            atmosphere.util.debug("Atmosphere Connection Transport Failure "+error);

            request.contenType = 'text/plain;charset=UTF-8';

            // The long polling wait time
            request.timeout = 70000;

            request.transport = fallback;
            request.fallbackTransport = null;

            request.trackMessageLength = true;

            request.uuid = 0;

            // CORS
            request.enableXDR = true;
            request.readResponsesHeaders = false;
            request.withCredentials = true;


            // Reconnection policy

            // The connect timeout. If the client fails to connect, the fallbackTransport will be used
            request.connectTimeout = -1;

            // Time between reconnection attempts
            request.reconnectInterval = 5000;

            // Number of reconnect attempts before throw an error
            request.maxReconnectOnClose = 5;

            // Try to reconnect on server's errors
            request.reconnectOnServerError = true;

            // Server<->client version control
            request.headers = {
              'X-client-version' : clientVersion
            };

          };

          //  RECONNECT
          socket.request.onReconnect = function(request, response) {
             request.uuid = 0;
          };

          // ERROR
          socket.request.onError = function(response) {
            impl.@org.waveprotocol.box.webclient.client.atmosphere.WaveSocketAtmosphere::onError(Ljava/lang/String;)(response.status);
          };

          // CLIENT TIMEOUT
          socket.request.onClientTimeout = function(request) {
            impl.@org.waveprotocol.box.webclient.client.atmosphere.WaveSocketAtmosphere::onDisconnect()();
          };

          socket.request.callback = function(response) {
            atmosphere.util.debug("Atmosphere callback "+response.status);
          };

          $wnd._socket = socket;
          return socket;
        }-*/;


        protected AtmosphereSocket() {

        }


        public native void close() /*-{
           // Avoid Use of XMLHttpRequest's withCredentials attribute is no longer supported
           // in the synchronous mode in window context.
           this.socket.request.withCredentials = false;
           $wnd.atmosphere.unsubscribe();
        }-*/;

        public native AtmosphereSocket connect() /*-{
           this.socket = $wnd.atmosphere.subscribe(this.request);
        }-*/;

        public native void send(String data) /*-{
           this.socket.push(data);
        }-*/;

    }


    private final WaveSocketCallback listener;
    private final String urlBase;
    private AtmosphereSocket socket = null;
    private final boolean useWebSocket;



    public WaveSocketAtmosphere(WaveSocketCallback listener,
               String urlBase, boolean useWebSocketAlt) {
        this.listener = listener;
        this.urlBase = urlBase;
        this.useWebSocket = !useWebSocketAlt;
    }


    @Override
    public void connect() {

      if (socket == null) {

        // Build the atmosphere.js URL using the Websocket URL
        final String scriptHost =
            urlBase.startsWith("wss://") ? "https://" + urlBase.substring(6) : "http://"
                + urlBase.substring(5);

            String scriptUrl = new String(scriptHost);

            // Remove trailing '/' before add context
            if (scriptUrl.lastIndexOf("/") == scriptUrl.length() - 1)
              scriptUrl = scriptUrl.substring(0, scriptUrl.length() - 1);

            scriptUrl += "/atmosphere/atmosphere.js";

            ScriptInjector.fromUrl(scriptUrl).setCallback(new Callback<Void, Exception>() {
              public void onFailure(Exception reason) {
                throw new IllegalStateException("atmosphere.js load failed!");
              }

              public void onSuccess(Void result) {
                // We assume Atmosphere is going to work only with http(s) schemas
          socket =
              AtmosphereSocket.create(WaveSocketAtmosphere.this, scriptHost, useWebSocket
                  ? "websocket" : "long-polling", "long-polling", Model.MODEL_VERSION);
                socket.connect();
              }

            }).setWindow(ScriptInjector.TOP_WINDOW).inject();

      } else {
        socket.connect();
      }
    }


    @Override
    public void sendMessage(String message) {
      socket.send(message);
    }


    /**
     * Atmosphere has detected a fatal error in the connection. Trigger a fallback
     * connection with long-polling, trying to avoid unstable WebSocket
     * connections.
     */
    @SuppressWarnings("unused")
    private void onError(String error) {
      log.severe(error);
      listener.onDisconnect();
    }

    /**
     * Notify the Wave's socket that connection is ready. It will be invoked in an
     * Atmosphere's open or reconnect event.
     *
     */
    @SuppressWarnings("unused")
    private void onConnect() {
       listener.onConnect();
    }

    /**
     * Notify the Wave's socket that connection was closed. It will be invoked in
     * an Atmosphere's onClose event.
     *
     */
      @SuppressWarnings("unused")
    private void onDisconnect() {
      listener.onDisconnect();
    }



    @SuppressWarnings("unused")
    private void onMessage(String message) {
    listener.onMessage(message);
  }


  @Override
  public void disconnect() {
    socket.close();
    }


}
