package org.swellrt.beta.client.platform.serverless;

import org.swellrt.beta.client.ServiceConfig;
import org.swellrt.beta.client.ServiceConfigProvider;
import org.swellrt.beta.client.ServiceFrontend;
import org.swellrt.beta.client.platform.web.browser.Console;
import org.swellrt.beta.model.ModelFactory;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

/**
 * A GWT entry point for a mocked service front end that doesn't require a
 * server. For testing purposes and to play with Swell data API without a
 * server. <br>
 * <p>
 * To properly expose this service in window.swell, the GWT script must be
 * loaded after the window.swell var exists. See file 'swell.js'.
 * </p>
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(namespace = "swell", name = "runtime")
public class ServerlessEntryPoint implements EntryPoint {

  private static ServiceFrontend service;

  @JsMethod(name = "get")
  public static ServiceFrontend getServiceInstance() {
    return service;
  }

  /**
   * Client apps can register handlers to be notified when SwellRT library is
   * fully functional.
   * <p>
   * See "swellrt.js" file for details.
   */
  private static native void notifyOnLoadHandlers(
    ServiceFrontend sf) /*-{

    if (!$wnd.swell) {
      console.log("Swell object not ready yet! wtf?")
    }

    for(var i in $wnd._lh) {
      $wnd._lh[i](sf);
    }

    delete $wnd._lh;

  }-*/;

  private static native ServiceConfigProvider getConfigProvider() /*-{

    if (!$wnd.__swell_config) {
      $wnd.__swell_config = {};
    }

    return $wnd.__swell_config = {};

  }-*/;

  private static native void getEditorConfigProvider() /*-{

    if (!$wnd.__swell_editor_config) {
      $wnd.__swell_editor_config = {};
    }

  }-*/;

  @JsIgnore
  @Override
  public void onModuleLoad() {

    // Model factory is used in remote Waves
    ModelFactory.instance = null;
    ServiceConfig.configProvider = getConfigProvider();
    getEditorConfigProvider();

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

    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {

        service = new ServerlessFrontend();
        notifyOnLoadHandlers(service);

      }
    });

  }




}
