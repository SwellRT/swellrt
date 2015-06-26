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
import com.google.gwt.core.shared.GWT;

import org.waveprotocol.wave.model.util.Base64DecoderException;
import org.waveprotocol.wave.model.util.CharBase64;

/**
 * The wrapper implementation of the atmosphere javascript client. By now only
 * implements Long-polling transport without fallback.
 * 
 * Atmosphere server and client must support following features:
 * <ul>
 * <li>Heart beat messages</li>
 * <li>Track message size + Base64 message encoding</li>
 * <li>Multiple Wave messages packed in one single HTTP message</li>
 * </ul>
 * 
 * TODO: implement fallback transport procedure, improve exception control and
 * propagation
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

        //socket.request.logLevel = 'debug';

        socket.request.transport = 'long-polling';
        socket.request.fallbackTransport = 'polling';

        // Track Message Lenght. Avoid partial message delivery
        socket.request.trackMessageLength = true;

        // CORS
        socket.request.enableXDR = true;
        socket.request.readResponsesHeaders = false;
        socket.request.withCredentials = true;

        socket.request.connectTimeout = 6000;
        socket.request.maxReconnectOnClose = 5; // Number of reconnect attempts before throw an error
        socket.request.reconnectOnServerError = true; // Try to reconnect on server's errors

        // Used with the server's TrackMessageSizeB64Interceptor
        // Delegate Base64 decoding to
        socket.request.trackMessageLength = true;


        // OPEN
        socket.request.onOpen = function() {
        atmosphere.util.debug("Atmosphere Connection Open");
        impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onConnect()();
        };

        // REOPEN
        socket.request.onReopen = function() {
        atmosphere.util.debug("Atmosphere Connection ReOpen");
        impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onConnect()();
        };

        // MESSAGE
        socket.request.onMessage = function(response) {
        //atmosphere.util.debug("Atmosphere Message received: "+response.responseBody);
        //atmosphere.util.debug("Atmosphere Message received");
        impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onMessage(Ljava/lang/String;)(response.responseBody);

        };

        // CLOSE
        socket.request.onClose = function(response) {
        atmosphere.util.debug("Atmosphere Connection Close");
        impl.@org.waveprotocol.box.webclient.client.atmosphere.AtmosphereConnectionImpl::onDisconnect(Ljava/lang/String;)(response);
        };

        // TRANSPORT FAILURE
        socket.request.onTransportFailure = function(errorMsg, request) {
        //atmosphere.util.debug("Atmosphere Connection Transport Failure: "+errorMsg);
        };

        //  RECONNECT
        socket.request.onReconnect = function(request, response) {
        atmosphere.util.debug("Atmosphere Connection Reconnect");

        //atmosphere.util.debug("Reconnect request.transport: "+request.transport);
        //atmosphere.util.debug("Reconnect request.enableXDR: "+request.enableXDR);
        //atmosphere.util.debug("Reconnect request.readResponsesHeaders: "+request.readResponsesHeaders);
        //atmosphere.util.debug("Reconnect request.withCredentials: "+request.withCredentials);
        //atmosphere.util.debug("Reconnect request.maxReconnectOnClose: "+request.maxReconnectOnClose);
        };

        // ERROR
        socket.request.onError = function(response) {
        atmosphere.util.debug("Atmosphere Connection Error");
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

      try {

      // Decode from Base64 because of Atmosphere Track Message Lenght server
      // feauture
      // NOTE: no Charset is specified, so this relies on UTF-8 as default
      // charset
      String dm = new String(CharBase64.decode(message));

      // Split into differente Wave Protocol messages.
      // Atmosphere's server implementation usually pack several Wave messages
      // in on HTTP response
      if (dm.indexOf('|') == 0) {
        while (dm.indexOf('|') == 0 && dm.length() > 1) {
          dm = dm.substring(1);
          int marker = dm.indexOf("}|");
          GWT.log("onMessage: " + dm);
          listener.onMessage(dm);
          dm = dm.substring(marker + 1);
        }
      } else {

        // Ignore heart-beat messages
        // NOTE: is heart beat string always " "?
        if (dm != null && !dm.isEmpty() && !dm.equals("  ")) {

          if (dm.charAt(dm.length() - 1) == '|') dm = dm.substring(0, dm.length() - 1);
          GWT.log("onMessage: " + dm);
          listener.onMessage(dm);
        }
      }

      } catch (Base64DecoderException e) {
        GWT.log("Error decoding Base64 message", e);
      }

    }


}
