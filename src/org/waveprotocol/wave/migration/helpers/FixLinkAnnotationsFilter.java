/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.wave.migration.helpers;

import org.waveprotocol.wave.client.common.util.WaveRefConstants;
import org.waveprotocol.wave.model.document.operation.Nindo.NindoCursor;
import org.waveprotocol.wave.model.document.operation.NindoCursorDecorator;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.waveref.InvalidWaveRefException;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.jvm.JavaWaverefEncoder;

/**
 * Renames both "link/manual" and "link/wave" annotations to just "link".
 *
 * In cases where they would conflict, link/manual's value wins. Also,
 * link/wave's wave id is converted to a wave://waveref URI format.
 *
 * Also, replaces all instances of "waveid://" with simply "wave://",
 * and fixes waveids to be wave refs. This also applies to link/auto:
 * it is not renamed, but its values are still normalized.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class FixLinkAnnotationsFilter extends NindoCursorDecorator {

  static final String OLD_MANUAL = "link/manual";
  static final String OLD_WAVE = "link/wave";
  static final String AUTO = "link/auto";
  static final String NEW = "link";
  static final String OLD_PREFIX = "waveid://";

  private boolean manualOpen;
  private boolean waveOpen;
  private String currentManual;
  private String currentWave;

  public FixLinkAnnotationsFilter(NindoCursor target) {
    super(target);
  }

  @Override
  public void begin() {
    currentManual = null;
    currentWave = null;
    manualOpen = false;
    waveOpen = false;
    target.begin();
  }

  @Override
  public void finish() {
    target.finish();
    assert currentManual == null;
    assert currentWave == null;
    assert manualOpen == false;
    assert waveOpen == false;
  }

  @Override
  public void startAnnotation(String key, String value) {
    if (OLD_MANUAL.equals(key)) {
      manualOpen = true;
      currentManual = normalize(value);
      startLink();
    } else if (OLD_WAVE.equals(key)) {
      try {
        currentWave = linkValueFromWaveValue(value);
      } catch (InvalidWaveRefException e) {
        // Discard invalid links.
        closeWave();
        return;
      }
      waveOpen = true;
      if (!manualOpen) {
        startLink();
      }
    } else if (AUTO.equals(key)) {
      super.startAnnotation(key, normalize(value));
    } else {
      super.startAnnotation(key, value);
    }
  }

  @Override
  public void endAnnotation(String key) {
    if (OLD_MANUAL.equals(key)) {
      manualOpen = false;
      currentManual = null;
      if (waveOpen) {
        startLink();
      } else {
        endLink();
      }
    } else if (OLD_WAVE.equals(key)) {
      closeWave();
    } else {
      super.endAnnotation(key);
    }
  }

  void closeWave() {
    if (waveOpen) {
      waveOpen = false;
      currentWave = null;
      if (!manualOpen) {
        endLink();
      }
    }
  }

  void startLink() {
    assert manualOpen || waveOpen;
    super.startAnnotation(NEW, manualOpen ? currentManual : currentWave);
  }

  void endLink() {
    assert !manualOpen && !waveOpen && (currentManual == null) && (currentWave == null);
    super.endAnnotation(NEW);
  }

  public static String normalize(String oldHref) {
    if (oldHref != null && oldHref.startsWith(OLD_PREFIX)) {
      try {
        return linkValueFromWaveValue(oldHref.substring(OLD_PREFIX.length()));
      } catch (InvalidWaveRefException e) {
        return null;
      }
    } else {
      return oldHref;
    }
  }

  public static String linkValueFromWaveValue(String rawString) throws InvalidWaveRefException {
    if (rawString == null) {
      return null;
    }
    WaveRef ref;
    try {
      ref = WaveRef.of(WaveId.checkedDeserialise(rawString));
    } catch (InvalidIdException e) {
      // Let's try decoding it as a wave ref instead
      ref = JavaWaverefEncoder.decodeWaveRefFromPath(rawString);
    }

    return WaveRefConstants.WAVE_URI_PREFIX + JavaWaverefEncoder.encodeToUriPathSegment(ref);
  }
}
