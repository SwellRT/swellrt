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

/**
 * The wrapper implementation of the atmosphere javascript client.
 *
 * https://github.com/Atmosphere/atmosphere/wiki/jQuery.atmosphere.js-atmosphere
 * .js-API
 *
 * More info about transports
 * https://github.com/Atmosphere/atmosphere/wiki/Supported
 * -WebServers-and-Browsers
 *
 * It tries to use Server-Sent Events first and fallback transport to
 * long-polling. We ignore Websockets by now because they are the default
 * transport on WiAB and atmosphere is the fallback.
 *
 * http://stackoverflow.com/questions/9397528/server-sent-events-vs-polling
 *
 *
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class AtmosphereConnectionImpl implements AtmosphereConnection {


      private static final class AtmosphereSocket extends JavaScriptObject {

        public static native AtmosphereSocket create(AtmosphereConnectionImpl impl, String urlBase) /*-{

          // Atmoshpere client
          var atmosphere = $wnd.atmosphere;

          // Atmosphere socket
          var socket = {
            request: null,
            socket: null
          };


          var socketURL = urlBase;
          if (socketURL.charAt(socketURL.length-1) != "/")
            socketURL+="/";
          socketURL += 'atmosphere';

          // Set up atmosphere connection properties
          socket.request = new atmosphere.AtmosphereRequest();

          socket.request.url = socketURL;
          socket.request.contenType = 'text/plain;charset=UTF-8';

          socket.request.logLevel = 'debug';
          socket.request.transport = 'websocket';
          socket.request.fallbackTransport = 'long-polling';

          // CORS
          socket.request.enableXDR = true;
          socket.request.readResponsesHeaders = false;
          socket.request.withCredentials = true;

          socket.request.connectTimeout = 6000;
          socket.request.maxReconnectOnClose = 5; // Number of reconnect attempts before throw an error
          socket.request.reconnectOnServerError = true; // Try to reconnect on server's errors

          //socket.request.trackMessageLength = true;


          // OPEN
          socket.request.onOpen = function() {
            atmosphere.util.info("Atmosphere Connection Open");
            impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onConnect()();
          };

           // REOPEN
          socket.request.onReopen = function() {
            atmosphere.util.info("Atmosphere Connection ReOpen");
            impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onConnect()();
          };

          // MESSAGE
          socket.request.onMessage = function(response) {
            atmosphere.util.info("Atmosphere Message received: "+response.responseBody);
            var r = response.responseBody;

            if (r.indexOf('|') == 0) {
              while (r.indexOf('|') == 0 && r.length > 1) {
                r = r.substring(1);
                var marker = r.indexOf('}|');
                impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onMessage(Ljava/lang/String;)(r.substring(0, marker+1));
                r = r.substring(marker+1);
              }
            }
            else {
              impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onMessage(Ljava/lang/String;)(r);
            }
          };

          // CLOSE
          socket.request.onClose = function(response) {
            atmosphere.util.info("Atmosphere Connection Close");
            impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onDisconnect(Ljava/lang/String;)(response);
          };

          // TRANSPORT FAILURE
          socket.request.onTransportFailure = function(errorMsg, request) {
            atmosphere.util.info("Atmosphere Connection Transport Failure: "+errorMsg);
          };

          //  RECONNECT
          socket.request.onReconnect = function(request, response) {
            atmosphere.util.info("Atmosphere Connection Reconnect");

            atmosphere.util.info("Reconnect request.transport: "+request.transport);
            atmosphere.util.info("Reconnect request.enableXDR: "+request.enableXDR);
            atmosphere.util.info("Reconnect request.readResponsesHeaders: "+request.readResponsesHeaders);
            atmosphere.util.info("Reconnect request.withCredentials: "+request.withCredentials);
            atmosphere.util.info("Reconnect request.maxReconnectOnClose: "+request.maxReconnectOnClose);
          };

          // ERROR
          socket.request.onError = function(response) {
            atmosphere.util.info("Atmosphere Connection Error");
          };

          return socket;
        }-*/;


        protected AtmosphereSocket() {

        }


        public native void close() /*-{
           this.socket.unsubscribe();
        }-*/;

        public native AtmosphereSocket connect() /*-{
           this.socket = $wnd.atmosphere.subscribe(this.request);
        }-*/;

        public native void send(String data) /*-{
           this.socket.push(data);
        }-*/;

    }



    private final AtmosphereConnectionListener listener;
    private String urlBase;
    private AtmosphereConnectionState state;
    private AtmosphereSocket socket = null;

    public AtmosphereConnectionImpl(AtmosphereConnectionListener listener,
               String urlBase) {
        this.listener = listener;
        this.urlBase = urlBase;

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
                socket = AtmosphereSocket.create(AtmosphereConnectionImpl.this, scriptHost);
                socket.connect();
              }

            }).setWindow(ScriptInjector.TOP_WINDOW).inject();

      } else {
        socket.connect();
      }
    }

    @Override
    public void close() {
      socket.close();
    }


    @Override
    public void sendMessage(String message) {
      socket.send(message);
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
    private void onDisconnect(String response) {
      listener.onDisconnect();
    }

    @SuppressWarnings("unused")
    private void onMessage(String message) {
      listener.onMessage(message);
    }


}
