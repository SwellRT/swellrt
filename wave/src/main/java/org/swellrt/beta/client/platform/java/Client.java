package org.swellrt.beta.client.platform.java;

import java.util.Random;
import java.util.Set;

import org.swellrt.beta.client.ServiceConfig;
import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.SessionManager;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.AccountImpl;
import org.swellrt.beta.client.wave.Log;
import org.swellrt.beta.client.wave.RemoteViewServiceMultiplexer;
import org.swellrt.beta.client.wave.StagedWaveLoader;
import org.swellrt.beta.client.wave.WaveFactories;
import org.swellrt.beta.client.wave.WaveLoader;
import org.swellrt.beta.client.wave.ws.WebSocket;
import org.swellrt.beta.common.ModelFactory;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.waveprotocol.wave.client.wave.DiffData.WaveletDiffData;
import org.waveprotocol.wave.client.wave.DiffProvider;
import org.waveprotocol.wave.concurrencycontrol.common.TurbulenceListener;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.common.util.concurrent.FutureCallback;
import com.google.gwt.core.client.Callback;

public class Client {

  private final String serverAddress;

  ServiceContext context;

  public Client(String serverAddress) {
    this.serverAddress = serverAddress;
  }

  public void connect() {

    ModelFactory.instance = null; // TODO to be completed
    ServiceConfig.configProvider = null; // TODO to be completed
    SessionManager sessionMgr = new SessionManager() {

      private Account account;

      @Override
      public void setSession(Account profile) {
        account = profile;
      }

      @Override
      public String getSessionId() {
        return account.getSessionId();
      }

      @Override
      public String getTransientSessionId() {
        return account.getTransientSessionId();
      }

      @Override
      public void removeSession() {

      }

      @Override
      public boolean isSession() {
        return true;
      }

      @Override
      public String getWaveDomain() {
        return account.getDomain();
      }

      @Override
      public String getUserId() {
        return account.getId();
      }

    };
    WaveFactories.loaderFactory = new WaveLoader.Factory() {

      @Override
      public WaveLoader create(WaveId waveId, RemoteViewServiceMultiplexer channel,
          IdGenerator idGenerator, String localDomain, Set<ParticipantId> participants,
          ParticipantId loggedInUser, UnsavedDataListener dataListener,
          TurbulenceListener turbulenceListener, DiffProvider diffProvider) {
        return new StagedWaveLoader(waveId, channel, idGenerator, localDomain, participants,
            loggedInUser, dataListener, turbulenceListener, diffProvider);
      }
    };
    WaveFactories.randomGenerator = new WaveFactories.Random() {

      @Override
      public int nextInt() {
        return new Random().nextInt();
      }
    };

    WaveFactories.protocolMessageUtils = new JavaProtocolMessageUtils();

    WaveFactories.logFactory = new Log.Factory() {

      @Override
      public Log create(Class<? extends Object> clazz) {
        return new ConsoleJavaLog(clazz);
      }
    };

    WaveFactories.websocketFactory = new WebSocket.Factory() {

      @Override
      public WebSocket create() {
        return new JavaWebSocket();
      }
    };

    JavaTimerService timerService = new JavaTimerService();

    WaveFactories.lowPriorityTimer = timerService;
    WaveFactories.mediumPriorityTimer = timerService;
    WaveFactories.highPriorityTimer = timerService;

    context = new ServiceContext(sessionMgr, serverAddress, new DiffProvider.Factory() {

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

    Account accountData = new AccountImpl("fake@local.net");

    context.init(accountData);

    context.getObject(WaveId.of("local.net", "object"), new FutureCallback<SWaveObject>() {

      @Override
      public void onSuccess(SWaveObject result) {
        System.out.println("Object loaded");
      }

      @Override
      public void onFailure(Throwable t) {
        System.err.println("Error " + t.getMessage());
      }

    });

  }

  public void disconnect() {

  }

  public static void main(String[] args) {

    Client client = new Client("http://localhost:9898");
    client.connect();

  }

}
