package org.swellrt.beta.client.wave;

public abstract class Log {

  public static interface Factory {
    Log create(Class<? extends Object> clazz);
  }

  public static Log get(Class<? extends Object> clazz) {
    return WaveFactories.logFactory.create(clazz);
  }

  protected final Class<? extends Object> clazz;

  public Log(Class<? extends Object> clazz) {
    this.clazz = clazz;
  }


  public abstract void info(String message);

  public abstract void severe(String message);

  public abstract void severe(String message, Throwable t);

  public abstract void debug(String message);

}
