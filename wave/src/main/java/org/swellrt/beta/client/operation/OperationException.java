package org.swellrt.beta.client.operation;

import org.swellrt.beta.client.SwellRTException;

@SuppressWarnings("serial")
public class OperationException extends SwellRTException {

  public OperationException(String message) {
    super(message);
  }
  
  public OperationException(String message, Throwable e) {
    super(message, e);
  }

}
