package org.swellrt.beta.client;

import org.swellrt.beta.client.js.Config;
import org.swellrt.beta.client.js.Console;
import org.swellrt.beta.client.js.PromisableServiceFrontend;
import org.swellrt.beta.client.js.SessionManagerJs;

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
@JsType(namespace = "swellrt", name = "runtime")
public class ServiceFrontendEntryPoint implements EntryPoint {

  private static ServiceContext context;
  private static ServiceFrontend callbackableFrontend;
  private static PromisableServiceFrontend promisableFrontend;

  @JsMethod(name = "getCallbackable")
  public static ServiceFrontend getCallbackableInstance() {

    if (callbackableFrontend == null)
      callbackableFrontend = ServiceFrontend.create(context);

    return callbackableFrontend;
  }

  @JsMethod(name = "get")
  public static PromisableServiceFrontend getPromisableInstance() {

    if (promisableFrontend == null)
      promisableFrontend = new PromisableServiceFrontend(getCallbackableInstance());

    return promisableFrontend;
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
  private static native void procOnReadyHandlers(
      PromisableServiceFrontend sf) /*-{

                                    if (!$wnd.swellrt) {
                                    console.log("swellrt object not ready yet! wtf?")
                                    }

                                    for(var i in $wnd._lh) {
                                    $wnd._lh[i](sf);
                                    }

                                    delete $wnd._lh;

                                    }-*/;

  @JsIgnore
  @Override
  public void onModuleLoad() {

    if (Config.getCaptureExceptions()) {

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

    ServiceFrontendEntryPoint.context = new ServiceContext(SessionManagerJs.create(),
        getServerURL());
    // Notify the host page that client is already loaded
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {

        procOnReadyHandlers(getPromisableInstance());

      }
    });

  }

}
