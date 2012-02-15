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

import com.google.common.base.Preconditions;
import com.google.inject.servlet.RequestScoped;

import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.logging.Logger;

import javax.annotation.Nullable;

/**
 * Holds information about the user we are acting as.  Many helper classes
 * receive a {@code UserContext} for making OAuth requests or for attributing
 * actions (for example, incoming deltas) to the current user.
 *
 * For each request, this starts out unintialized, since we have no uniform way
 * of identifying the user.  Authentication filters like
 * {@link InteractiveAuthFilter} and {@link RpcAuthFilter} implement different
 * means of identifying the user and populate the {@code UserContext}
 * accordingly, including reading OAuth credentials from persistent storage.
 *
 * Servlets like {@link OAuthCallbackHandler} that need to manipulate
 * credentials in special ways populate the {@link UserContext} directly.
 *
 * Attempting to call a getter method while the corresponding information is not
 * present will result in an exception.  This makes it easy to tell when
 * a servlet is not using the right authentication filter.
 *
 * This class is not thread-safe; if we ever use more than one thread for a
 * single request, we'll have to think about thread safety.
 *
 * This class is structurally similar to {@link AccountStore.Record} but plays a
 * very different role -- {@code UserContext} is mutable to allow authentication
 * filters (and other classes that handle login) to pass information about the
 * current user to other classes that need it, while {@code AccountStore.Record}
 * is an immutable in-memory representation of a record in {@link AccountStore}.
 *
 * @author ohler@google.com (Christian Ohler)
 */
@RequestScoped
public class UserContext {

  @SuppressWarnings("unused")
  private static final Logger log = Logger.getLogger(UserContext.class.getName());

  // At some point, we might want to refactor this to allow the auth filters to
  // perform retrieval of the OAuth credentials lazily -- many servlets don't
  // need credentials (or need them only in some cases), and the redundant
  // datastore read is undesirable.  However, once we cache credentials in
  // memcache, we will merely be doing a redundant memcache lookup, which is
  // less of a problem.  So implementing this laziness is not urgent.

  @Nullable private StableUserId userId = null;
  @Nullable private ParticipantId participantId = null;
  @Nullable private OAuthCredentials oAuthCredentials = null;

  public UserContext() {
  }

  public boolean hasUserId() {
    return userId != null;
  }

  public StableUserId getUserId() {
    Preconditions.checkState(hasUserId(), "No userId: %s", this);
    return userId;
  }

  public UserContext setUserId(@Nullable StableUserId userId) {
    this.userId = userId;
    return this;
  }

  public boolean hasParticipantId() {
    return participantId != null;
  }

  public ParticipantId getParticipantId() {
    Preconditions.checkState(hasParticipantId(), "No participantId: %s", this);
    return participantId;
  }

  public UserContext setParticipantId(@Nullable ParticipantId participantId) {
    this.participantId = participantId;
    return this;
  }

  public boolean hasOAuthCredentials() {
    return oAuthCredentials != null;
  }

  public OAuthCredentials getOAuthCredentials() {
    Preconditions.checkState(hasOAuthCredentials(), "No oAuthCredentials: %s", this);
    return oAuthCredentials;
  }

  public UserContext setOAuthCredentials(@Nullable OAuthCredentials oAuthCredentials) {
    this.oAuthCredentials = oAuthCredentials;
    return this;
  }

  @Override public String toString() {
    return "UserContext("
        + userId + ", "
        + participantId + ", "
        + oAuthCredentials
        + ")";
  }

}
