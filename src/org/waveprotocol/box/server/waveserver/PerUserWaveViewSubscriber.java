/**
 * Copyright 2012 Apache Wave
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
 *
 */

package org.waveprotocol.box.server.waveserver;

import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.waveserver.WaveBus.Subscriber;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.RemoveParticipant;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Listens on the {@link WaveBus} and keeps the per user waves view up to date.
 *
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class PerUserWaveViewSubscriber implements Subscriber {

  private static final Log LOG = Log.get(PerUserWaveViewSubscriber.class);

  /**
   * The period of time in minutes the per user waves view should be actively
   * kept up to date after last access.
   */
  private static final int PER_USER_WAVES_VIEW_CACHE_MINUTES = 5;

  /** The computing map that holds wave viev per each online user.*/
  public ConcurrentMap<ParticipantId, Multimap<WaveId, WaveletId>> explicitPerUserWaveViews;

  @Inject
  public PerUserWaveViewSubscriber(final WaveMap waveMap) {
    // Let the view expire if it not accessed for some time.
    explicitPerUserWaveViews =
        new MapMaker().expireAfterAccess(PER_USER_WAVES_VIEW_CACHE_MINUTES, TimeUnit.MINUTES)
            .makeComputingMap(new Function<ParticipantId, Multimap<WaveId, WaveletId>>() {

              @Override
              public Multimap<WaveId, WaveletId> apply(final ParticipantId user) {
                Multimap<WaveId, WaveletId> userView = HashMultimap.create();

                // Create initial per user waves view by looping over all waves
                // in the waves store.
                // After that the view is maintained up to date continuously in
                // the subscriber.waveletUpdate method until the user logs of
                // and the key is expired.
                // On the next login the waves view will be rebuild.
                ExceptionalIterator<WaveId, WaveServerException> waveIds = waveMap.getWaveIds();
                try {
                  while (waveIds.hasNext()) {
                    WaveId waveId = waveIds.next();
                    ImmutableSet<WaveletId> waveletIds = waveMap.lookupWavelets(waveId);
                    for (WaveletId waveletId : waveletIds) {
                      WaveletContainer c = waveMap.getLocalWavelet(WaveletName.of(waveId, waveletId));
                      try {
                        if (!c.hasParticipant(user)) {
                          continue;
                        }
                        // Add this wave to the user view.
                        userView.put(waveId, waveletId);
                      } catch (WaveletStateException e) {
                        LOG.warning("Failed to access wavelet " + c.getWaveletName(), e);
                      }
                    }
                  }
                } catch (WaveletStateException e) {
                  LOG.severe(String.format("Failed to initialise waves view for user %s", user.getAddress()), e);
                } catch (WaveServerException e) {
                  LOG.severe(String.format("Failed to initialise waves view for user %s", user.getAddress()), e);
                }
                LOG.info("Initalized waves view for user: " + user.getAddress()
                    + ", number of waves in view: " + userView.size());
                return userView;
              }
            });
  }

  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, DeltaSequence deltas) {
    WaveletId waveletId = wavelet.getWaveletId();
    // Find whether participants where added/removed and update the views
    // accordingly.
    for (TransformedWaveletDelta delta : deltas) {
      for (WaveletOperation op : delta) {
        if (op instanceof AddParticipant) {
          ParticipantId user = ((AddParticipant) op).getParticipantId();
          // Check first if we need to update views for this user.
          if (explicitPerUserWaveViews.containsKey(user)) {
            Multimap<WaveId, WaveletId> perUserView = explicitPerUserWaveViews.get(user);
            WaveId waveId = wavelet.getWaveId();
            if (!perUserView.containsEntry(waveId, waveletId)) {
              perUserView.put(waveId, waveletId);
              LOG.fine("Added wavelet: " + WaveletName.of(waveId, waveletId)
                  + " to the view of user: " + user.getAddress());
            }
          }
        } else if (op instanceof RemoveParticipant) {
          ParticipantId user = ((RemoveParticipant) op).getParticipantId();
          if (explicitPerUserWaveViews.containsKey(user)) {
            Multimap<WaveId, WaveletId> perUserView = explicitPerUserWaveViews.get(user);
            WaveId waveId = wavelet.getWaveId();
            if (perUserView.containsEntry(waveId, waveletId)) {
              perUserView.remove(waveId, waveletId);
              LOG.fine("Removed wavelet: " + WaveletName.of(waveId, waveletId)
                  + " from the view of user: " + user.getAddress());
            }
          }
        }
      }
    }
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version) {
    // No op.
  }

  /**
   * Returns the per user waves view.
   */
  public Multimap<WaveId, WaveletId> getPerUserWaveView(ParticipantId user) {
    return explicitPerUserWaveViews.get(user);
  }
}