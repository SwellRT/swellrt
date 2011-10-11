// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.wave.api.oauth;

import com.google.wave.api.Wavelet;

/**
 * Interface that handles the rendering in the wave of a login form in the wave 
 * to direct the user to authorize access to the service provider's resources.
 * 
 * @author elizabethford@google.com (Elizabeth Ford)
 */
public interface LoginFormHandler {

  /**
   * Renders a link to the service provider's login page and a confirmation
   * button to press when login is complete.
   * 
   * @param userRecordKey The user id.
   * @param wavelet The wavelet to which the robot is added.
   */
  void renderLogin(String userRecordKey, Wavelet wavelet);
}
