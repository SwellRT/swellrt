package org.swellrt.android.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.swellrt.android.service.WaveWebSocketClient.ConnectionListener;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TypeIdGenerator;
import org.waveprotocol.wave.model.document.WaveContext;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.IdGeneratorImpl;
import org.waveprotocol.wave.model.id.IdGeneratorImpl.Seed;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;

public class SwellRTService extends Service {


  public interface SwellRTServiceCallback {

    public void onStartSessionSuccess(String session);

    public void onStartSessionFail(String error);

    public void onOpen();

    public void onClose(boolean everythingCommitted);

    public void onUpdate(int inFlightSize, int notAckedSize, int unCommitedSize);

    public void onError(String message);

    public void onDebugInfo(String message);

  }




  public class SwellRTBinder extends Binder {

    public SwellRTService getService(SwellRTServiceCallback callback) {
      SwellRTService.this.mCallback = callback;
      return SwellRTService.this;
    }

  };

  private SwellRTServiceCallback mCallback;
  private SwellRTBinder mBinder = new SwellRTBinder();



  private URL serverUrl;
  private String webSocketUrl; // TODO dont' build socket url in this class
  private String waveDomain;

  private String sessionId;
  private ParticipantId participantId;
  private IdGenerator idGenerator;
  private TypeIdGenerator typeIdGenerator;

  private WaveWebSocketClient webSocketClient;
  private RemoteViewServiceMultiplexer channel;

  private Map<WaveRef, Pair<WaveLoader, Model>> waveStore;
  private Timer timer;

  private class LoginTask extends AsyncTask<String, Void, String> {

    @Override
    protected String doInBackground(String... params) {

      String sessionId = null;
      WaveHttpLogin login = new WaveHttpLogin(params[0], params[1], params[2]);
      sessionId = login.execute();
      return sessionId;
    }

    @Override
    protected void onPostExecute(String result) {

      if (result != null) {

        openWebSocket(participantId.getDomain(), result);

      } else {

        mCallback.onStartSessionFail("");

      }

    }

  }


  @Override
  public void onCreate() {

    waveStore = new HashMap<WaveRef, Pair<WaveLoader, Model>>();
    timer = new Timer("WaveServiceTimer");

  }

  @Override
  public int onStartCommand(Intent intent, int flags, int startId) {
    return START_STICKY;

  }

  @Override
  public IBinder onBind(Intent intent) {
    return mBinder;
  }

  //
  // Public service interface
  //

  public void startSession(String host, String username, String password) throws MalformedURLException {


    serverUrl = new URL(host);

    try {
      participantId = ParticipantId.of(username);
    } catch (InvalidParticipantAddress e) {

      mCallback.onError(e.getMessage());
      return;
    }

    new LoginTask().execute(serverUrl.toString(), participantId.toString(), password);

  }




  private void openWebSocket(String waveDomain, final String sessionId) {

    Preconditions.checkArgument(sessionId != null, "Session id is null");
    Preconditions.checkArgument(waveDomain != null, "Wave domain is null");

    this.webSocketUrl = serverUrl.getProtocol() + "://" + serverUrl.getHost() + ":"
        + serverUrl.getPort() + "/atmosphere";

    this.sessionId = sessionId;

    idGenerator = new IdGeneratorImpl(waveDomain, new Seed() {

      @Override
      public String get() {

        return sessionId;
      }
    });

    typeIdGenerator = TypeIdGenerator.get(idGenerator);

    webSocketClient = new WaveWebSocketClient(webSocketUrl, sessionId);
    webSocketClient.connect(new ConnectionListener() {

      @Override
      public void onDisconnect() {
        mCallback.onDebugInfo("Websocket disconnected");
      }

      @Override
      public void onConnect() {
        mCallback.onStartSessionSuccess(sessionId);
      }

      @Override
      public void onReconnect() {
        mCallback.onDebugInfo("Websocket reconnected");
      }
    });

    channel = new RemoteViewServiceMultiplexer(webSocketClient, participantId.getAddress());

  }


  public Model openModel(String modelId) {

    WaveRef waveRef = WaveRef.of(WaveId.deserialise(modelId));

    if (waveStore.containsKey(waveRef)) {
      return waveStore.get(waveRef).getSecond();
    }

    WaveLoader loader = WaveLoader.create(false, waveRef, channel, participantId,
        Collections.<ParticipantId> emptySet(), idGenerator, null, timer);

    WaveContext wave = loader.getWaveContext();
    Model model = Model.create(wave, waveDomain, participantId, false, idGenerator);

    waveStore.put(waveRef, new Pair<WaveLoader, Model>(loader, model));

    return model;
  }

  public Model createModel() {

    WaveId newWaveId = typeIdGenerator.newWaveId();
    WaveRef waveRef = WaveRef.of(newWaveId);

    WaveLoader loader = WaveLoader.create(true, waveRef, channel, participantId,
        Collections.<ParticipantId> emptySet(), idGenerator, null, timer);

    WaveContext wave = loader.getWaveContext();
    Model model = Model.create(wave, waveDomain, participantId, true, idGenerator);

    waveStore.put(waveRef, new Pair<WaveLoader, Model>(loader, model));

    return model;
  }


  public void closeModel(String waveId) {

    WaveRef waveRef = WaveRef.of(WaveId.deserialise(waveId));
    Preconditions.checkState(waveStore.containsKey(waveRef), "Trying to close a not opened Model");

    WaveLoader loader = waveStore.get(waveRef).getFirst();
    loader.close();

    waveStore.remove(waveRef);
  }


  private void closeWebSocket() {

    waveStore = null;
    idGenerator = null;

    channel = null;
    webSocketClient = null;

  }

  public void stopSession() {

    for (WaveRef waveRef : waveStore.keySet()) {
      WaveLoader loader = waveStore.get(waveRef).getFirst();
      loader.close();
    }
    waveStore.clear();

    closeWebSocket();

    sessionId = null;
    participantId = null;
    waveDomain = null;
  }

}
