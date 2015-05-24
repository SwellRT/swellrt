package org.swellrt.android.service;

import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.common.logging.Logger;
import org.waveprotocol.wave.common.logging.LoggerBundle;

public class AndroidLoggerBundle implements LoggerBundle {

  private AndroidLogger traceLogger;
  private AndroidLogger errorLogger;
  private AndroidLogger fatalLogger;

  private final String tag;

  public AndroidLoggerBundle(String tag) {
    this.tag = tag;
  }

  @Override
  public void log(Level level, Object... messages) {

    if (level.equals(Level.ERROR)) {
      error().log(messages);
    } else if (level.equals(Level.TRACE)) {
      trace().log(messages);
    } else if (level.equals(Level.FATAL)) {
      fatal().log(messages);
    }

  }

  @Override
  public Logger trace() {

    if (traceLogger == null)
      traceLogger = new AndroidLogger(tag, Level.TRACE);

    return traceLogger;
  }

  @Override
  public Logger error() {

    if (errorLogger == null)
      errorLogger = new AndroidLogger(tag, Level.ERROR);

    return errorLogger;
  }

  @Override
  public Logger fatal() {

    if (fatalLogger == null)
      fatalLogger = new AndroidLogger(tag, Level.FATAL);

    return fatalLogger;

  }

  @Override
  public boolean isModuleEnabled() {
    return true;
  }

}
