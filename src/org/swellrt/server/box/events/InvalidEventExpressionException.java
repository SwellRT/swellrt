package org.swellrt.server.box.events;

@SuppressWarnings("serial")
public class InvalidEventExpressionException extends Exception {

  public InvalidEventExpressionException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidEventExpressionException(Throwable cause) {
    super(cause);
  }

  public InvalidEventExpressionException(String message) {
    super(message);
  }
}
