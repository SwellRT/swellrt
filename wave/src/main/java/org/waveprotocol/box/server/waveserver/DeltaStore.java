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

package org.waveprotocol.box.server.waveserver;

import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.io.Closeable;
import java.util.Collection;

/**
 * Stores wavelet deltas.
 *
 * @author soren@google.com (Soren Lassen)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public interface DeltaStore extends WaveletStore<DeltaStore.DeltasAccess> {

  /**
   * Wavelet snapshot and resulting version together.
   * 
   * @author pablojan@gmail.com (Pablo Ojanguren)
   */
  interface Snapshot {
    
    /** Gets the wavelet snapshot */
    public WaveletData getWaveletData();
  }
  
  /**
   * Accesses the delta history for a wavelet.
   * Permits reading historical deltas and appending deltas to the history.
   * 
   * Optionally, it can provide service to load and store snapshots. 
   */
  interface DeltasAccess extends WaveletDeltaRecordReader, Closeable {
    /**
     * Blocking call to append deltas to the end of the delta history.
     * If the call returns normally (doesn't throw an exception), then
     * the deltas have been successfully and "durably" stored, that is,
     * the method forces the data to disk.
     *
     * @param deltas contiguous deltas, beginning from the DeltaAccess object's
     *        end version. It is the caller's responsibility to ensure that
     *        everything matches up (applied and transformed deltas in the
     *        records match, that the hashes are correctly computed, etc).
     * @throws PersistenceException if anything goes wrong with the underlying
     *         storage.
     */
    void append(Collection<WaveletDeltaRecord> deltas) throws PersistenceException;
    
    /**
     * Loads the last snapshot of the wavelet.
     * 
     * @return the wavelet data object
     * @throws PersistenceException if anything goes wrong with the underlying
     *         storage.
     */
    Snapshot loadSnapshot() throws PersistenceException;
    
    /**
     * Stores a wavelet snapshot as the last one.  
     * 
     * @param waveletData the wavelet data
     * @throws PersistenceException if anything goes wrong with the underlying
     *         storage.
     */
    void storeSnapshot(WaveletData waveletData) throws PersistenceException;
    
  }
}
