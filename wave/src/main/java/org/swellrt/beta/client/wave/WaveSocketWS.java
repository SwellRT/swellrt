package org.swellrt.beta.client.wave;

import java.util.Queue;

import org.swellrt.beta.client.ServiceConfig;
import org.swellrt.beta.client.wave.Log.Level;
import org.swellrt.beta.client.wave.ws.CloseEvent;
import org.swellrt.beta.client.wave.ws.Event;
import org.swellrt.beta.client.wave.ws.MessageEvent;
import org.swellrt.beta.client.wave.ws.WebSocket;
import org.swellrt.beta.client.wave.ws.WebSocket.Function;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.model.util.CollectionUtils;

/**
 * Adapt the native browser WebSocket to WaveSocket interface.
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
 * <li>Client sends heart beats message with the value of recvCount (the server
 * side will remove oldest recvCount messages from its queue)
 * <li>Receiving heart beat response, then reset recvCount and remove oldest
 * values from sentMessages according to received value. <br>
 * On reconnection:
 * <li>Send reconnection message with recvCount, and reset recvCount
 * <li>On received reconnection message: discard the specified n oldest messages
 * from the setMessages queue. Sent rest of the queue.
 *
 *
 * @author pablojan (pablojan@gmail.com)
 *
 */
public class WaveSocketWS implements WaveSocket {

  private final static String WEBSOCKET_CONTEXT = "socket";

  private final static String CONNECTION_TOKEN_PARAM = "ct";

  /** The heart beat signal string */
  private static final String HEARTBEAT_DATA_PREFIX = "hb:";

  private static final String RECONNECTION_DATA_PREFIX = "rc:";

  private static final int HEARTBEAT_INTERVAL = ServiceConfig.websocketHeartbeatInterval();

  private static final int HEARTBEAT_TIMEOUT = ServiceConfig.websocketHeartbeatTimeout();

  private static final Log LOG;

  static {
    LOG = Log.get(WaveSocketWS.class);
    LOG.setLevel(ServiceConfig.websocketDebugLog() ? Level.DEBUG : Level.INFO);
  }

  private final Scheduler.IncrementalTask heartbeatTask = new Scheduler.IncrementalTask() {

    @Override
    public boolean execute() {
      if (ws.getReadyState() == WebSocket.OPEN) {
        ws.send(HEARTBEAT_DATA_PREFIX + recvCount);
        heartbeatAck = false;
        WaveDeps.lowPriorityTimer.scheduleDelayed(hearbeatCheckTask,
            HEARTBEAT_TIMEOUT);
      } else {
        return false;
      }
      return true;
    }

  };

  private final Scheduler.Task hearbeatCheckTask = new Scheduler.Task() {
    @Override
    public void execute() {
      if (!heartbeatAck) {
        heartbeatTurbulence = true;
        LOG.info("turbulence detected");
        callback.onTurbulence(false);
      }
    }
  };

  private final String serverUrl;

  /** Callback for the websocket */
  private final WaveSocketCallback callback;
  private WebSocket ws;
  private boolean heartbeatOn = HEARTBEAT_INTERVAL != 0;
  private boolean heartbeatAck = true;
  private boolean heartbeatTurbulence = false;

  private boolean connectedAtLeastOnce = false;

  private final Queue<String> sentMessages = CollectionUtils.createQueue();
  private int recvCount = 0;

  public WaveSocketWS(String serverUrl, String connectionToken, WaveSocketCallback callback) {
    if (serverUrl.charAt(serverUrl.length() - 1) != '/')
      serverUrl += "/" + WEBSOCKET_CONTEXT;
    else
      serverUrl += WEBSOCKET_CONTEXT;

    if (connectionToken != null) {
      serverUrl += "?" + CONNECTION_TOKEN_PARAM + "=" + connectionToken;
    }

    this.serverUrl = serverUrl;
    this.callback = callback;
  }

  protected void startHeartbeat() {
    if (heartbeatOn)
      WaveDeps.lowPriorityTimer.scheduleRepeating(heartbeatTask, HEARTBEAT_INTERVAL,
          HEARTBEAT_INTERVAL);
  }

  protected void stopHeartbeat() {
    if (heartbeatOn) {
      WaveDeps.lowPriorityTimer.cancel(heartbeatTask);
      stopHeartbeatCheck();
    }
  }

  protected void stopHeartbeatCheck() {
    WaveDeps.lowPriorityTimer.cancel(hearbeatCheckTask);
  }

  @Override
  public void connect() {

    ws = WaveDeps.websocketFactory.create();

    try {
      ws.connect(serverUrl);
      LOG.debug("created");
    } catch (Exception e) {
      LOG.severe("create exception", e);
      callback.onError("WebSockets not available ");
      return;
    }

    ws.onOpen(new WebSocket.Function<Event>() {
      @Override
      public void exec(Event e) {

        // This is a reconnection
        if (connectedAtLeastOnce) {
          ws.send(RECONNECTION_DATA_PREFIX + recvCount);
          LOG.debug("reconnection: sent ACK for " + recvCount + " messages");
          recvCount = 0;
        }

        connectedAtLeastOnce = true;
        startHeartbeat();
        LOG.debug("open");
        callback.onConnect();
      }
    });

    ws.onClose(new WebSocket.Function<CloseEvent>() {
      @Override
      public void exec(CloseEvent e) {
        stopHeartbeat();
        LOG.debug("close (" + e.code + "," + e.reason + ")");
        callback.onDisconnect();

        if (e.code == 1002 || e.code == 1011)
          callback.onError(e.reason);
      }
    });

    ws.onError(new Function<Event>() {
      @Override
      public void exec(Event e) {
        stopHeartbeat();
        LOG.debug("error");
        // Only notify fatal errors, e.g. when we can't connect first time
        if (!connectedAtLeastOnce)
          callback.onError("Error opening WebSocket");

      }
    });

    ws.onMessage(new Function<MessageEvent>() {
      @Override
      public void exec(MessageEvent e) {
        String data = (String) e.data;

        if (data != null) {

          if (data.startsWith(RECONNECTION_DATA_PREFIX)) {
            handleReconnectionMessage(data);
            return;
          }

          if (data.startsWith(HEARTBEAT_DATA_PREFIX)) {
            handleHeartbeatMessage(data);
            return;
          }

          recvCount++;
          callback.onMessage((String) e.data);

        }

      }
    });



  }

  @Override
  public void disconnect() {
    ws.close();
  }

  @Override
  public void sendMessage(String message) {
    sentMessages.add(message);
    ws.send(message);
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

      int rs = 0;
      while (!sentMessages.isEmpty()) {
        ws.send(sentMessages.poll());
        rs++;
      }

      LOG.debug(
          "reconnection: received ACK for " + n + " messages / resent " + rs + " pending messages");

    } catch (Exception ex) {
      LOG.severe("Error processing reconnection message ", ex);
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
      LOG.severe("Error processing heart beat message ", ex);
    }

    recvCount = 0;

    heartbeatAck = true;
    stopHeartbeatCheck();

    if (heartbeatTurbulence) {
      LOG.info("turbulence finished");
      heartbeatTurbulence = false;
      callback.onTurbulence(true);
    }

  }

}
