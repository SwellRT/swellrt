/*
 * Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.box.waveimport.google.oauth;

/**
 * Thrown when a new OAuth token is needed, either because the current token has
 * been revoked, or because we have no token in the first place.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class NeedNewOAuthTokenException extends RuntimeException {

  public NeedNewOAuthTokenException() {
  }

  public NeedNewOAuthTokenException(String message) {
    super(message);
  }

  public NeedNewOAuthTokenException(Throwable cause) {
    super(cause);
  }

  public NeedNewOAuthTokenException(String message, Throwable cause) {
    super(message, cause);
  }

}
