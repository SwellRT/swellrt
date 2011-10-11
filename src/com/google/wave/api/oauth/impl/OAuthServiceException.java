// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.wave.api.oauth.impl;

/**
 * A checked exception thrown by the Java robot oauth library.
 * 
 * @author kimwhite@google.com (Kimberly White)
 */
public class OAuthServiceException extends Exception {

  /*
   * Class constructor.
   */
  public OAuthServiceException() {
  }
  
  /*
   * Overloaded constructor.
   * 
   * @param message The exception message.
   */
  public OAuthServiceException(String message) {
    super(message);
  }
  
  /*
   * Overloaded constructor.
   * 
   * @param cause The exception cause.
   */
  public OAuthServiceException(Throwable cause) {
    super(cause);
  }
  
  /*
   * Overloaded constructor. 
   * 
   * @param message The exception message.
   * @param cause The exception cause.
   */
  public OAuthServiceException(String message, Throwable cause) {
    super(message, cause);
  }
}
