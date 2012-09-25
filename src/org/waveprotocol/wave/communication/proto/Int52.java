/** Copyright 2011 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
