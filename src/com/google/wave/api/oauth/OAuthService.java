// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.wave.api.oauth;

import com.google.wave.api.Wavelet;
import com.google.wave.api.oauth.impl.OAuthServiceException;

import java.util.Map;

/**
 * Generic OAuthService to handle OAuth tokens and to form OAuth urls.
 * 
 * @author elizabethford@google.com (Elizabeth Ford)
 * @author kimwhite@google.com (Kimberly White)
 */
public interface OAuthService {
  
  /**
   * Verifies that the user profile contains a request token (i.e. user has
   * logged in). If user profile does not exist or does not contain a request
   * token, fetches a request token and renders the login form so the request
   * token can be signed.
   * 
   * If the user profile contains a request token and "confirmed" is true,
   * exchanges the signed request token with the service provider for an access
   * token.
   * 
   * @param wavelet The wavelet on which the robot resides.
   * @param loginForm the form that handles user authorization in wave.
   * @return boolean True if user is authorized, false if rendering a login form 
   *     to authorize the user is required.
   */
  boolean checkAuthorization(Wavelet wavelet, LoginFormHandler loginForm);
  
  /**
   * Checks if the user is authorized.
   * 
   * @return True if the user has an access token.
   */
  boolean hasAuthorization();
  
  /**
   * Performs HTTP POST to the Service provider.
   * 
   * @param url Service provider url to post data.
   * @param parameters Service provider parameters.
   * @return String of the response message.
   * @throws com.google.wave.api.oauth.impl.OAuthServiceException;
   */
  String post(String url, Map<String, String> parameters) 
      throws OAuthServiceException;
  
  /**
   * Performs HTTP GET from the Service provider.
   * 
   * @param url Service provider url to fetch resources.
   * @param parameters Service provider parameters.
   * @return String of the response message.
   * @throws com.google.wave.api.oauth.impl.OAuthServiceException;
   */
  String get(String url, Map<String, String> parameters) 
      throws OAuthServiceException;
}
