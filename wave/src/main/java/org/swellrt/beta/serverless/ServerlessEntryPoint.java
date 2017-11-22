package org.swellrt.beta.serverless;

import org.swellrt.beta.client.js.Console;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsType;

/**
 * A GWT entry point for a mocked service front end that doesn't require a
 * server. For testing purposes and to play with Swell data API without a
 * server.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(namespace = "swell", name = "Serverless")
public class ServerlessEntryPoint implements EntryPoint {


  @JsMethod(name = "get")
  public static ServiceServerless getInstance() {
    return ServiceServerless.create();
  }

  @JsIgnore
  @Override
  public void onModuleLoad() {


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

      }
    });

  }




}
