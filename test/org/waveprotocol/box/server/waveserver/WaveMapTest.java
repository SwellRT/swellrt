/**
 * Copyright 2010 Google Inc.
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
import com.google.common.collect.Ordering;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gwt.thirdparty.guava.common.collect.Lists;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.common.CoreWaveletOperationSerializer;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.persistence.memory.MemoryDeltaStore;
import org.waveprotocol.wave.federation.Proto.ProtocolSignedDelta;
import org.waveprotocol.wave.federation.Proto.ProtocolWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.AddParticipant;
import org.waveprotocol.wave.model.operation.wave.WaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperationContext;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionZeroFactoryImpl;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.ParticipantIdUtil;
import org.waveprotocol.wave.model.wave.data.ObservableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveViewData;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.Executor;

/**
 * @author josephg@gmail.com (Joseph Gentle)
 * @author soren@google.com (Soren Lassen)
 */
public class WaveMapTest extends TestCase {
  private static final HashedVersionFactory V0_HASH_FACTORY =
      new HashedVersionZeroFactoryImpl(new IdURIEncoderDecoder(new JavaUrlCodec()));

  private static final String DOMAIN = "example.com";
  private static final WaveId WAVE_ID = WaveId.of(DOMAIN, "abc123");
  private static final WaveletId WAVELET_ID = WaveletId.of(DOMAIN, "conv+root");
  private static final WaveletName WAVELET_NAME = WaveletName.of(WAVE_ID, WAVELET_ID);

  private static final ParticipantId USER1 = ParticipantId.ofUnsafe("user1@" + DOMAIN);
  private static final ParticipantId USER2 = ParticipantId.ofUnsafe("user2@" + DOMAIN);

  private static final WaveletOperationContext CONTEXT =
      new WaveletOperationContext(USER1, 1234567890, 1);
  

  /** Sorts search result in ascending order by LMT. */
  static final Comparator<WaveViewData> ASCENDING_DATE_COMPARATOR =
      new Comparator<WaveViewData>() {
        @Override
        public int compare(WaveViewData arg0, WaveViewData arg1) {
          long lmt0 = computeLmt(arg0);
          long lmt1 = computeLmt(arg1);
          return Long.signum(lmt0 - lmt1);
        }

        private long computeLmt(WaveViewData arg0) {
          long lmt = -1;
          for (ObservableWaveletData wavelet : arg0.getWavelets()) {
            // Skip non conversational wavelets.
            if (!IdUtil.isConversationalId(wavelet.getWaveletId())) {
              continue;
            }
            lmt = lmt < wavelet.getLastModifiedTime() ? wavelet.getLastModifiedTime() : lmt;
          }
          return lmt;
        }
      };

  /** Sorts search result in descending order by LMT. */
  static final Comparator<WaveViewData> DESCENDING_DATE_COMPARATOR =
      new Comparator<WaveViewData>() {
        @Override
        public int compare(WaveViewData arg0, WaveViewData arg1) {
          return -ASCENDING_DATE_COMPARATOR.compare(arg0, arg1);
        }
      };
      
  /** Sorts search result in ascending order by creation time. */
  static final Comparator<WaveViewData> ASC_CREATED_COMPARATOR = new Comparator<WaveViewData>() {
    @Override
    public int compare(WaveViewData arg0, WaveViewData arg1) {
      long time0 = computeCreatedTime(arg0);
      long time1 = computeCreatedTime(arg1);
      return Long.signum(time0 - time1);
    }

    private long computeCreatedTime(WaveViewData arg0) {
      long creationTime = -1;
      for (ObservableWaveletData wavelet : arg0.getWavelets()) {
        creationTime =
            creationTime < wavelet.getCreationTime() ? wavelet.getCreationTime() : creationTime;
      }
      return creationTime;
    }
  };
  
  
  /** Sorts search result in descending order by creation time. */
  static final Comparator<WaveViewData> DESC_CREATED_COMPARATOR = new Comparator<WaveViewData>() {
    @Override
    public int compare(WaveViewData arg0, WaveViewData arg1) {
      return -ASC_CREATED_COMPARATOR.compare(arg0, arg1);
    }
  };
  
