package org.swellrt.beta.client.wave;

public abstract class Log {

  public enum Level {
    SEVERE, INFO, DEBUG, NONE
  }

  public static interface Factory {
    Log create(Class<? extends Object> clazz);
  }

  public static Log get(Class<? extends Object> clazz) {
    return WaveDeps.logFactory.create(clazz);
  }

  protected final Class<? extends Object> clazz;

  protected Level level = Level.SEVERE;

  public Log(Class<? extends Object> clazz) {
    this.clazz = clazz;
  }

  public void setLevel(Level level) {
    this.level = level;
  }

  public abstract void info(String message);

  public abstract void severe(String message);

  public abstract void severe(String message, Throwable t);

  public abstract void debug(String message);

}
