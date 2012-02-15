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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Pojo implementation of RobotSearchDigest.
 *
 * Generated from google-import.proto. Do not edit.
 */
public class RobotSearchDigestImpl implements RobotSearchDigest {
  private String waveId;
  private final List<String> participant = new ArrayList<String>();
  private String title;
  private String snippet;
  private Long lastModifiedMillis;
  private Integer blipCount;
  private Integer unreadBlipCount;
  public RobotSearchDigestImpl() {
  }

  public RobotSearchDigestImpl(RobotSearchDigest message) {
    copyFrom(message);
  }

  @Override
  public void copyFrom(RobotSearchDigest message) {
    setWaveId(message.getWaveId());
    clearParticipant();
    for (String field : message.getParticipant()) {
      addParticipant(field);
    }
    setTitle(message.getTitle());
    setSnippet(message.getSnippet());
    setLastModifiedMillis(message.getLastModifiedMillis());
    setBlipCount(message.getBlipCount());
    setUnreadBlipCount(message.getUnreadBlipCount());
  }

  @Override
  public String getWaveId() {
    return waveId;
  }

  @Override
  public void setWaveId(String value) {
    this.waveId = value;
  }

  @Override
  public List<String> getParticipant() {
    return Collections.unmodifiableList(participant);
  }

  @Override
  public void addAllParticipant(List<String> values) {
    this.participant.addAll(values);
  }

  @Override
  public String getParticipant(int n) {
    return participant.get(n);
  }

  @Override
  public void setParticipant(int n, String value) {
    this.participant.set(n, value);
  }

  @Override
  public int getParticipantSize() {
    return participant.size();
  }

  @Override
  public void addParticipant(String value) {
    this.participant.add(value);
  }

  @Override
  public void clearParticipant() {
    participant.clear();
  }

  @Override
  public String getTitle() {
    return title;
  }

  @Override
  public void setTitle(String value) {
    this.title = value;
  }

  @Override
  public String getSnippet() {
    return snippet;
  }

  @Override
  public void setSnippet(String value) {
    this.snippet = value;
  }

  @Override
  public long getLastModifiedMillis() {
    return lastModifiedMillis;
  }

  @Override
  public void setLastModifiedMillis(long value) {
    this.lastModifiedMillis = value;
  }

  @Override
  public int getBlipCount() {
    return blipCount;
  }

  @Override
  public void setBlipCount(int value) {
    this.blipCount = value;
  }

  @Override
  public int getUnreadBlipCount() {
    return unreadBlipCount;
  }

  @Override
  public void setUnreadBlipCount(int value) {
    this.unreadBlipCount = value;
  }

  /** Provided to subclasses to clear all fields, for example when deserializing. */
  protected void reset() {
    this.waveId = null;
    this.participant.clear();
    this.title = null;
    this.snippet = null;
    this.lastModifiedMillis = null;
    this.blipCount = null;
    this.unreadBlipCount = null;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof RobotSearchDigestImpl) && isEqualTo(o);
  }

  @Override
  public boolean isEqualTo(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof RobotSearchDigest) {
      return RobotSearchDigestUtil.isEqual(this, (RobotSearchDigest) o);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return RobotSearchDigestUtil.getHashCode(this);
  }

}