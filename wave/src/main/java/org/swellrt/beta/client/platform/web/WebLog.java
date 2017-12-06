package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.platform.web.browser.Console;
import org.swellrt.beta.client.wave.Log;

public class WebLog extends Log {

  public WebLog(Class<? extends Object> clazz) {
    super(clazz);
  }

  @Override
  public void info(String message) {
    Console.log("INFO [" + clazz.getSimpleName() + "] " + message);
  }

  @Override
  public void severe(String message) {
    Console.log("ERROR [" + clazz.getSimpleName() + "] " + message);
  }

  @Override
  public void severe(String message, Throwable t) {
    Console
        .log("ERROR [" + clazz.getSimpleName() + "] " + message + ", Exception: " + t.getMessage());
  }

  @Override
  public void debug(String message) {
    Console.log("DEBUG [" + clazz.getSimpleName() + "] " + message);
  }

}
