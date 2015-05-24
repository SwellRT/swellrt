package org.swellrt.android.service;

import org.waveprotocol.wave.common.logging.AbstractLogger.Level;
import org.waveprotocol.wave.common.logging.Logger;

import android.util.Log;

public class AndroidLogger implements Logger {

  private final Level level;
  private final String tag;

  public AndroidLogger(String tag, Level level) {
    this.level = level;
    this.tag = tag;
  }

  private String stringifyLogObject(Object o) {
    if (o instanceof Object[]) {
      StringBuilder builder = new StringBuilder();
      Object[] objects = (Object[]) o;
      for (Object object : objects) {
        builder.append(object.toString());
      }
      return builder.toString();
    } else {
      return o.toString();
    }
  }

  @Override
  public void log(String msg) {

    if (level.equals(Level.ERROR)) {
      Log.w(tag, msg);
    } else if (level.equals(Level.FATAL)) {
      Log.e(tag, msg);
    } else if (level.equals(Level.TRACE)) {
      Log.v(tag, msg);
    } else {
      Log.i(tag, msg);
    }

  }

  protected void logThrowable(String msg, Throwable t) {
    if (level.equals(Level.ERROR)) {
      Log.w(tag, t);
    } else if (level.equals(Level.FATAL)) {
      Log.e(tag, msg, t);
    } else if (level.equals(Level.TRACE)) {
      Log.v(tag, msg, t);
    } else {
      Log.i(tag, msg, t);
    }
  }


  @Override
  public void log(Object... messages) {
    log(stringifyLogObject(messages));
  }

  @Override
  public void logPlainText(String msg) {
    log(msg);
  }

  @Override
  public void logPlainText(String msg, Throwable t) {
    logThrowable(msg, t);
  }

  @Override
  public void logXml(String xml) {
    log(xml);
  }

  @Override
  public void log(String label, Object o) {
    log(label + ": " + stringifyLogObject(o));
  }

  @Override
  public void log(Throwable t) {
    logThrowable("", t);
  }

  @Override
  public void log(String label, Throwable t) {
    logThrowable(label, t);
  }

  @Override
  public void logLazyObjects(Object... objects) {
    log(stringifyLogObject(objects));
  }

  @Override
  public boolean shouldLog() {
    return true;
  }

}
