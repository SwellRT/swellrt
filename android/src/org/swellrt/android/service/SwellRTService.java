package org.swellrt.android.service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import org.swellrt.android.service.WaveWebSocketClient.ConnectionListener;
import org.swellrt.android.service.wave.client.concurrencycontrol.MuxConnector.Command;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.TextType;
import org.swellrt.model.generic.TypeIdGenerator;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.concurrencycontrol.wave.CcDataDocumentImpl;
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
import android.util.Log;

public class SwellRTService extends Service implements UnsavedDataListener {


  public interface SwellRTServiceCallback {

    public void onStartSessionSuccess(String session);

    public void onStartSessionFail(String error);

    public void onCreate(Model model);

    public void onOpen(Model model);

    public void onClose(boolean everythingCommitted);

    public void onUpdate(int inFlightSize, int notAckedSize, int unCommitedSize);

    public void onError(String message);

    public void onDebugInfo(String message);

  }

  public final static String TAG = "SwellRTService";


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

    Log.d(TAG, "SwellRT Service onCreated()");

  }



  @Override
  public IBinder onBind(Intent intent) {

    Log.d(TAG, "SwellRT Service onBind() for " + intent.toString());
    return mBinder;
  }


  @Override
  public void onDestroy() {

    Log.d(TAG, "SwellRT Service onDestroy()");
    // Close and clean all comms
    stopSession();
  }

  //
  // Public service interface
  //

  public void startSession(String host, String username, String password)
      throws MalformedURLException, InvalidParticipantAddress {


    serverUrl = new URL(host);
    participantId = ParticipantId.of(username);

    new LoginTask().execute(serverUrl.toString(), participantId.toString(), password);

  }

  public boolean isSessionStarted() {
    return sessionId != null;
  }


  private void openWebSocket(String waveDomain, final String sessionId) {

    Preconditions.checkArgument(sessionId != null, "Session id is null");
    Preconditions.checkArgument(waveDomain != null, "Wave domain is null");

    this.webSocketUrl = serverUrl.getProtocol() + "://" + serverUrl.getHost() + ":"
        + serverUrl.getPort() + "/atmosphere";

    this.sessionId = sessionId;
    this.waveDomain = waveDomain;

    idGenerator = new IdGeneratorImpl(waveDomain, new Seed() {

      @Override
      public String get() {

        return sessionId.substring(0, 5);
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


  public void openModel(String modelId) {

    final WaveRef waveRef = WaveRef.of(WaveId.deserialise(modelId));

    if (waveStore.containsKey(waveRef)) {
      mCallback.onOpen(waveStore.get(waveRef).getSecond());
    }


    final WaveLoader loader = WaveLoader.create(false, waveRef, channel, participantId,
        Collections.<ParticipantId> emptySet(), idGenerator, null, timer);

    loader.init(new Command() {

      @Override
      public void execute() {

        WaveContext wave = loader.getWaveContext();
        Model model = Model.create(wave, waveDomain, participantId, false, idGenerator);

        waveStore.put(waveRef, new Pair<WaveLoader, Model>(loader, model));

        mCallback.onOpen(model);

      }
    });

  }

  public String createModel() {

    WaveId newWaveId = typeIdGenerator.newWaveId();
    final WaveRef waveRef = WaveRef.of(newWaveId);

    final WaveLoader loader = WaveLoader.create(true, waveRef, channel, participantId,
        Collections.<ParticipantId> emptySet(), idGenerator, null, timer);

    loader.init(new Command() {

      @Override
      public void execute() {

        WaveContext wave = loader.getWaveContext();
        Model model = Model.create(wave, waveDomain, participantId, true, idGenerator);

        waveStore.put(waveRef, new Pair<WaveLoader, Model>(loader, model));

        mCallback.onCreate(model);
      }

    });


    return waveRef.getWaveId().serialise();
  }


  public void closeModel(String modelId) {

    WaveRef waveRef = WaveRef.of(WaveId.deserialise(modelId));
    Preconditions.checkState(waveStore.containsKey(waveRef), "Trying to close a not opened Model");

    WaveLoader loader = waveStore.get(waveRef).getFirst();
    loader.close();

    waveStore.remove(waveRef);
  }

  public Model getModel(String modelId) {
    WaveRef waveRef = WaveRef.of(WaveId.deserialise(modelId));
    if (!waveStore.containsKey(waveRef))
      return null;

    return waveStore.get(waveRef).getSecond();
  }

  public CcDataDocumentImpl getReadableDocument(TextType text) {
    WaveRef waveRef = WaveRef.of(text.getModel().getWaveId());
    if (!waveStore.containsKey(waveRef))
      return null;

    return waveStore.get(waveRef).getFirst().getDocumentRegistry()
        .getBlipDocument(text.getModel().getWaveletIdString(), text.getDocumentId());
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


  @Override
  public void onClose(boolean everythingCommited) {
    mCallback.onClose(everythingCommited);
  }

  @Override
  public void onUpdate(UnsavedDataInfo unsavedDataInfo) {
    mCallback.onUpdate(unsavedDataInfo.inFlightSize(),
        unsavedDataInfo.estimateUnacknowledgedSize(), unsavedDataInfo.estimateUncommittedSize());
    Log.d(TAG, "OnUpdate(): " + unsavedDataInfo.getInfo());
  }

}
