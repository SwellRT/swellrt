package org.swellrt.beta.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.operation.data.ProfileData;
import org.swellrt.beta.model.SObject;
import org.swellrt.beta.model.remote.SObjectRemote;
import org.swellrt.beta.wave.transport.RemoteViewServiceMultiplexer;
import org.swellrt.beta.wave.transport.WaveLoader;
import org.swellrt.beta.wave.transport.WaveSocket;
import org.swellrt.beta.wave.transport.WaveWebSocketClient;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Random;

/**
 * Instances of this class represent the runtime state of the service provided by
 * the SwellRT client.
 * <p>
 * 
 *  
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ServiceContext {

  public static final String SWELL_DATAMODEL_VERSION = "1.0";
  public static final String WAVEID_NAMESPACE_PREFIX = "s";
  
  // TODO move to utility class
  static final char[] WEB64_ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
      .toCharArray();
  
  static final int WAVE_ID_SEED_LENGTH = 10;
  
  private static String getRandomBase64(int length) {
    StringBuilder result = new StringBuilder(length);
    int bits = 0;
    int bitCount = 0;
    while (result.length() < length) {
      if (bitCount < 6) {
        bits = Random.nextInt();
        bitCount = 32;
      }
      result.append(WEB64_ALPHABET[bits & 0x3F]);
      bits >>= 6;
      bitCount -= 6;
    }
    return result.toString();
  }
  
  public interface ObjectCallback {
    
    public void onReady(SObject object);
    
    public void onFailure(Exception e);
    
  }
  
  
  private interface WaveCallback {
    
    public void onReady(WaveLoader wave);
    
    public void onFailure(Exception e);
    
  }
  
  private Map<WaveId, SObjectRemote> objectRegistry = new HashMap<WaveId, SObjectRemote>();
  
  private IdGenerator legacyIdGenerator;
  private SessionManager sessionManager;
  private boolean sessionCookieAvailable = false;
  
  private String httpAddress = "http://localhost:9898";
  private String websocketAddress = null;
  
  private WaveWebSocketClient waveWebsocket = null; 
  private RemoteViewServiceMultiplexer waveServiceMux = null;
  
  public ServiceContext(SessionManager sessionManager) {
    this.sessionManager = sessionManager;
  }
 
  protected void closeWebsocket() {
    if (waveWebsocket != null) {
      waveWebsocket.disconnect(false);
      waveWebsocket = null;
      waveServiceMux = null;
    }
  }
  

  
  protected void setupIdGenerator() {
    final String seed = getRandomBase64(WAVE_ID_SEED_LENGTH);
    this.legacyIdGenerator = 
        new IdGeneratorImpl(sessionManager.getWaveDomain(), new IdGeneratorImpl.Seed() {
          @Override
          public String get() {
            return seed;
          }          
        });
  }
  
  
  /**
   * @return HTTP server address, e.g. http://swellrt.acme.com:8080
   */
  public String getHTTPAddress() {
    return httpAddress;
  }
  
  /**
   * @return  Websocket server address, e.g. ws://swellrt.acme.com:8080
   */
  public String getWebsocketAddress() {

    if (websocketAddress == null) {
      websocketAddress = httpAddress + "/";

      if (websocketAddress.startsWith("http://"))
        websocketAddress = websocketAddress.replace("http://", "ws://");
      else if (websocketAddress.startsWith("https://"))
        websocketAddress = websocketAddress.replace("https://", "wss://");
    }
    return websocketAddress;
  }
  
  
  /**
   * @return true iff the HTTP client sends the session cookie to the server.
   */
  public boolean isSessionCookieAvailable() {
    return sessionCookieAvailable;
  }
  
  public void setSessionCookieAvailability(boolean isEnabled) {
    sessionCookieAvailable = isEnabled;
  }
  
  /**
   * Initializes the service context, resetting first if it is necessary
   * and setting a new session.
   * @param profile
   */
  public void init(ProfileData profile) {
    reset();
    sessionManager.setSession(profile);
    setupIdGenerator();
  }
 
  public String getWindowId() {
    return sessionManager.getWindowId();
  }
  
  public String getSessionId() {
    return sessionManager.getSessionId();
  }
  
  /**
   * Clean up the internal state of this context.
   * This will normally happen on a session close
   */
  public void reset() {    
    // TODO clean text editor, object registry...
    closeWebsocket();
    sessionManager.removeSession();
    
  }
  
  public boolean isSession() {
    return sessionManager.isSession();
  }
  
  public WaveId generateWaveId() {
    return WaveId.of(sessionManager.getWaveDomain(), legacyIdGenerator.newId(WAVEID_NAMESPACE_PREFIX));
  }
  
  
  private void getWave(WaveRef waveRef, WaveCallback callback) {
    Preconditions.checkState(sessionManager.isSession(), "Session is not ready");
    Preconditions.checkState(waveServiceMux != null, "Wave View service is not ready");

    WaveLoader wave = new WaveLoader(waveRef, waveServiceMux, legacyIdGenerator,
        sessionManager.getWaveDomain(), Collections.<ParticipantId> emptySet(),
        ParticipantId.ofUnsafe(sessionManager.getUserId()), null);

    if (wave.isLoaded()) {
      callback.onReady(wave);
    } else {

      try {

        wave.load(new Command() {
          @Override
          public void execute() {
            callback.onReady(wave);
          }
        });

      } catch (RuntimeException e) {
        callback.onFailure(e);
      }
    }
  }
  
  /**
   * Returns a SObject instance supported by a Wave.
   * @param waveId
   * @param callback
   */
  public void getObject(WaveId waveId, ObjectCallback callback) {
    
    WaveCallback waveCallback = new WaveCallback() {
      
      @Override
      public void onReady(WaveLoader wave) {        
        SObjectRemote object = 
            SObjectRemote.inflateFromWave(legacyIdGenerator, sessionManager.getWaveDomain(), wave.getWave());       
        
        objectRegistry.put(waveId, object);
        callback.onReady(object);
      }
      
      @Override
      public void onFailure(Exception e) {
        callback.onFailure(e);
      }
    };
    
    
    if (waveWebsocket != null) {
      if (objectRegistry.containsKey(waveId)) {
        callback.onReady(objectRegistry.get(waveId));
        return;
      } else {
        getWave(WaveRef.of(waveId), waveCallback);
        return;
      }
      
    } else {
      openWaveSocket(new WaveSocket.WaveSocketStartCallback() {
        
        @Override
        public void onSuccess() {
          getWave(WaveRef.of(waveId), waveCallback);
        }

        @Override
        public void onFailure() {
          callback.onFailure(new SwellRTException("Error opening Websocket"));
        }        
      });
    } 
    
  }
  
  /**
   * Open a websocket anyway and bind it to Wave protocol infrastructure. 
   * Don't check for already open websocket.
   * <p>
   * This method must be call after session is started
   * @param callback
   */
  private void openWaveSocket(WaveSocket.WaveSocketStartCallback callback) {
        
    waveWebsocket = new WaveWebSocketClient(getWebsocketAddress(), SWELL_DATAMODEL_VERSION);
    waveWebsocket.connect(sessionManager.getSessionToken(), new WaveSocket.WaveSocketStartCallback() {

      @Override
      public void onSuccess() {
        waveServiceMux = new RemoteViewServiceMultiplexer(waveWebsocket, sessionManager.getUserId());
        callback.onSuccess();
      }

      @Override
      public void onFailure() {
        callback.onFailure();
      }
      
    });
    
  }
}
