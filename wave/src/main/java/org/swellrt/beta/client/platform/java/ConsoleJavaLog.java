package org.swellrt.beta.client.platform.java;

import org.swellrt.beta.client.wave.Log;

public class ConsoleJavaLog extends Log {

  public ConsoleJavaLog(Class<? extends Object> clazz) {
    super(clazz);
  }

  @Override
  public void info(String message) {
    if (Level.INFO.equals(level) || Level.DEBUG.equals(level))
      System.err.println("INFO [" + clazz.getSimpleName() + "] " + message);
  }

  @Override
  public void severe(String message) {
    if (Level.SEVERE.equals(level) || Level.INFO.equals(level) || Level.DEBUG.equals(level))
      System.err.println("ERROR [" + clazz.getSimpleName() + "] " + message);
  }

  @Override
  public void severe(String message, Throwable t) {
    if (Level.SEVERE.equals(level) || Level.INFO.equals(level) || Level.DEBUG.equals(level))
      System.err.println(
          "ERROR [" + clazz.getSimpleName() + "] " + message + ", Exception: " + t.getMessage());
  }

  @Override
  public void debug(String message) {
    if (Level.DEBUG.equals(level))
      System.err.println("DEBUG [" + clazz.getSimpleName() + "] " + message);
  }

}
