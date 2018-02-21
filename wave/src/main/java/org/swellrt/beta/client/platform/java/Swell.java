package org.swellrt.beta.client.platform.java;

import java.util.Random;
import java.util.Set;

import org.swellrt.beta.client.DefaultFrontend;
import org.swellrt.beta.client.ServiceConfig;
import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.ServiceDeps;
import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.ServiceSession;
import org.swellrt.beta.client.rest.JsonParser;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.wave.Log;
import org.swellrt.beta.client.wave.RemoteViewServiceMultiplexer;
import org.swellrt.beta.client.wave.StagedWaveLoader;
import org.swellrt.beta.client.wave.WaveDeps;
import org.swellrt.beta.client.wave.WaveLoader;
import org.swellrt.beta.client.wave.ws.WebSocket;
import org.swellrt.beta.model.ModelFactory;
import org.swellrt.beta.model.java.JavaModelFactory;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.wave.DiffData.WaveletDiffData;
import org.waveprotocol.wave.client.wave.DiffProvider;
import org.waveprotocol.wave.concurrencycontrol.common.TurbulenceListener;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gson.Gson;
import com.google.gwt.core.client.Callback;

public class Swell {

  public static ServiceFrontend getService(String serverAddress) {

    Swell c = new Swell(serverAddress);
    c.start();
    return c.getService();

  }

  private final String serverAddress;

  ServiceContext context;
  ServiceFrontend service;

  private ServiceFrontend getService() {
    return service;
  }

  private Swell(String serverAddress) {
    this.serverAddress = serverAddress;
  }

  private void start() {

    ModelFactory.instance = new JavaModelFactory();
    ServiceConfig.configProvider = new JavaServiceConfig();

    WaveDeps.loaderFactory = new WaveLoader.Factory() {

      @Override
      public WaveLoader create(WaveId waveId, RemoteViewServiceMultiplexer channel,
          IdGenerator idGenerator, String localDomain, Set<ParticipantId> participants,
          ParticipantId loggedInUser, UnsavedDataListener dataListener,
          TurbulenceListener turbulenceListener, DiffProvider diffProvider) {
        return new StagedWaveLoader(waveId, channel, idGenerator, localDomain, participants,
            loggedInUser, dataListener, turbulenceListener, diffProvider);
      }
    };
    WaveDeps.intRandomGeneratorInstance = new WaveDeps.IntRandomGenerator() {

      @Override
      public int nextInt() {
        return new Random().nextInt();
      }
    };

    WaveDeps.protocolMessageUtils = new JavaProtocolMessageUtils();

    WaveDeps.logFactory = new Log.Factory() {

      @Override
      public Log create(Class<? extends Object> clazz) {
        return new ConsoleJavaLog(clazz);
      }
    };

    WaveDeps.websocketFactory = new WebSocket.Factory() {

      @Override
      public WebSocket create() {
        return new JavaWebSocket();
      }
    };

    JavaTimerService timerService = new JavaTimerService();

    WaveDeps.lowPriorityTimer = timerService;
    WaveDeps.mediumPriorityTimer = timerService;
    WaveDeps.highPriorityTimer = timerService;

    WaveDeps.json = new JsonParser() {

      Gson gson = new Gson();

      @Override
      public <R, S extends R> R parse(String json, Class<S> implType) {
        R result = gson.fromJson(json, implType);
        return result;
      }

      @Override
      public <O> String serialize(O data) {
        if (data != null)
          return gson.toJson(data);
        return null;
      }

    };

    WaveDeps.sJsonFactory = new SJsonFactoryJava();

    WaveDeps.colorGeneratorInstance = new WaveDeps.ColorGenerator() {

      @Override
      public RgbColor getColor(String id) {
        return RgbColor.WHITE;
      }

    };

    ServiceDeps.serviceSessionFactory = new ServiceSession.Factory() {

      @Override
      public ServiceSession create(Account account) {
        return new JavaServiceSession(account);
      }

      @Override
      public String getWindowId() {
        return JavaServiceSession.WINDOW_ID;
      }
    };

    context = new ServiceContext(serverAddress, new DiffProvider.Factory() {

      @Override
      public DiffProvider get(WaveId waveId) {
        return new DiffProvider() {

          @Override
          public void getDiffs(WaveletId waveletId, String docId, HashedVersion version,
              Callback<WaveletDiffData, Exception> callback) {
            callback.onSuccess(null);
          }

        };
      }
    });

    ServiceDeps.remoteOperationExecutor = new JavaServerOperationExecutor(context);

    service = new DefaultFrontend(context, ServiceDeps.remoteOperationExecutor);
  }



}
