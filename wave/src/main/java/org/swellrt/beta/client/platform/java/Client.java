package org.swellrt.beta.client.platform.java;

import java.util.Random;
import java.util.Set;

import org.swellrt.beta.client.ServiceConfig;
import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.SessionManager;
import org.swellrt.beta.client.wave.RemoteViewServiceMultiplexer;
import org.swellrt.beta.client.wave.StagedWaveLoader;
import org.swellrt.beta.client.wave.WaveFactories;
import org.swellrt.beta.client.wave.WaveLoader;
import org.swellrt.beta.common.ModelFactory;
import org.swellrt.beta.model.wave.mutable.SWaveObject;
import org.waveprotocol.wave.client.account.ServerAccountData;
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

      private ServerAccountData account;

      @Override
      public void setSession(ServerAccountData profile) {
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

    context.init(new ServerAccountData() {

      @Override
      public String getTransientSessionId() {
        return "xxx";
      }

      @Override
      public String getSessionId() {
        // TODO Auto-generated method stub
        return "yyy";
      }

      @Override
      public String getName() {
        return "Fake";
      }

      @Override
      public String getLocale() {
        return "en";
      }

      @Override
      public String getId() {
        return "fake@local.net";
      }

      @Override
      public String getEmail() {
        return null;
      }

      @Override
      public String getDomain() {
        return "local.net";
      }

      @Override
      public String getAvatarUrl() {
        return null;
      }
    });

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

    Client client = new Client("localhost:9898");
    client.connect();

  }

}
