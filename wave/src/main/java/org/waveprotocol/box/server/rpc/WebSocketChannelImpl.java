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
import java.util.Queue;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.swellrt.beta.client.wave.WaveSocketWS;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.util.logging.Log;

/**
 * A channel implementation for websocket.
 *
 * <p>
 * <br>
 * <li>Implements heart beat on the WebSocket to detect network turbulence.
 * <li>Implements a transparent reconnection mechanism with message
 * reconciliation.
 *
 * <br>
 * <br>
 * Reconciliation mechanism is implemented as follows:
 * <li>We assume web socket messages preserve order
 * <li>Queue each message to be sent in sentMessages
 * <li>Increment recvCount for each incoming message.
 * <li>On send heart beat message with the value of recvCount (the other side
 * will remove oldest recvCount messages from its queue)
 * <li>On Receive heart beat response: reset recvCount and remove oldest values
 * from sentMessages according to received value. <br>
 * On reconnection:
 * <li>Send reconnection message with recvCount, and reset recvCount
 * <li>On received reconnection message: discard the specified n oldest messages
 * from the setMessages queue. Sent rest of the queue.
 *
 * See counter part class for client {@link WaveSocketWS}
 *
 * @author akaplanov@gmai.com (A. Kaplanov)
 * @author pablojan@gmai.com (Pablo Ojanguren)
 */
@WebSocket
public class WebSocketChannelImpl extends WebSocketChannel {
  private static final Log LOG = Log.get(WebSocketChannelImpl.class);

  /** The heart beat signal string */
  private static final String HEARTBEAT_DATA_PREFIX = "hb:";

  private static final String RECONNECTION_DATA_PREFIX = "rc:";

  private Session session;
  private int count = 0;
  private final String connectionId;

  private final Queue<String> sentMessages = CollectionUtils.createQueue();
  private int recvCount = 0;

  public WebSocketChannelImpl(String connectionId, ProtoCallback callback) {
    super(callback);
    this.connectionId = connectionId;
  }

  @OnWebSocketConnect
  public void onOpen(Session session) {
    synchronized (this) {
      this.session = session;
      count++;
    }

    LOG.info("Websocket[" + connectionId + "] open (#" + count + ")");
  }

  @OnWebSocketMessage
  public void onMessage(String data) {

    if (data.startsWith(HEARTBEAT_DATA_PREFIX)) {
      handleHeartbeatMessage(data);
      return;
    }

    if (data.startsWith(RECONNECTION_DATA_PREFIX)) {
      handleReconnectionMessage(data);
      return;
    }

    recvCount++;
    handleMessageString(data);
  }

  @OnWebSocketClose
  public void onClose(int closeCode, String closeReason) {
    LOG.info(
        "Websocket[" + connectionId + "] disconnected (" + closeCode + " - " + closeReason + ")");
    synchronized (this) {
      session = null;
    }
  }

  @Override
  public void sendMessageString(String data) throws IOException {
    synchronized (this) {
      sentMessages.add(data);
      if (session == null) {
        LOG.fine("Websocket[" + connectionId + "] is not connected");
      } else {
        session.getRemote().sendStringByFuture(data);
      }
    }
  }

  /**
   * @param message
   *          the message starting with {@link #RECONNECTION_DATA_PREFIX}
   */
  protected void handleReconnectionMessage(String message) {

    try {
      String tmp = message.substring(3);
      int n = Integer.parseInt(tmp);

      for (int i = 0; i < n; i++)
        sentMessages.poll();

      synchronized (this) {
        if (session != null) {
          int rs = 0;
          while (!sentMessages.isEmpty()) {
            session.getRemote().sendStringByFuture(sentMessages.poll());
            rs++;
          }

          // Reset our recv. counter optimistically: we assume
          // the client will receive this message (thus update its queue).
          session.getRemote().sendStringByFuture(RECONNECTION_DATA_PREFIX + recvCount);
          recvCount = 0;

          LOG.info("Websocket[" + connectionId + "] reconnection: received ACK for " + n
              + " messages / sent ACK for " + recvCount + " messages / resent " + rs
              + " pending messages");

        }
      }

    } catch (Exception ex) {
      LOG.warning("Websocket[" + connectionId + "] Error processing reconnection message: "
          + ex.getMessage());
    }
  }

  /**
   * @param message
   *          the message starting with {@link #HEARTBEAT_DATA_PREFIX}
   */
  protected void handleHeartbeatMessage(String message) {

    //
    // Heart beat data format is
    // hb:<n>
    // where n = number of messages ACK'ed by the server
    //
    // remove oldest n messages in the queue
    //

    try {
      String tmp = message.substring(3);
      int n = Integer.parseInt(tmp);

      for (int i = 0; i < n; i++)
        sentMessages.poll();

    } catch (Exception ex) {
      LOG.warning("Websocket[" + connectionId + "] Error processing heart beat message: "
          + ex.getMessage());
    }

    synchronized (this) {
      if (session != null) {
        session.getRemote().sendStringByFuture(HEARTBEAT_DATA_PREFIX + recvCount);
        // Reset our recv. counter optimistically: we assume
        // the client will receive this message (thus update its queue).
        recvCount = 0;
      }
    }

  }

}
