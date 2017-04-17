package org.swellrt.beta.client.wave;

import org.swellrt.beta.client.js.WebSocket;
import org.swellrt.beta.client.js.WebSocket.Function;
import org.swellrt.beta.client.wave.WaveSocket;

/**
 * Adapt the native browser WebSocket to WaveSocket interface 
 *  
 * @author pablojan (pablojan@gmail.com)
 *
 */
public class WaveSocketWS implements WaveSocket {

  private final String serverUrl;
   
  /** Callback for the websocket */
  private final WaveSocketCallback callback;
  
  private WebSocket ws;
  
  public WaveSocketWS(String serverUrl, WaveSocketCallback callback) {
    this.serverUrl = serverUrl;
    this.callback = callback;
  }
  
  @Override
  public void connect() {
    ws = new WebSocket(serverUrl);
    
    ws.onopen = new WebSocket.Function<Void>() {
      @Override
      public void exec(Void o) {
          callback.onConnect();      
      }
    };
        
    ws.onclose = new WebSocket.Function<Void>() {
      @Override
      public void exec(Void o) {
        callback.onDisconnect();        
      }
    };
    
    ws.onerror = new Function<String>() {      
      @Override
      public void exec(String o) {
        callback.onError();
      }      
    };
    
    ws.onmessage = new Function<String>() {      
      @Override
      public void exec(String message) {
        callback.onMessage(message);
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
