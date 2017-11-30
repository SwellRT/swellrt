package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.DefaultFrontend;
import org.swellrt.beta.client.ServiceConfig;
import org.swellrt.beta.client.ServiceConfigProvider;
import org.swellrt.beta.client.ServiceContext;
import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.ServiceLogger;
import org.swellrt.beta.client.platform.web.browser.Console;
import org.swellrt.beta.common.ModelFactory;
import org.waveprotocol.wave.client.wave.DiffProvider;
import org.waveprotocol.wave.model.id.WaveId;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;

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

    ModelFactory.instance = new WebModelFactory();
    ServiceConfig.configProvider = getConfigProvider();
    getEditorConfigProvider();

    if (ServiceConfig.captureExceptions()) {

      GWT.setUncaughtExceptionHandler(new GWT.UncaughtExceptionHandler() {

        @Override
        public void onUncaughtException(Throwable e) {
          Console.log("Uncaught Exception: " + e.getMessage());
          String string = "";
          for (StackTraceElement element : e.getStackTrace()) {
            string += element + "\n";
          }
          Console.log("Trace: ");
          Console.log(string);

        }
      });

    } else {
      GWT.setUncaughtExceptionHandler(null);
    }


    // Notify the host page that client is already loaded
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {


        context = new ServiceContext(WebSessionManager.create(), getServerURL(),
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
