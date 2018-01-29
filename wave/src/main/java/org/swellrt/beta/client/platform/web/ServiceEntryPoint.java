package org.swellrt.beta.client.platform.web;

import java.util.Set;

import org.swellrt.beta.client.DefaultFrontend;
import org.swellrt.beta.client.ServiceConfig;
import org.swellrt.beta.client.ServiceConfigProvider;
import org.swellrt.beta.client.ServiceConnection;
import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.ServiceDeps;
import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.ServiceLogger;
import org.swellrt.beta.client.ServiceSession;
import org.swellrt.beta.client.platform.js.JsProtocolMessageUtils;
import org.swellrt.beta.client.platform.web.browser.Console;
import org.swellrt.beta.client.platform.web.browser.JSON;
import org.swellrt.beta.client.platform.web.editor.SEditorStatics;
import org.swellrt.beta.client.rest.JsonParser;
import org.swellrt.beta.client.rest.operations.params.Account;
import org.swellrt.beta.client.wave.Log;
import org.swellrt.beta.client.wave.RemoteViewServiceMultiplexer;
import org.swellrt.beta.client.wave.StagedWaveLoader;
import org.swellrt.beta.client.wave.WaveDeps;
import org.swellrt.beta.client.wave.WaveLoader;
import org.swellrt.beta.client.wave.ws.WebSocket;
import org.swellrt.beta.model.ModelFactory;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.common.util.RgbColorPalette;
import org.waveprotocol.wave.client.debug.logger.LogLevel;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.wave.DiffProvider;
import org.waveprotocol.wave.concurrencycontrol.common.TurbulenceListener;
import org.waveprotocol.wave.concurrencycontrol.common.UnsavedDataListener;
import org.waveprotocol.wave.model.id.IdGenerator;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Random;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

/**
 * A GWT entry point for the service front end.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(namespace = "swell", name = "runtime")
public class ServiceEntryPoint implements EntryPoint {

  private static ServiceContext context;
  private static ServiceFrontend service;
  private static PromisableFrontend promisableService;

  @JsMethod(name = "getCallbackable")
  public static ServiceFrontend getCallbackableInstance() {
    return service;
  }

  @JsMethod(name = "get")
  public static PromisableFrontend getPromisableInstance() {
    return promisableService;
  }

  @JsIgnore
  public static ServiceConnection getServiceConnection() {
    return service;
  }

  @JsIgnore
  public static ServiceContext getServiceContext() {
    return context;
  }

  private static String getServerURL() {
    String url = GWT.getModuleBaseURL();

    int c = 3;
    String s = url;
    int index = -1;
    while (c > 0) {
      index = s.indexOf("/", index + 1);
      if (index == -1)
        break;
      c--;
    }

    if (c == 0)
      url = url.substring(0, index);

    return url;
  }

  /**
   * Client apps can register handlers to be notified when SwellRT library is
   * fully functional.
   * <p>
   * See "swellrt.js" file for details.
   */
  private static native void notifyOnLoadHandlers(
    PromisableFrontend sf) /*-{

    if (!$wnd.swell) {
      console.log("Swell object not ready yet! wtf?")
    }

    for(var i in $wnd._lh) {
      $wnd._lh[i](sf);
    }

    delete $wnd._lh;

  }-*/;

  private static native void getEditorConfigProvider() /*-{


    if (!$wnd.__swell_editor_config) {
      $wnd.__swell_editor_config = {};
    }

  }-*/;


  private static native ServiceConfigProvider getConfigProvider() /*-{

      if (!$wnd.__swell_config) {
        $wnd.__swell_config = {};
      }

      return $wnd.__swell_config = {};

  }-*/;



  @JsIgnore
  @Override
  public void onModuleLoad() {

    // Making sure Editor registries are initialized before
    // getting any instance of ContentDocument
    SEditorStatics.initRegistries();

    ModelFactory.instance = new WebModelFactory();

    WaveDeps.logFactory = new Log.Factory() {

      @Override
      public Log create(Class<? extends Object> clazz) {
        return new WebLog(clazz);
      }
    };

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
        return Random.nextInt();
      }
    };

    WaveDeps.protocolMessageUtils = new JsProtocolMessageUtils();

    WaveDeps.websocketFactory = new WebSocket.Factory() {
      @Override
      public WebSocket create() {
        return new WebWebSocket();
      }
    };

    WaveDeps.json = new JsonParser() {

      @Override
      public <T, R extends T> T parse(String json, Class<R> dataType) {
        return JSON.<T> parse(json);
      }

      @Override
      public <O> String serialize(O data) {
        if (data != null) {
          if (data instanceof JavaScriptObject) {
            return JsonUtils.stringify((JavaScriptObject) data);
          }
        }
        return null;
      }

    };

    WaveDeps.sJsonFactory = new SJsonFactoryWeb();

    WaveDeps.lowPriorityTimer = SchedulerInstance.getLowPriorityTimer();
    WaveDeps.mediumPriorityTimer = SchedulerInstance.getMediumPriorityTimer();
    WaveDeps.highPriorityTimer = SchedulerInstance.getHighPriorityTimer();

    WaveDeps.colorGeneratorInstance = new WaveDeps.ColorGenerator() {

      @Override
      public RgbColor getColor(String id) {
        int colorIndex = id.hashCode() % RgbColorPalette.PALETTE.length;
        colorIndex = colorIndex < 0 ? -colorIndex : colorIndex;
        RgbColor colour = RgbColorPalette.PALETTE[colorIndex].get("400");
        return colour;
      }
    };

    ServiceDeps.serviceSessionFactory = new ServiceSession.Factory() {

      @Override
      public ServiceSession create(Account account) {
        return new WebServiceSession(account);
      }

      @Override
      public String getWindowId() {
        return WebServiceSession.WINDOW_ID;
      }
    };

    ServiceConfig.configProvider = getConfigProvider();
    getEditorConfigProvider();

    if (ServiceConfig.captureExceptions() || LogLevel.showErrors()) {

      GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {

        @Override
        public void onUncaughtException(Throwable e) {
          GWT.log("Uncaught Exception", e);
          e.printStackTrace(System.err);
        }
      });

    } else {
      GWT.setUncaughtExceptionHandler(null);
    }


    // Notify the host page that client is already loaded
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {


        context = new ServiceContext(getServerURL(),
            new DiffProvider.Factory() {

              @Override
              public DiffProvider get(WaveId waveId) {
                return new RemoteDiffProvider(waveId, context);
              }
            });

        service = DefaultFrontend.create(context,
            new WebServerOperationExecutor(context),
            new ServiceLogger() {

              @Override
              public void log(String message) {
                Console.log(message);
              }

            });

        promisableService = new PromisableFrontend(service);

        notifyOnLoadHandlers(promisableService);

      }
    });

  }

}
