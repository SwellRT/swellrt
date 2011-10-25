/**
 * The MIT License
 * Copyright (c) 2010 Tad Glines
 *
 * Contributors: Ovea.com, Mycila.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.waveprotocol.box.server.rpc;

import com.google.inject.Inject;

import com.glines.socketio.server.AnnotationTransportHandlerProvider;
import com.glines.socketio.server.ClasspathTransportDiscovery;
import com.glines.socketio.server.ServletBasedSocketIOConfig;
import com.glines.socketio.server.SocketIOConfig;
import com.glines.socketio.server.SocketIOInbound;
import com.glines.socketio.server.SocketIOServlet;
import com.glines.socketio.server.SocketIOSessionManager;
import com.glines.socketio.server.Transport;
import com.glines.socketio.server.TransportDiscovery;
import com.glines.socketio.server.TransportHandlerProvider;
import com.glines.socketio.server.TransportInitializationException;
import com.glines.socketio.server.TransportType;
import com.glines.socketio.server.transport.FlashSocketTransport;
import com.glines.socketio.server.transport.HTMLFileTransport;
import com.glines.socketio.server.transport.JSONPPollingTransport;
import com.glines.socketio.server.transport.XHRMultipartTransport;
import com.glines.socketio.server.transport.XHRPollingTransport;
import com.glines.socketio.server.transport.jetty.JettyWebSocketTransport;
import com.glines.socketio.util.IO;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public abstract class AbstractWaveSocketIOServlet extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(SocketIOServlet.class.getName());
    private static final long serialVersionUID = 2L;

    private final SocketIOSessionManager sessionManager = new SocketIOSessionManager();
    private final TransportHandlerProvider transportHandlerProvider = new AnnotationTransportHandlerProvider();

    private SocketIOConfig config;
    private final Transport[] transports;

    public AbstractWaveSocketIOServlet(Transport...transports) {
      this.transports = transports;
    }

    @Override
    public void init() throws ServletException {
        config = new ServletBasedSocketIOConfig(getServletConfig(), "socketio");

        // lazy load available transport handlers
        transportHandlerProvider.init();
        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.log(Level.INFO, "Transport handlers loaded: " + transportHandlerProvider.listAll());

        // lazily load available transports
        //TransportDiscovery transportDiscovery = new ClasspathTransportDiscovery();
        for (Transport transport : transports) {
            if (transportHandlerProvider.isSupported(transport.getType())) {
                transport.setTransportHandlerProvider(transportHandlerProvider);
                config.addTransport(transport);
            } else {
                LOGGER.log(Level.WARNING, "Transport " + transport.getType() + " ignored since not supported by any TransportHandler");
            }
        }
        // initialize them
        for (Transport t : config.getTransports()) {
            try {
                t.init(getServletConfig());
            } catch (TransportInitializationException e) {
                config.removeTransport(t.getType());
                LOGGER.log(Level.WARNING, "Transport " + t.getType() + " disabled. Initialization failed: " + e.getMessage());
            }
        }
        if (LOGGER.isLoggable(Level.INFO))
            LOGGER.log(Level.INFO, "Transports loaded: " + config.getTransports());
    }

    @Override
    public void destroy() {
        for (Transport t : config.getTransports()) {
            t.destroy();
        }
        super.destroy();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        serve(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        serve(req, resp);
    }

    /**
     * Returns an instance of SocketIOInbound or null if the connection is to be denied.
     * The value of cookies and protocols may be null.
     */
    protected abstract SocketIOInbound doSocketIOConnect(HttpServletRequest request);

    private void serve(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String path = request.getPathInfo();
        if (path == null || path.length() == 0 || "/".equals(path)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing SocketIO transport");
            return;
        }
        if (path.startsWith("/")) path = path.substring(1);
        String[] parts = path.split("/");

        Transport transport = config.getTransport(TransportType.from(parts[0]));
        if (transport == null) {
            if ("GET".equals(request.getMethod()) && "socket.io.js".equals(parts[0])) {
                response.setContentType("text/javascript");
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("com/glines/socketio/socket.io.js");
                OutputStream os = response.getOutputStream();
                IO.copy(is, os);
                return;
            }else if ("GET".equals(request.getMethod()) && "WebSocketMain.swf".equals(parts[0])) {
                response.setContentType("application/x-shockwave-flash");
                InputStream is = this.getClass().getClassLoader().getResourceAsStream("com/glines/socketio/WebSocketMain.swf");
                OutputStream os = response.getOutputStream();
                IO.copy(is, os);
                return;
            } else {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown SocketIO transport: " + parts[0]);
                return;
            }
        }

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.log(Level.FINE, "Handling request from " + request.getRemoteHost() + ":" + request.getRemotePort() + " with transport: " + transport.getType());

        transport.handle(request, response, new Transport.InboundFactory() {
            @Override
            public SocketIOInbound getInbound(HttpServletRequest request) {
                return AbstractWaveSocketIOServlet.this.doSocketIOConnect(request);
            }
        }, sessionManager);
    }

}
