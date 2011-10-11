// Copyright 2011 Google Inc. All Rights Reserved.

package com.google.wave.api;

import org.waveprotocol.wave.model.id.DualIdSerialiser;
import org.waveprotocol.wave.model.id.IdSerialiser;

/**
 * Serializer for ids transmitted as part of the API.
 *
 * @author anorth@google.com (Alex North)
 */
public final class ApiIdSerializer {

  public static IdSerialiser instance() {
    return DualIdSerialiser.LEGACY;
  }

  private ApiIdSerializer() {
  }
}
