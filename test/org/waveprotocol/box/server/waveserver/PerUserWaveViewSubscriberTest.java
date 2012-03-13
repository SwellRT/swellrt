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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gwt.thirdparty.guava.common.collect.Maps;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Collection;
import java.util.Map;

/**
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class PerUserWaveViewSubscriberTest extends TestCase implements TestingConstants {

  private static final HashedVersion BEGIN_VERSION = HashedVersion.unsigned(101L);
  private static final HashedVersion END_VERSION = HashedVersion.unsigned(102L);

  private static final ProtocolWaveletDelta DELTA = ProtocolWaveletDelta.newBuilder()
    .setAuthor(USER)
    .setHashedVersion(CoreWaveletOperationSerializer.serialize(BEGIN_VERSION))
    .addOperation(ProtocolWaveletOperation.newBuilder().setAddParticipant(USER).build()).build();

  private static final DeltaSequence POJO_DELTAS =
      DeltaSequence.of(CoreWaveletOperationSerializer.deserialize(DELTA, END_VERSION, 0L));

  private PerUserWaveViewSubscriber perUserWaveViewSubscriber;

  @Mock WaveMap waveMap;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    perUserWaveViewSubscriber = new PerUserWaveViewSubscriber(waveMap);
  }

  public void testGetPerUserWaveView() throws WaveletStateException {
    LocalWaveletContainer.Factory localFactory = mock(LocalWaveletContainer.Factory.class);

    WaveletNotificationSubscriber notifiee = mock(WaveletNotificationSubscriber.class);

    SettableFuture<ImmutableSet<WaveletId>> lookedupWavelets = SettableFuture.create();
    lookedupWavelets.set(ImmutableSet.of(WAVELET_NAME.waveletId));

    Wave wave =
        new Wave(WAVELET_NAME.waveId, lookedupWavelets, notifiee, localFactory, null,
            WAVELET_NAME.waveId.getDomain());
    Map<WaveId, Wave> waves = Maps.newHashMap();
    waves.put(WAVELET_NAME.waveId, wave);
    when(waveMap.getWaves()).thenReturn(waves);
    ImmutableSet<WaveletId> wavelets = ImmutableSet.of(WAVELET_NAME.waveletId);
    when(waveMap.lookupWavelets(WAVELET_NAME.waveId)).thenReturn(wavelets);

    LocalWaveletContainer c = mock(LocalWaveletContainer.class);
    when(c.hasParticipant(PARTICIPANT)).thenReturn(true);
    when(waveMap.getLocalWavelet(WAVELET_NAME)).thenReturn(c);

    long dummyCreationTime = System.currentTimeMillis();
    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, PARTICIPANT,
        BEGIN_VERSION, dummyCreationTime);

    // The first getPerUserWaveView causes the user's wavelets to be tracked.
    Multimap<WaveId, WaveletId> perUserWavesView =
        perUserWaveViewSubscriber.getPerUserWaveView(PARTICIPANT);
    assertEquals(0, perUserWavesView.size());

    // Mock adding the user to a new wavelet.
    perUserWaveViewSubscriber.waveletUpdate(wavelet, POJO_DELTAS);
    perUserWavesView =
        perUserWaveViewSubscriber.getPerUserWaveView(PARTICIPANT);
    // Verify the user was actually added.
    assertEquals(1, perUserWavesView.size());
    Collection<WaveletId> waveletsPerUser = perUserWavesView.get(WAVELET_NAME.waveId);
    assertNotNull(waveletsPerUser);
    assertEquals(WAVELET_NAME.waveletId, waveletsPerUser.iterator().next());
  }
}
