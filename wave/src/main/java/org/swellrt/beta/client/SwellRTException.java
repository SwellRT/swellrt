package org.swellrt.beta.client;

@SuppressWarnings("serial")
public class SwellRTException extends Exception {

  public SwellRTException(String message) {
    super(message);
  }

  public SwellRTException(String message, Throwable e) {
    super(message, e);
  }
  
}