  /** Sorts search result in ascending order by author. */
  static final Comparator<WaveViewData> ASC_CREATOR_COMPARATOR = new Comparator<WaveViewData>() {
    @Override
    public int compare(WaveViewData arg0, WaveViewData arg1) {
      ParticipantId author0 = computeAuthor(arg0);
      ParticipantId author1 = computeAuthor(arg1);
      return author0.compareTo(author1);
    }

    private ParticipantId computeAuthor(WaveViewData wave) {
      ParticipantId author = null;
      for (ObservableWaveletData wavelet : wave.getWavelets()) {
        if (IdUtil.isConversationRootWaveletId(wavelet.getWaveletId())) {
          author = wavelet.getCreator();
        }
      }
      assert author != null : "Cannot find author for the wave: " + wave.getWaveId().serialise();
      return author;
    }
  };
  
  /** Sorts search result in descending order by author. */
  static final Comparator<WaveViewData> DESC_CREATOR_COMPARATOR = new Comparator<WaveViewData>() {
    @Override
    public int compare(WaveViewData arg0, WaveViewData arg1) {
      return -ASC_CREATOR_COMPARATOR.compare(arg0, arg1);
    }
  };

  private static WaveletOperation addParticipantToWavelet(ParticipantId user) {
    return new AddParticipant(CONTEXT, user);
  }

  @Mock private WaveletNotificationDispatcher notifiee;
  @Mock private RemoteWaveletContainer.Factory remoteWaveletContainerFactory;

  private DeltaAndSnapshotStore waveletStore;
  private WaveMap waveMap;

  @Override
  protected void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    final DeltaStore deltaStore = new MemoryDeltaStore();
    final Executor persistExecutor = MoreExecutors.sameThreadExecutor();
    LocalWaveletContainer.Factory localWaveletContainerFactory =
        new LocalWaveletContainer.Factory() {
          @Override
          public LocalWaveletContainer create(WaveletNotificationSubscriber notifiee,
              WaveletName waveletName, String domain) {
            WaveletState waveletState;
            try {
              waveletState = DeltaStoreBasedWaveletState.create(deltaStore.open(waveletName),
                  persistExecutor);
            } catch (PersistenceException e) {
              throw new RuntimeException(e);
            }
            return new LocalWaveletContainerImpl(waveletName, notifiee,
                Futures.immediateFuture(waveletState), DOMAIN);
          }
        };

