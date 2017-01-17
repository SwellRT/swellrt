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

package org.swellrt.beta.client.wave;

import org.waveprotocol.box.common.comms.ProtocolWaveletUpdate;
import org.waveprotocol.wave.communication.gwt.JsonHelper;
import org.waveprotocol.wave.communication.gwt.JsonMessage;
import org.waveprotocol.wave.concurrencycontrol.common.ChannelException;
import org.waveprotocol.wave.concurrencycontrol.common.Recoverable;
import org.waveprotocol.wave.concurrencycontrol.common.ResponseCode;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;

/**
 * Callback for a wave websocket.
 *
 * @author arb@google.com (Anthony Baxter)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public interface WaveWebSocketCallback {
  
  /**
   * Hand made wrapper class for messages of type RpcFinished.
   * This is a workaround, because real implementation of this protobuf wrapper 
   * is not visible for client. 
   */
  public static final class RpcFinished  extends JsonMessage {
    
    protected RpcFinished() {
      super();
    }
    
    public boolean hasFailed() {
      return JsonHelper.getPropertyAsBoolean(this, "1");
    }
    
    public boolean hasErrorText() {
      return JsonHelper.hasProperty(this, "2");
    }
    
    public String getErrorText() {
      return JsonHelper.getPropertyAsString(this, "2");
    }

    public ChannelException getChannelException() {
      ChannelException e = null;
      if (hasErrorText()) {       
        e = ChannelException.deserialize(getErrorText());        
      }      
      return e;
    }
    
  }
  
  

  void onWaveletUpdate(ProtocolWaveletUpdate message);
  
  void onFinished(RpcFinished message);
}
