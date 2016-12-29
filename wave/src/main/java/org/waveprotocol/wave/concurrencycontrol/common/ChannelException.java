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

package org.waveprotocol.wave.concurrencycontrol.common;

import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;


/**
 * Represents failure within a communication channel or stack.
 * Original implementation is extended to represent exceptions
 * thrown in the server.
 *
 * @author anorth@google.com (Alex North)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
@SuppressWarnings("serial")
public class ChannelException extends Exception {
  
  private final static String TS = ",";
  
  public static ChannelException deserialize(String message) {

    String[] tokens = message.split(TS);

    if (tokens == null || tokens.length == 0)
      return null;

    ResponseCode code = ResponseCode.UNKNOWN;
    WaveId waveId = null;
    WaveletId waveletId = null;
    String description = "Unknown Excpetion";
    Recoverable isRecoverable = Recoverable.NOT_RECOVERABLE;

    for (int t = 0; t < tokens.length; t++) {

      try {

        switch (t) {

        case 0:
          int intCode = Integer.parseInt(tokens[0]);
          code = ResponseCode.of(intCode);
          break;

        case 1:
          if (!tokens[1].isEmpty())
            waveId = ModernIdSerialiser.INSTANCE.deserialiseWaveId(tokens[1]);
          break;

        case 2:
          if (!tokens[2].isEmpty())
            waveletId = ModernIdSerialiser.INSTANCE.deserialiseWaveletId(tokens[2]);
          break;

        case 3:
          description = tokens[3];
          break;

        case 4:
          isRecoverable = (tokens[4] != null && tokens[4].equals("true") ? Recoverable.RECOVERABLE
              : Recoverable.NOT_RECOVERABLE);
          break;

        }

      } catch (Exception e) {
        continue;
      }
    }

    return new ChannelException(code, description, null, isRecoverable, waveId, waveletId);

  }
  
  private final Recoverable isRecoverable;
  private final WaveId waveId;
  private final WaveletId waveletId;
  private final ResponseCode responseCode;

  /**
   * Creates a new exception
   * @param responseCode ResponseCode identifying the problem
   * @param message description of the failure
   * @param cause exception causing the failure
   * @param isRecoverable whether recovery is possible
   * @param waveId id of the associated wave (may be null)
   * @param waveletId id of the associated wavelet (may be null)
   */
  public ChannelException(ResponseCode responseCode, String message, Throwable cause,
      Recoverable isRecoverable, WaveId waveId, WaveletId waveletId) {
    super(message, cause);
    this.isRecoverable = isRecoverable;
    this.waveId = waveId;
    this.waveletId = waveletId;
    this.responseCode = responseCode;
  }

  /**
   * Simpler constructor for when there is no known error code, usually used
   * when some sanity check failed or other low-level problem occurred
   * (therefore it will be treated as {@link ResponseCode#INTERNAL_ERROR}
   *
   * @see #ChannelException(ResponseCode, String, Throwable, Recoverable, WaveId,
   *      WaveletId)
   */
  public ChannelException(String message, Recoverable isRecoverable) {
    this(ResponseCode.INTERNAL_ERROR, message, null, isRecoverable, null, null);
  }

  /**
   * Whether recovery by reconnection is possible.
   */
  public Recoverable getRecoverable() {
    return isRecoverable;
  }

  /**
   * Gets the associated wave id (may be null).
   */
  public WaveId getWaveId() {
    return waveId;
  }

  /**
   * Gets the associated wavelet id (may be null).
   */
  public WaveletId getWaveletId() {
    return waveletId;
  }

  /**
   * Gets the identified cause
   */
  public ResponseCode getResponseCode() {
    return responseCode;
  }

  @Override
  public String toString() {
    // Space before \n in case some logger swallows the newline.
    return super.toString() + ", \nwaveId: " + waveId + " waveletId: " + waveletId
        + ", isRecoverable: " + isRecoverable + ", responseCode: " + responseCode;
  }
  
  public String serialize() {
    
      return ""+ responseCode.getValue() + TS 
          + (waveId != null ? ModernIdSerialiser.INSTANCE.serialiseWaveId(waveId) : "") + TS 
          + (waveletId != null ? ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletId) : "" ) + TS 
          + getMessage() + TS 
          + (isRecoverable != null && isRecoverable.equals(Recoverable.RECOVERABLE) ? "true" : "false");
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((isRecoverable == null) ? 0 : isRecoverable.hashCode());
    result = prime * result + ((responseCode == null) ? 0 : responseCode.hashCode());
    result = prime * result + ((waveId == null) ? 0 : waveId.hashCode());
    result = prime * result + ((waveletId == null) ? 0 : waveletId.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    ChannelException other = (ChannelException) obj;
    if (isRecoverable != other.isRecoverable)
      return false;
    if (responseCode != other.responseCode)
      return false;
    if (waveId == null) {
      if (other.waveId != null)
        return false;
    } else if (!waveId.equals(other.waveId))
      return false;
    if (waveletId == null) {
      if (other.waveletId != null)
        return false;
    } else if (!waveletId.equals(other.waveletId))
      return false;
    return true;
  }
  
  
    
}