    waveletStore = mock(DeltaAndSnapshotStore.class);
    waveMap =
        new WaveMap(waveletStore, notifiee, notifiee, localWaveletContainerFactory,
            remoteWaveletContainerFactory, "example.com");
  }

  public void testWaveMapStartsEmpty() throws WaveServerException {
    assertFalse(waveMap.getWaveIds().hasNext());
  }

  public void testWavesStartWithNoWavelets() throws WaveletStateException, PersistenceException {
    when(waveletStore.lookup(WAVE_ID)).thenReturn(ImmutableSet.<WaveletId>of());
    assertNull(waveMap.getLocalWavelet(WAVELET_NAME));
    assertNull(waveMap.getRemoteWavelet(WAVELET_NAME));
  }

  public void testWaveAvailableAfterLoad() throws PersistenceException, WaveServerException {
    when(waveletStore.getWaveIdIterator()).thenReturn(eitr(WAVE_ID));
    waveMap.loadAllWavelets();

    ExceptionalIterator<WaveId, WaveServerException> waves = waveMap.getWaveIds();
    assertTrue(waves.hasNext());
    assertEquals(WAVE_ID, waves.next());
  }

  public void testWaveletAvailableAfterLoad() throws WaveletStateException, PersistenceException {
    when(waveletStore.getWaveIdIterator()).thenReturn(eitr(WAVE_ID));
    when(waveletStore.lookup(WAVE_ID)).thenReturn(ImmutableSet.<WaveletId>of(WAVELET_ID));
    waveMap.loadAllWavelets();

    assertNotNull(waveMap.getLocalWavelet(WAVELET_NAME));
  }

  public void testGetOrCreateCreatesWavelets() throws WaveletStateException, PersistenceException {
    when(waveletStore.lookup(WAVE_ID)).thenReturn(ImmutableSet.<WaveletId>of());
    LocalWaveletContainer wavelet = waveMap.getOrCreateLocalWavelet(WAVELET_NAME);
    assertSame(wavelet, waveMap.getLocalWavelet(WAVELET_NAME));
  }

  public void testSearchEmptyInboxReturnsNothing() {
    Collection<WaveViewData> results = waveMap.search(USER1, "in:inbox", 0, 20);

    assertEquals(0, results.size());
  }

  public void testSearchInboxReturnsWaveWithExplicitParticipant() throws Exception {
    submitDeltaToNewWavelet(WAVELET_NAME, USER1, addParticipantToWavelet(USER2));

    Collection<WaveViewData> results = waveMap.search(USER2, "in:inbox", 0, 20);

    assertEquals(1, results.size());
    assertEquals(WAVELET_NAME.waveId, results.iterator().next().getWaveId());
  }

  public void testSearchInboxDoesNotReturnWaveWithoutUser() throws Exception {
    submitDeltaToNewWavelet(WAVELET_NAME, USER1, addParticipantToWavelet(USER1));

    Collection<WaveViewData> results = waveMap.search(USER2, "in:inbox", 0, 20);
    assertEquals(0, results.size());
  }
  
  public void testSearchWaveReturnsWaveWithImplicitParticipant() throws Exception {
    ParticipantId sharedDomainParticipantId =
        ParticipantIdUtil.makeUnsafeSharedDomainParticipantId(DOMAIN);
    WaveletName waveletName =
      WaveletName.of(WaveId.of(DOMAIN, String.valueOf(1)), WAVELET_ID);
    // Implicit participant in this wave.
    submitDeltaToNewWavelet(waveletName, USER1,
        addParticipantToWavelet(sharedDomainParticipantId));
    waveletName = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(2)), WAVELET_ID);
    // Explicit participant in this wave.
    submitDeltaToNewWavelet(waveletName, USER1, addParticipantToWavelet(USER2));

    Collection<WaveViewData> results = waveMap.search(USER2, "", 0, 20);
    // Should return both waves.
    assertEquals(2, results.size());
  }

  public void testSearchAllReturnsWavesOnlyWithSharedDomainUser() throws Exception {
    WaveletName waveletName =
      WaveletName.of(WaveId.of(DOMAIN, String.valueOf(1)), WAVELET_ID);
    submitDeltaToNewWavelet(waveletName, USER1, addParticipantToWavelet(USER1));
    waveletName = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(2)), WAVELET_ID);
    submitDeltaToNewWavelet(waveletName, USER1, addParticipantToWavelet(USER2));
    
    Collection<WaveViewData> results = waveMap.search(USER2, "", 0, 20);
    assertEquals(1, results.size());
  }

  public void testSearchLimitEnforced() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, "w" + i), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
    }

    Collection<WaveViewData> results = waveMap.search(USER1, "in:inbox", 0, 5);

    assertEquals(5, results.size());
  }

  public void testSearchIndexWorks() throws Exception {
    // For this test, we'll create 10 waves with wave ids "0", "1", ... "9" and then run 10
    // searches using offsets 0..9. The waves we get back can be in any order, but we must get
    // all 10 of the waves back exactly once each from the search query.

    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
    }

    // The number of times we see each wave when we search
    int[] saw_wave = new int[10];

    for (int i = 0; i < 10; i++) {
      Collection<WaveViewData> results = waveMap.search(USER1, "in:inbox", i, 1);
      assertEquals(1, results.size());
      int index = Integer.parseInt(results.iterator().next().getWaveId().getId());
      saw_wave[index]++;
    }

    for (int i = 0; i < 10; i++) {
      // Each wave should appear exactly once in the results
      assertEquals(1, saw_wave[i]);
    }
  }
  
  public void testSearchOrderByAscWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
    }
    Collection<WaveViewData> results = waveMap.search(USER1, "in:inbox orderby:dateasc", 0, 10);
    Ordering<WaveViewData> ascOrdering = Ordering.from(ASCENDING_DATE_COMPARATOR);
    assertTrue(ascOrdering.isOrdered(results));
  }
  
  public void testSearchOrderByDescWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
    }
    Collection<WaveViewData> results = waveMap.search(USER1, "in:inbox orderby:datedesc", 0, 10);
    Ordering<WaveViewData> descOrdering = Ordering.from(DESCENDING_DATE_COMPARATOR);
    assertTrue(descOrdering.isOrdered(results));
  }
  
  public void testSearchOrderByCreatedAscWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
    }
    Collection<WaveViewData> results = waveMap.search(USER1, "in:inbox orderby:createdasc", 0, 10);
    Ordering<WaveViewData> ascOrdering = Ordering.from(ASC_CREATED_COMPARATOR);
    assertTrue(ascOrdering.isOrdered(results));
  }
  
  public void testSearchOrderByCreatedDescWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
    }
    Collection<WaveViewData> results = waveMap.search(USER1, "in:inbox orderby:createddesc", 0, 10);
    Ordering<WaveViewData> descOrdering = Ordering.from(DESC_CREATED_COMPARATOR);
    assertTrue(descOrdering.isOrdered(results));
  }
  
  public void testSearchOrderByAuthorAscWithCompundingWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      // Add USER2 to two waves.
      if (i == 1 || i == 2) {
        WaveletOperation op1 = addParticipantToWavelet(USER1);
        WaveletOperation op2 = addParticipantToWavelet(USER2);
        submitDeltaToNewWavelet(name, USER1, op1, op2);
      } else {
        submitDeltaToNewWavelet(name, USER2, addParticipantToWavelet(USER2));
      }
    }
    Collection<WaveViewData> resultsAsc =
        waveMap.search(USER2, "in:inbox orderby:creatorasc orderby:createddesc", 0, 10);
    assertEquals(10, resultsAsc.size());
    Ordering<WaveViewData> ascAuthorOrdering = Ordering.from(ASC_CREATOR_COMPARATOR);
    assertTrue(ascAuthorOrdering.isOrdered(resultsAsc));
    Ordering<WaveViewData> descCreatedOrdering = Ordering.from(DESC_CREATED_COMPARATOR);
    // The whole list should not be ordered by creation time.
    assertFalse(descCreatedOrdering.isOrdered(resultsAsc));
    // Each sublist should be ordered by creation time.
    assertTrue(descCreatedOrdering.isOrdered(Lists.newArrayList(resultsAsc).subList(0, 2)));
    assertTrue(descCreatedOrdering.isOrdered(Lists.newArrayList(resultsAsc).subList(2, 10)));
  }
  
  public void testSearchOrderByAuthorDescWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      // Add USER2 to two waves.
      if (i == 1 || i == 2) {
        WaveletOperation op1 = addParticipantToWavelet(USER1);
        WaveletOperation op2 = addParticipantToWavelet(USER2);
        submitDeltaToNewWavelet(name, USER1, op1, op2);
      } else {
        submitDeltaToNewWavelet(name, USER2, addParticipantToWavelet(USER2));
      }
    }
    Collection<WaveViewData> resultsAsc =
        waveMap.search(USER2, "in:inbox orderby:creatordesc", 0, 10);
    assertEquals(10, resultsAsc.size());
    Ordering<WaveViewData> descAuthorOrdering = Ordering.from(DESC_CREATOR_COMPARATOR);
    assertTrue(descAuthorOrdering.isOrdered(resultsAsc));
  }
  
  public void testSearchFilterByWithWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      // Add USER2 to two waves.
      if (i == 1 || i == 2) {
        WaveletOperation op1 = addParticipantToWavelet(USER1);
        WaveletOperation op2 = addParticipantToWavelet(USER2);
        submitDeltaToNewWavelet(name, USER1, op1, op2);
      } else {
        submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
      }
    }
    Collection<WaveViewData> results =
        waveMap.search(USER1, "in:inbox with:" + USER2.getAddress(), 0, 10);
    assertEquals(2, results.size());
    results = waveMap.search(USER1, "in:inbox with:" + USER1.getAddress(), 0, 10);
    assertEquals(10, results.size());
  }
  
  /**
   * If query contains invalid search param - it should return empty result.
   */
  public void testInvalidWithSearchParam() throws Exception {
    WaveletName name = WaveletName.of(WAVE_ID, WAVELET_ID);
    submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
    Collection<WaveViewData> results =
        waveMap.search(USER1, "in:inbox with@^^^@:" + USER1.getAddress(), 0, 10);
    assertEquals(0, results.size());
  }
  
  public void testInvalidOrderByParam() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
    }
    Collection<WaveViewData> results =
        waveMap.search(USER1, "in:inbox orderby:createddescCCC", 0, 10);
    assertEquals(0, results.size());
  }
  
  public void testSearchFilterByCreatorWorks() throws Exception {
    for (int i = 0; i < 10; i++) {
      WaveletName name = WaveletName.of(WaveId.of(DOMAIN, String.valueOf(i)), WAVELET_ID);
      // Add USER2 to two waves as creator.
      if (i == 1 || i == 2) {
        WaveletOperation op1 = addParticipantToWavelet(USER1);
        WaveletOperation op2 = addParticipantToWavelet(USER2);
        submitDeltaToNewWavelet(name, USER2, op1, op2);
      } else {
        submitDeltaToNewWavelet(name, USER1, addParticipantToWavelet(USER1));
      }
    }
    Collection<WaveViewData> results =
        waveMap.search(USER1, "in:inbox creator:" + USER2.getAddress(), 0, 10);
    assertEquals(2, results.size());
    results = waveMap.search(USER1, "in:inbox creator:" + USER1.getAddress(), 0, 10);
    assertEquals(8, results.size());
    results =
        waveMap.search(USER1,
            "in:inbox creator:" + USER1.getAddress() + " creator:" + USER2.getAddress(), 0, 10);
    assertEquals(0, results.size());
  }


  private ExceptionalIterator<WaveId, PersistenceException> eitr(WaveId... waves) {
    return ExceptionalIterator.FromIterator.<WaveId, PersistenceException>create(
        Arrays.asList(waves).iterator());
  }

  // *** Helpers

  private void submitDeltaToNewWavelet(WaveletName name, ParticipantId user,
      WaveletOperation... ops) throws Exception {
    HashedVersion version = V0_HASH_FACTORY.createVersionZero(name);
    WaveletDelta delta = new WaveletDelta(user, version, Arrays.asList(ops));
    ProtocolWaveletDelta protoDelta = CoreWaveletOperationSerializer.serialize(delta);

    // Submitting the request will require the certificate manager to sign the delta. We'll just
    // leave it unsigned.
    ProtocolSignedDelta signedProtoDelta =
        ProtocolSignedDelta.newBuilder().setDelta(protoDelta.toByteString()).build();

    LocalWaveletContainer wavelet = waveMap.getOrCreateLocalWavelet(name);
    wavelet.submitRequest(name, signedProtoDelta);
  }
  
}
