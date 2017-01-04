package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;

@SuppressWarnings("serial")
public class IllegalCastException extends SException {

  public IllegalCastException(String m) {
    super(ILLEGAL_CAST,null,m);
    
  }

  
}
