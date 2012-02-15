/**
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.waveprotocol.box.waveimport.google;

import java.util.List;

/**
 * Model interface for RobotSearchDigest.
 *
 * Generated from com/google/walkaround/proto/google-import.proto. Do not edit.
 */
public interface RobotSearchDigest {

  /** Does a deep copy from model. */
  void copyFrom(RobotSearchDigest model);

  /**
   * Tests if this model is equal to another object.
   * "Equal" is recursively defined as:
   * <ul>
   * <li>both objects implement this interface,</li>
   * <li>all corresponding primitive fields of both objects have the same value, and</li>
   * <li>all corresponding nested-model fields of both objects are "equal".</li>
   * </ul>
   *
   * This is a coarser equivalence than provided by the equals() methods. Two
   * objects may not be equal() to each other, but may be isEqualTo() each other.
   */
  boolean isEqualTo(Object o);

  /** Returns waveId, or null if hasn't been set. */
  String getWaveId();

  /** Sets waveId. */
  void setWaveId(String waveId);

  /** Returns participant, or null if hasn't been set. */
  List<String> getParticipant();

  /** Adds an element to participant. */
  void addParticipant(String value);

  /** Adds a list of elements to participant. */
  void addAllParticipant(List<String> participant);

  /** Returns the nth element of participant. */
  String getParticipant(int n);

  /** Sets the nth element of participant. */
  void setParticipant(int n, String value);

  /** Returns the length of participant. */
  int getParticipantSize();

  /** Clears participant. */
  void clearParticipant();

  /** Returns title, or null if hasn't been set. */
  String getTitle();

  /** Sets title. */
  void setTitle(String title);

  /** Returns snippet, or null if hasn't been set. */
  String getSnippet();

  /** Sets snippet. */
  void setSnippet(String snippet);

  /** Returns lastModifiedMillis, or null if hasn't been set. */
  long getLastModifiedMillis();

  /** Sets lastModifiedMillis. */
  void setLastModifiedMillis(long lastModifiedMillis);

  /** Returns blipCount, or null if hasn't been set. */
  int getBlipCount();

  /** Sets blipCount. */
  void setBlipCount(int blipCount);

  /** Returns unreadBlipCount, or null if hasn't been set. */
  int getUnreadBlipCount();

  /** Sets unreadBlipCount. */
  void setUnreadBlipCount(int unreadBlipCount);
}