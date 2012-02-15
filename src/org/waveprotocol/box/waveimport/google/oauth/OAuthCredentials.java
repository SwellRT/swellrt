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

import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

/**
 * OAuth2 credentials for a given user.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class OAuthCredentials {

  private final String refreshToken;
  private final String accessToken;

  public OAuthCredentials(String refreshToken, String accessToken) {
    Preconditions.checkNotNull(refreshToken, "Null refreshToken");
    Preconditions.checkNotNull(accessToken, "Null accessToken");
    this.refreshToken = refreshToken;
    this.accessToken = accessToken;
  }

  public String getRefreshToken() {
    return refreshToken;
  }

  public String getAccessToken() {
    return accessToken;
  }

  @Override public String toString() {
    return "OAuthCredentials(" + refreshToken + ", " + accessToken + ")";
  }

  @Override public final boolean equals(Object o) {
    if (o == this) { return true; }
    if (!(o instanceof OAuthCredentials)) { return false; }
    OAuthCredentials other = (OAuthCredentials) o;
    return Objects.equal(refreshToken, other.refreshToken)
        && Objects.equal(accessToken, other.accessToken);
  }

  @Override public final int hashCode() {
    return Objects.hashCode(refreshToken, accessToken);
  }

}
