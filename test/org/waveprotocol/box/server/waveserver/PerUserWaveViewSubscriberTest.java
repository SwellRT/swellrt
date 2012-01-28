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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.box.server.util.testing.TestingConstants;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletOperation;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.WaveletData;

import java.util.Iterator;

/**
 * @author yurize@apache.org (Yuri Zelikov)
 */
public class PerUserWaveViewSubscriberTest extends TestCase implements TestingConstants {

  private static final HashedVersion BEGIN_VERSION = HashedVersion.unsigned(101L);
  private static final HashedVersion END_VERSION = HashedVersion.unsigned(102L);

  private static final ProtocolWaveletDelta DELTA = ProtocolWaveletDelta.newBuilder()
    .setAuthor(USER)
    .setHashedVersion(CoreWaveletOperationSerializer.serialize(BEGIN_VERSION))
    .addOperation(ProtocolWaveletOperation.newBuilder().setNoOp(true).build()).build();

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
    Iterator<WaveId> inner = ImmutableList.of(WAVELET_NAME.waveId).iterator();
    ExceptionalIterator<WaveId, WaveServerException> iter= ExceptionalIterator.FromIterator.create(inner);
    when(waveMap.getWaveIds()).thenReturn(iter);
    ImmutableSet<WaveletId> wavelets = ImmutableSet.of(WAVELET_NAME.waveletId);
    when(waveMap.lookupWavelets(WAVELET_NAME.waveId)).thenReturn(wavelets);

    LocalWaveletContainer c = mock(LocalWaveletContainer.class);
    when(c.hasParticipant(PARTICIPANT)).thenReturn(true);
    when(waveMap.getLocalWavelet(WAVELET_NAME)).thenReturn(c);

    long dummyCreationTime = System.currentTimeMillis();
    WaveletData wavelet = WaveletDataUtil.createEmptyWavelet(WAVELET_NAME, PARTICIPANT,
        BEGIN_VERSION, dummyCreationTime);
    perUserWaveViewSubscriber.waveletUpdate(wavelet, POJO_DELTAS);
    Multimap<WaveId, WaveletId> perUserWavesView = perUserWaveViewSubscriber.getPerUserWaveView(PARTICIPANT);

    assertNotNull(perUserWavesView);
    assertEquals(1, perUserWavesView.size());
  }
}
