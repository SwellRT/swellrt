package org.swellrt.beta.client.wave;

import org.swellrt.beta.client.js.Config;
import org.swellrt.beta.client.js.WebSocket;
import org.swellrt.beta.client.js.WebSocket.Function;
import org.swellrt.beta.client.js.event.CloseEvent;
import org.swellrt.beta.client.js.event.Event;
import org.swellrt.beta.client.js.event.MessageEvent;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;

/**
 * Adapt the native browser WebSocket to WaveSocket interface.
 * <p>
 * Implements keep alive logic over the WebSocket.
 *  
 * @author pablojan (pablojan@gmail.com)
 *
 */
public class WaveSocketWS implements WaveSocket {

  private static final String HEARTBEAT_DATA = "[hb]";
  
  private final Scheduler.IncrementalTask keepAliveTask = new Scheduler.IncrementalTask() {

    @Override
    public boolean execute() {
        if (ws.readyState == WebSocket.OPEN) {
          ws.send(HEARTBEAT_DATA);
        } else {
          return false;
        }
        return true;
    }

  };
  
  
  private final String serverUrl;
   
  /** Callback for the websocket */
  private final WaveSocketCallback callback;
  
  private final static String WEBSOCKET_CONTEXT = "socketBAD";
  private final static String SESSION_TOKEN_PARAM = "st";
  
  private WebSocket ws;
  private int heartbeatInterval = 0;
  private boolean connectedAtLeastOnce = false;
  
  
  
  public WaveSocketWS(String serverUrl, String sessionToken, WaveSocketCallback callback) {
    if (serverUrl.charAt(serverUrl.length()-1) != '/')
      serverUrl += "/"+WEBSOCKET_CONTEXT;
    else
      serverUrl += WEBSOCKET_CONTEXT;
    
    if (sessionToken != null) {
      serverUrl += "?"+SESSION_TOKEN_PARAM+"="+sessionToken;
    }
    
    this.serverUrl = serverUrl;
    this.callback = callback;
    
    this.heartbeatInterval = Config.webSocketKeepAliveInterval();
  }
  
  protected void startKeepAlive() {
    if (heartbeatInterval != 0)
      SchedulerInstance.getLowPriorityTimer().scheduleRepeating(keepAliveTask, heartbeatInterval, heartbeatInterval);
  }
  
  protected void endKeepAlive() {
    if (heartbeatInterval != 0)
      SchedulerInstance.getLowPriorityTimer().cancel(keepAliveTask);
  }
  
  @Override
  public void connect() {
    
    try {    
      ws = new WebSocket(serverUrl);
    } catch (Exception e) {
      callback.onError("WebSockets not available ");
      return;
    }
    
    ws.onopen = new WebSocket.Function<Event>() {
      @Override
      public void exec(Event e) {
        connectedAtLeastOnce = true;
        startKeepAlive();
        callback.onConnect();      
      }
    };
        
    ws.onclose = new WebSocket.Function<CloseEvent>() {
      @Override
      public void exec(CloseEvent e) {
        endKeepAlive();        
        callback.onDisconnect();
        
        // CLOSE_PROTOCOL_ERROR, the server is forcing us to raise an error
        if (e.code == 1002) 
          callback.onError(e.reason);
      }
    };
    
    ws.onerror = new Function<Event>() {      
      @Override
      public void exec(Event e) {
        endKeepAlive();
        // Only notify fatal errors, e.g. when we can't connect first time
        if (!connectedAtLeastOnce)
          callback.onError("Error opening WebSocket");
 
      }      
    };
    
    ws.onmessage = new Function<MessageEvent>() {      
      @Override
      public void exec(MessageEvent e) {
        String data = (String) e.data;
        if (data != null) {
          
          if (data.equals(HEARTBEAT_DATA))
            return;
          
          callback.onMessage((String) e.data);
          
        }
        
      }      
    };
    
  }

  @Override
  public void disconnect() {
    ws.close();
  }

  @Override
  public void sendMessage(String message) {
    ws.send(message);
  }

}
