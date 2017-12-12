package org.swellrt.beta.client.platform.java;

import java.util.Random;
import java.util.Set;

import org.swellrt.beta.client.DefaultFrontend;
import org.swellrt.beta.client.ServiceConfig;
import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.ServiceLogger;
import org.swellrt.beta.client.SessionManager;
import org.swellrt.beta.client.rest.JsonParser;
import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.rest.operations.params.CredentialImpl;
import org.swellrt.beta.client.rest.operations.params.ObjectIdImpl;
import org.swellrt.beta.client.wave.Log;
import org.swellrt.beta.client.wave.RemoteViewServiceMultiplexer;
import org.swellrt.beta.client.wave.StagedWaveLoader;
import org.swellrt.beta.client.wave.WaveFactories;
import org.swellrt.beta.client.wave.WaveLoader;
import org.swellrt.beta.client.wave.ws.WebSocket;
import org.swellrt.beta.common.ModelFactory;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SObject;
import org.swellrt.beta.model.SPrimitive;
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

public class Client {

  public static ServiceFrontend get(String serverAddress) {

    Client c = new Client(serverAddress);
    c.start();
    return c.getService();

  }

  private final String serverAddress;

  ServiceContext context;
  ServiceFrontend service;

  private ServiceFrontend getService() {
    return service;
  }

  private Client(String serverAddress) {
    this.serverAddress = serverAddress;
  }

  private void start() {

    ModelFactory.instance = new JavaModelFactory(); // TODO to be completed
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

    WaveFactories.json = new JsonParser() {

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

    service = new DefaultFrontend(context, new JavaServerOperationExecutor(context),
        new ServiceLogger() {

          @Override
          public void log(String message) {
            System.out.println(message);
          }

        });

    /*
     * context.getObject(WaveId.of("local.net", "object"), new
     * FutureCallback<SWaveObject>() {
     *
     * @Override public void onSuccess(SWaveObject result) {
     * System.out.println("Object loaded"); }
     *
     * @Override public void onFailure(Throwable t) {
     * System.err.println("Error " + t.getMessage()); }
     *
     * });
     *
     */

  }

  public void clear() {

  }

  public static void main(String[] args) {

    ServiceFrontend service = Client.get("http://localhost:9898");

    service.login(new CredentialImpl("tam@local.net", "tam", true),
        new ServiceOperation.Callback<Account>() {

          @Override
          public void onError(SException exception) {
            System.err.println(exception.getMessage());

          }

          @Override
          public void onSuccess(Account response) {
            System.out.println("Login success " + response.getId());

            service.open(new ObjectIdImpl("local.net/form-demo-default"),
                new ServiceOperation.Callback<SObject>() {

                  @Override
                  public void onError(SException exception) {
                    System.err.println("Error open " + exception.getMessage());
                  }

                  @Override
                  public void onSuccess(SObject response) {
                    try {
                      System.out.println(
                          "Object loaded -> " + SPrimitive.asString(response.node("form.address")));
                    } catch (SException e) {
                      e.printStackTrace();
                    }

                  }

                });

          }
        });

  }

}
