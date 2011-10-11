// Copyright 2011 Google Inc. All Rights Reserved.

package org.waveprotocol.wave.communication.proto;

/**
 * A utility class for getting real field types from *.proto file
 *
 * @author piotrkaleta@google.com (Piotr Kaleta)
 *
 */
public class Int52 {

  public static long int52to64(double value) {
    return (long) value;
  }

  public static double int64to52(long value) {
    return value;
  }
}
