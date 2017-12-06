package org.swellrt.beta.client.platform.web;

import org.swellrt.beta.client.platform.web.browser.Console;
import org.swellrt.beta.client.wave.Log;

public class WebLog extends Log {

  public WebLog(Class<? extends Object> clazz) {
    super(clazz);
  }

  @Override
  public void info(String message) {
    if (Level.INFO.equals(level) || Level.DEBUG.equals(level))
      Console.log("INFO [" + clazz.getSimpleName() + "] " + message);
  }

  @Override
  public void severe(String message) {
    if (Level.SEVERE.equals(level) || Level.INFO.equals(level) || Level.DEBUG.equals(level))
      Console.log("ERROR [" + clazz.getSimpleName() + "] " + message);
  }

  @Override
  public void severe(String message, Throwable t) {
    if (Level.SEVERE.equals(level) || Level.INFO.equals(level) || Level.DEBUG.equals(level))
      Console
        .log("ERROR [" + clazz.getSimpleName() + "] " + message + ", Exception: " + t.getMessage());
  }

  @Override
  public void debug(String message) {
    if (Level.DEBUG.equals(level))
      Console.log("DEBUG [" + clazz.getSimpleName() + "] " + message);
  }

}
