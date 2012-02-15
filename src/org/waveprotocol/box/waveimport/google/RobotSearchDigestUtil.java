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

import java.util.Iterator;
import java.util.List;

/**
 * Compares {@link RobotSearchDigest}s for equality.
 *
 * Generated from google-import.proto. Do not edit.
 */
public final class RobotSearchDigestUtil {
  private RobotSearchDigestUtil() {
  }

  /** Returns true if m1 and m2 are structurally equal. */
  public static boolean isEqual(RobotSearchDigest m1, RobotSearchDigest m2) {
    if (!m1.getWaveId().equals(m2.getWaveId())) return false;
    if (!m1.getParticipant().equals(m2.getParticipant())) return false;
    if (!m1.getTitle().equals(m2.getTitle())) return false;
    if (!m1.getSnippet().equals(m2.getSnippet())) return false;
    if (m1.getLastModifiedMillis() != m2.getLastModifiedMillis()) return false;
    if (m1.getBlipCount() != m2.getBlipCount()) return false;
    if (m1.getUnreadBlipCount() != m2.getUnreadBlipCount()) return false;
    return true;
  }

  /** Returns true if m1 and m2 are equal according to isEqual. */
  public static boolean areAllEqual(List<? extends RobotSearchDigest> m1,
  List<? extends RobotSearchDigest> m2) {
    if (m1.size() != m2.size()) return false;
    Iterator<? extends RobotSearchDigest> i1 = m1.iterator();
    Iterator<? extends RobotSearchDigest> i2 = m2.iterator();
    while (i1.hasNext()) {
      if (!isEqual(i1.next(), i2.next())) return false;
    }
    return true;
  }

  /** Returns a structural hash code of message. */
  public static int getHashCode(RobotSearchDigest message) {
    int result = 1;
    result = (31 * result) + message.getWaveId().hashCode();
    result = (31 * result) + message.getParticipant().hashCode();
    result = (31 * result) + message.getTitle().hashCode();
    result = (31 * result) + message.getSnippet().hashCode();
    result = (31 * result) + Long.valueOf(message.getLastModifiedMillis()).hashCode();
    result = (31 * result) + Integer.valueOf(message.getBlipCount()).hashCode();
    result = (31 * result) + Integer.valueOf(message.getUnreadBlipCount()).hashCode();
    return result;
  }

}