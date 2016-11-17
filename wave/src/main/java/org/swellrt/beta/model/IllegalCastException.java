package org.swellrt.beta.model;

import org.swellrt.beta.client.SwellRTException;

@SuppressWarnings("serial")
public class IllegalCastException extends SwellRTException {

  public IllegalCastException(String msg) {
    super(msg);
  }
}
