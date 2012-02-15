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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;

import java.io.Serializable;

/**
 * A stable user identifier: a string that uniquely identifies a user and
 * remains the same even if e.g. the e-mail address changes, and will not be
 * re-used even if an account is deleted.
 *
 * @author ohler@google.com (Christian Ohler)
 */
public class StableUserId implements Serializable {

  private final String id;

  public StableUserId(String id) {
    this.id = checkNotNull(id, "Null id");
  }

  public String getId() {
    return id;
  }

  @Override public String toString() {
    return "StableUserId(" + id + ")";
  }

  @Override public final boolean equals(Object o) {
    if (o == this) { return true; }
    if (!(o instanceof StableUserId)) { return false; }
    StableUserId other = (StableUserId) o;
    return Objects.equal(id, other.id);
  }

  @Override public final int hashCode() {
    return Objects.hashCode(id);
  }

}
