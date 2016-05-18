package org.swellrt.web;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.RootPanel;

/**
 * The GWT Entry point for the JavaScript Web Client of SwellRT.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WebAPIEntryPoint implements EntryPoint {

  public static native void log(String s) /*-{
    console.log(s);
  }-*/;

  @Override
  public void onModuleLoad() {

    RootPanel.get();

    setupWindowId();
    WebAPI webapi = WebAPI.create(getServerURL());
    injectAPItoWindow(webapi);

    // Force to flow exceptions to the browser
    GWT.setUncaughtExceptionHandler(null);

    // Notify the host page that client is already loaded
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        notifyReady();
      }
    });

  }



  /**
   * Get the sliced server's URL with the protocol and server parts. The URL
   * doesn't end with /
   *
   * @return the base URL of the SwellRT server
   */
  public String getServerURL() {
    String u = GWT.getModuleBaseURL();

    int last = -1;
    for (int i = 0; i < 3; i++) {
      last = u.indexOf("/", last + 1);
    }

    if (last != -1) u = u.substring(0, last);

    return u;
  }

  private native void setupWindowId() /*-{

    try {

      if (!$wnd.sessionStorage) return;

      // Generate a browser window/tab id
      if (!$wnd.sessionStorage.getItem("x-swellrt-window-id")) {

        if (!$wnd.localStorage.getItem("x-swellrt-window-count")) {
          $wnd.localStorage.setItem("x-swellrt-window-count", 0);
        }

        var windowCount = $wnd.localStorage.getItem("x-swellrt-window-count");
        windowCount++;
        $wnd.localStorage.setItem("x-swellrt-window-count", windowCount);

        $wnd.sessionStorage.setItem("x-swellrt-window-id",windowCount);
      }

    } catch (e) {
      console.log("Ignoring session storage: "+e);
    }


  }-*/;

  private native void notifyReady() /*-{

      var handlers = [];

      if ($wnd.SwellRT)
        handlers = $wnd.SwellRT._readyHandlers;

      // set here definite SwellRT object
      // to make it available to handlers
      $wnd.SwellRT = $wnd.__SwellRT;
      delete $wnd.__SwellRT;

      for(var i=0; i < handlers.length; i++)
        handlers[i]();


   }-*/;

  private native void injectAPItoWindow(WebAPI wapi) /*-{
     $wnd.__SwellRT = wapi;
  }-*/;




}
