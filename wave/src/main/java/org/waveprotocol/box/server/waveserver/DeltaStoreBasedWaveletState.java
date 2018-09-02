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

import static java.lang.String.format;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import org.waveprotocol.box.common.ListReceiver;
import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.swell.ReadableWaveletContributions;
import org.waveprotocol.box.server.swell.WaveletContributions;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;
import org.waveprotocol.wave.util.logging.Log;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

/**
 * Simplistic {@link DeltaStore}-backed wavelet state implementation
 * which goes to persistent storage for every history request.
 * <br/><br/>
 * If implementation of {@link DeltaStore.DeltaAccess} supports snapshot storage,
 * this class will take advantage of it:
 * <br/><br/>
 * On creation, it'll try to load a snapshot from the storage instead of compose all deltas.
 * If the snapshot is built from deltas composition, persist the snapshot.
 * <br/><br/>
 * Snapshot updates will be persisted every time a fixed number of deltas are added to the wavelet.
 * This will be done by the persistence task, after deltas are persisted.
 *
 *
 * @author soren@google.com (Soren Lassen)
 * @author akaplanov@gmail.com (Andew Kaplanov)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
class DeltaStoreBasedWaveletState implements WaveletState {

  private static final Log LOG = Log.get(DeltaStoreBasedWaveletState.class);


  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new JavaUrlCodec());

  private static final HashedVersionFactory HASH_FACTORY =
      new HashedVersionFactoryImpl(URI_CODEC);

  private static final Function<WaveletDeltaRecord, TransformedWaveletDelta> TRANSFORMED =
      new Function<WaveletDeltaRecord, TransformedWaveletDelta>() {
        @Override
        public TransformedWaveletDelta apply(WaveletDeltaRecord record) {
          return record.getTransformedDelta();
        }
      };

  /**
   * @return An entry keyed by a hashed version with the given version number,
   *         if any, otherwise null.
   */
  private static <T> Map.Entry<HashedVersion, T> lookupCached(NavigableMap<HashedVersion, T> map,
      long version) {
    // Smallest key with version number >= version.
    HashedVersion key = HashedVersion.unsigned(version);
    Map.Entry<HashedVersion, T> entry = map.ceilingEntry(key);
    return (entry != null && entry.getKey().getVersion() == version) ? entry : null;
  }

  /**
   * Creates a new delta store based state.
   *
   * The executor must ensure that only one thread executes at any time for each
   * state instance.
   *
   * @param deltasAccess delta store accessor
   * @param persistExecutor executor for making persistence calls
   * @return a state initialized from the deltas
   * @throws PersistenceException if a failure occurs while reading or
   *         processing stored deltas
   */
  public static DeltaStoreBasedWaveletState create(DeltaStore.DeltasAccess deltasAccess,
      Executor persistExecutor) throws PersistenceException {
    return create(deltasAccess, persistExecutor, 250);
  }

  /**
   * Creates a new delta store based state.
   *
   * The executor must ensure that only one thread executes at any time for each
   * state instance.
   *
   * @param deltasAccess delta store accessor
   * @param persistExecutor executor for making persistence calls
   * @param persistSnapshotOnDeltasCount number of deltas to receive before storing the snapshot
   * @return a state initialized from the deltas
   * @throws PersistenceException if a failure occurs while reading or
   *         processing stored deltas
   */
  public static DeltaStoreBasedWaveletState create(DeltaStore.DeltasAccess deltasAccess,
      Executor persistExecutor, int persistSnapshotOnDeltasCount) throws PersistenceException {
    if (deltasAccess.isEmpty()) {
      return new DeltaStoreBasedWaveletState(deltasAccess, null, persistExecutor, persistSnapshotOnDeltasCount, null);
    } else {

      long t1 = 0;
      long t2 = 0;

      ImmutableList<WaveletDeltaRecord> deltas = null;
      WaveletData snapshot = null;
      WaveletContributions contributions = null;


      String waveletName = ModernIdSerialiser.INSTANCE.serialiseWaveId(deltasAccess.getWaveletName().waveId)+"/"+
          deltasAccess.getWaveletName().waveletId.getId();

      WaveletDeltaRecord lastStoredDelta = null;
      try {
        lastStoredDelta = deltasAccess.getLastDelta();
      } catch (IOException e1) {
        throw new PersistenceException("Failed to get wavelet's last delta", e1);
      }

      // Get snapshot from persistence.
      // Snapshot storage is optional for underlying {@link DeltaStore.DeltasAccess}
      // In case of no available snapshot, build it from deltas.
      t1 = System.currentTimeMillis();

      DeltaStore.Snapshot persistenceSnapshot = deltasAccess.loadSnapshot();

      if (persistenceSnapshot != null) {

        try {
          // Is the persisted snapshot up to date?
          if (lastStoredDelta.getResultingVersion().getVersion() > persistenceSnapshot
              .getWaveletData().getVersion()) {

            long startVersion = persistenceSnapshot.getWaveletData().getHashedVersion().getVersion();
            long endVersion = lastStoredDelta.getResultingVersion().getVersion();

            deltasAccess.getDeltasInRange(startVersion, endVersion,
                new Receiver<WaveletDeltaRecord>() {

                  @Override
                  public boolean put(WaveletDeltaRecord delta) {
                    try {
                      WaveletDataUtil.applyWaveletDelta(delta.getTransformedDelta(),
                          persistenceSnapshot.getWaveletData());
                    } catch (OperationException e) {
                      return false;
                    }
                    return true;
                  }
                });


          }

          if (lastStoredDelta.getResultingVersion().getVersion() == persistenceSnapshot
              .getWaveletData().getVersion()) {
            snapshot = persistenceSnapshot.getWaveletData();
          } else {
            // Wow, the snapshot has a version higher than last delta!
            // That's impossible, let's suppose delta history is right and
            // ignore snapshot
            LOG.severe("Wavelet snaphot has a different version than last delta version");
          }

        } catch (IllegalStateException e) {
          throw new PersistenceException("Failed to compose wavelet snapshot", e);
        } catch (IOException e) {
          throw new PersistenceException("Failed to compose wavelet snapshot", e);
        }

        t2 = System.currentTimeMillis();

        LOG.info("Snapshot loaded for "+waveletName+" in "+ (t2-t1) +"ms");

      }


      // Get the contributions set. The version must be equals to snapshot's version
      WaveletContributions persistenceContributions = deltasAccess.loadContributions();

      if (persistenceContributions != null) {

        try {

          // Is the persisted contributions set up to date according to deltas?
          if (lastStoredDelta.getResultingVersion().getVersion() > persistenceContributions
              .getWaveletVersion().getVersion()) {

            long startVersion = persistenceContributions.getWaveletVersion().getVersion();
            long endVersion = lastStoredDelta.getResultingVersion().getVersion();
            deltasAccess.getDeltasInRange(startVersion, endVersion,
                new Receiver<WaveletDeltaRecord>() {

                  @Override
                  public boolean put(WaveletDeltaRecord delta) {
                    try {
                      persistenceContributions.apply(delta.getTransformedDelta());
                    } catch (Exception e) {
                      return false;
                    }
                    return true;
                  }
                });

          }

          if (lastStoredDelta.getResultingVersion().getVersion() == persistenceContributions
              .getWaveletVersion().getVersion()) {

            contributions = persistenceContributions;

          } else {
            LOG.severe("Wavelet contributions has a different version than last delta version");
          }

        } catch (IOException e) {
          throw new PersistenceException("Failed to compose wavelet contributions", e);
        }

      }




      // Build snapshot from deltas if it is not already built
      if (snapshot == null) {

        try {

          t1 = System.currentTimeMillis();
          // the readAll method is extremely inefficient for mongodb storage
          deltas = readAll(deltasAccess, null);
          snapshot = WaveletDataUtil.buildWaveletFromDeltas(deltasAccess.getWaveletName(),
              Iterators.transform(deltas.iterator(), TRANSFORMED));

          t2 = System.currentTimeMillis();

          LOG.info("Snapshot built for " + waveletName + " in " + (t2 - t1) + "ms");

          // Persist the snapshot only for data wavelets
          if (snapshot.getWaveletId().isDataWavelet())
            deltasAccess.storeSnapshot(snapshot);

        } catch (IOException e) {
          throw new PersistenceException("Failed to read stored deltas", e);
        } catch (OperationException e) {
          throw new PersistenceException("Failed to compose stored deltas", e);
        }

      }

      // Build contributions from deltas
      if (contributions == null) {

        final WaveletContributions newContributions = new WaveletContributions(deltasAccess.getWaveletName());

        try {

          t1 = System.currentTimeMillis();

          LOG.info("Building wavelet contributions for " + waveletName + " ...");
          deltasAccess.getAllDeltas(new Receiver<WaveletDeltaRecord>() {

            @Override
            public boolean put(WaveletDeltaRecord delta) {
              newContributions.apply(delta.getTransformedDelta());
              return true;
            }

          });

          t2 = System.currentTimeMillis();

          LOG.info("Contributions built for " + waveletName + " in " + (t2 - t1) + "ms");

          contributions = newContributions;

          // Persist contributions if it is not user wavelet
          if (!IdUtil.isUserDataWavelet(snapshot.getWaveletId()))
            deltasAccess.storeContributions(contributions);

        } catch (IOException e) {
          throw new PersistenceException("Failed to read stored deltas", e);
        }

      }


      return new DeltaStoreBasedWaveletState(deltasAccess, snapshot, persistExecutor, persistSnapshotOnDeltasCount, contributions);



    }
  }

  /**
   * Reads all deltas from persistent storage.
   */
  private static ImmutableList<WaveletDeltaRecord> readAll(WaveletDeltaRecordReader reader,
      ConcurrentNavigableMap<HashedVersion, WaveletDeltaRecord> cachedDeltas)
      throws IOException {
    HashedVersion startVersion = HASH_FACTORY.createVersionZero(reader.getWaveletName());
    HashedVersion endVersion = reader.getEndVersion();
    ListReceiver<WaveletDeltaRecord> receiver = new ListReceiver<WaveletDeltaRecord>();
    readDeltasInRange(reader, cachedDeltas, startVersion, endVersion, receiver);
    return ImmutableList.copyOf(receiver);
  }

  private static void readDeltasInRange(WaveletDeltaRecordReader reader,
      ConcurrentNavigableMap<HashedVersion, WaveletDeltaRecord> cachedDeltas,
      HashedVersion startVersion, HashedVersion endVersion, Receiver<WaveletDeltaRecord> receiver)
      throws IOException {

    WaveletDeltaRecord delta = getDelta(reader, cachedDeltas, startVersion);
    Preconditions.checkArgument(delta != null && delta.getAppliedAtVersion().equals(startVersion),
        "invalid start version");
    for (;;) {
      if (!receiver.put(delta)) {
        return;
      }
      if (delta.getResultingVersion().getVersion() >= endVersion.getVersion()) {
        break;
      }
      delta = getDelta(reader, cachedDeltas, delta.getResultingVersion());
      if (delta == null) {
        break;
      }
    }
    Preconditions.checkArgument(delta != null && delta.getResultingVersion().equals(endVersion),
        "invalid end version");
  }

  private static class DeltaRecordTrackerReceiver implements Receiver<WaveletDeltaRecord> {

    Receiver<WaveletDeltaRecord> target;
    WaveletDeltaRecord lastDelta = null;
    boolean halted = false;

    public DeltaRecordTrackerReceiver(Receiver<WaveletDeltaRecord> target) {
      this.target = target;
    }

    @Override
    public boolean put(WaveletDeltaRecord delta) {
      lastDelta = delta;
      boolean goNext = target.put(delta);
      halted = !goNext;
      return goNext;
    }

  }

  /**
   * A smart method to read a range of deltas from database in one request and
   * maybe some others from in memory cache (those deltas not yet persisted).
   * <p>
   * Use instead of former method
   * {@link #readDeltasInRange(WaveletDeltaRecordReader, ConcurrentNavigableMap, HashedVersion, HashedVersion, Receiver)
   * for efficient storages like MongoDB
   * <p><br>
   * Look up deltas in storage first, then look up in memory
   * if is necessary the rest of the range
   * <p>
   * <pre>
   * |---- stored deltas ---|--- cached deltas ---|
   *            |-----requested range---|
   *           start                    end
   * </pre>
   * <b>
   * <p>
   * Deltas are returned in ascending sort if startVersion is greater than endVersion,
   * and desceding sort otherwise.
   *
   * @param reader
   * @param cachedDeltas
   * @param startVersion
   * @param endVersion
   * @param receiver
   * @throws IOException
   */
  private static void readDeltasInRangeSmart(WaveletDeltaRecordReader reader,
      ConcurrentNavigableMap<HashedVersion, WaveletDeltaRecord> cachedDeltas,
      HashedVersion startVersion, HashedVersion endVersion, Receiver<WaveletDeltaRecord> receiver)
      throws IOException {

    boolean ascendingSort = startVersion.getVersion() < endVersion.getVersion();

    DeltaRecordTrackerReceiver internalReceiver = new DeltaRecordTrackerReceiver(receiver);

    reader.getDeltasInRange(startVersion.getVersion(), endVersion.getVersion(), internalReceiver);

    if (internalReceiver.halted)
      return;

    HashedVersion startVersionCache = null;
    HashedVersion endVersionCache = null;

    if (internalReceiver.lastDelta == null) {

      startVersionCache = startVersion;
      endVersionCache = endVersion;

    } else if ( (ascendingSort && internalReceiver.lastDelta.getResultingVersion().getVersion() < endVersion.getVersion()) ||
                (!ascendingSort && internalReceiver.lastDelta.getResultingVersion().getVersion() > endVersion.getVersion() )) {

      // there are deltas left to retrieve from in memory cache
        startVersionCache = internalReceiver.lastDelta.getResultingVersion();
        endVersionCache = endVersion;

    } else {
      // all deltas where retrieved from storage
      return;
    }


    if (ascendingSort) {
      readDeltasCacheAscending(cachedDeltas, startVersionCache, endVersionCache, internalReceiver);
    } else {
      readDeltasCacheDescending(cachedDeltas, startVersionCache, endVersionCache, internalReceiver);
    }

    /*

    WaveletDeltaRecord delta = internalReceiver.lastDelta;
    if (cachedDeltas != null) {

      // Read deltas range from cache
      delta = cachedDeltas.get(startVersionCache);
      Preconditions.checkArgument(
          delta != null && delta.getAppliedAtVersion().equals(startVersionCache),
          "invalid start version");
      for (;;) {
        if (!receiver.put(delta)) {
          return;
        }
        if (delta.getResultingVersion().getVersion() >= endVersion.getVersion()) {
          break;
        }
        delta = cachedDeltas.get(delta.getResultingVersion());
        if (delta == null) {
          break;
        }
      }
    }

    Preconditions.checkArgument(
        delta != null && delta.getResultingVersion().equals(endVersionCache),
        "invalid end version");
     */

  }

  private static void readDeltasCacheAscending(
      ConcurrentNavigableMap<HashedVersion, WaveletDeltaRecord> cachedDeltas,
      HashedVersion startVersion, HashedVersion endVersion, DeltaRecordTrackerReceiver receiver) {

    Preconditions.checkArgument(startVersion.getVersion() < endVersion.getVersion(),
        "Delta start version must be less than end version");
    ConcurrentNavigableMap<HashedVersion, WaveletDeltaRecord> subCachedDeltas = cachedDeltas
        .tailMap(startVersion);

    Iterator<HashedVersion> viterator = subCachedDeltas.navigableKeySet().iterator();
    while (viterator.hasNext()) {
      boolean stop = receiver.put(subCachedDeltas.get(viterator.next()));
      stop = stop || receiver.halted
          || receiver.lastDelta.getResultingVersion().getVersion() >= endVersion.getVersion();
      if (stop)
        return;
    }

  }

  private static void readDeltasCacheDescending(
      ConcurrentNavigableMap<HashedVersion, WaveletDeltaRecord> cachedDeltas,
      HashedVersion startVersion, HashedVersion endVersion, DeltaRecordTrackerReceiver receiver) {

    Preconditions.checkArgument(startVersion.getVersion() > endVersion.getVersion(),
        "Delta start version must be greater than end version");
    ConcurrentNavigableMap<HashedVersion, WaveletDeltaRecord> subCachedDeltas = cachedDeltas
        .headMap(startVersion);

    Iterator<HashedVersion> viterator = subCachedDeltas.navigableKeySet().descendingIterator();
    while (viterator.hasNext()) {
      boolean stop = receiver.put(subCachedDeltas.get(viterator.next()));
      stop = stop || receiver.halted
          || receiver.lastDelta.getResultingVersion().getVersion() <= endVersion.getVersion();
      if (stop)
        return;
    }

  }

  private static WaveletDeltaRecord getDelta(WaveletDeltaRecordReader reader,
      ConcurrentNavigableMap<HashedVersion, WaveletDeltaRecord> cachedDeltas,
      HashedVersion version) throws IOException {

    WaveletDeltaRecord delta = null;

    // try cache first!
    if (cachedDeltas != null)
      delta = cachedDeltas.get(version);

    if (delta == null) delta = reader.getDelta(version.getVersion());

    return delta;
  }

  private final Executor persistExecutor;
  private final HashedVersion versionZero;
  private final DeltaStore.DeltasAccess deltasAccess;

  /** The lock that guards access to persistence related state. */
  private final Object persistLock = new Object();

  /**
   * Indicates the version of the latest appended delta that was already requested to be
   * persisted.
   */
  private HashedVersion latestVersionToPersist = null;

  /** The persist task that will be executed next. */
  private ListenableFutureTask<Void> nextPersistTask = null;

  /**
   * the number of deltas to be processed before to
   * persist the snapshot.
   */
  private final int persistSnapshotDeltasCountThreshold;

  /**
   * Counter of processed deltas in order to persist the snapshot
   * if its value excess {@link persistSnapshotOnDeltaCount}
   */
  private int deltasCountBeforeSnapshotStore = 0;



  /**
   * Processes the persist task and checks if there is another task to do when
   * one task is done. In such a case, it writes all waiting to be persisted
   * deltas to persistent storage in one operation.
   *
   * Also persist the snapshot if it is required.
   */
  private final Callable<Void> persisterTask = new Callable<Void>() {
    @Override
    public Void call() throws PersistenceException {
      HashedVersion last;
      HashedVersion version;
      synchronized (persistLock) {
        last = lastPersistedVersion.get();
        version = latestVersionToPersist;
      }
      if (last != null && version.getVersion() <= last.getVersion()) {
        LOG.fine("Attempt to persist version " + version
            + " smaller than last persisted version " + last);
        // Done, version is already persisted.
        version = last;
      } else {
        ImmutableList.Builder<WaveletDeltaRecord> deltas = ImmutableList.builder();
        HashedVersion v = (last == null) ? versionZero : last;
        do {
          WaveletDeltaRecord d = cachedDeltas.get(v);
          deltas.add(d);
          v = d.getResultingVersion();
        } while (v.getVersion() < version.getVersion());
        Preconditions.checkState(v.equals(version));
        deltasAccess.append(deltas.build());

        if (deltasCountBeforeSnapshotStore >= persistSnapshotDeltasCountThreshold) {
          synchronized (persistLock) {
            if (snapshot.getWaveletId().isDataWavelet()) {
              deltasAccess.storeSnapshot(snapshot);
              deltasAccess.storeContributions(contributions);
            }
            deltasCountBeforeSnapshotStore = 0;
          }
        }

      }
      synchronized (persistLock) {
        Preconditions.checkState(last == lastPersistedVersion.get(),
            "lastPersistedVersion changed while we were writing to storage");
        lastPersistedVersion.set(version);
        if (nextPersistTask != null) {
          persistExecutor.execute(nextPersistTask);
          nextPersistTask = null;
        } else {
          latestVersionToPersist = null;
          }
        }
      return null;
    }
  };


  /** Keyed by appliedAtVersion. */
  private final ConcurrentNavigableMap<HashedVersion, WaveletDeltaRecord> cachedDeltas =
      new ConcurrentSkipListMap<HashedVersion, WaveletDeltaRecord>();

  /** Is null if the wavelet state is empty. */
  private WaveletData snapshot;

  /** Null if the wavelet state is empty. */
  private WaveletContributions contributions;


  /**
   * Last version persisted with a call to persist(), or null if never called.
   * It's an atomic reference so we can set in one thread (which
   * asynchronously writes deltas to storage) and read it in another,
   * simultaneously.
   */
  private final AtomicReference<HashedVersion> lastPersistedVersion;

  /**
   * Constructs a wavelet state with the given snapshot.

   * The snapshot must be the composition of the deltas, or null if there
   * are no deltas. The constructed object takes ownership of the
   * snapshot and will mutate it if appendDelta() is called.
   * <p>
   * The delta store is responsible to update the snapshot
   * but no necessarily for each appendDelta(). Hence it is expected that
   * snapshots are stored less frequently.
   * <p>
   *
   */
  @VisibleForTesting
  DeltaStoreBasedWaveletState(DeltaStore.DeltasAccess deltasAccess, WaveletData snapshot, Executor persistExecutor, int persistSnapshotOnDeltasCount, WaveletContributions contributions) {
    this.persistExecutor = persistExecutor;
    this.versionZero = HASH_FACTORY.createVersionZero(deltasAccess.getWaveletName());
    this.deltasAccess = deltasAccess;
    this.snapshot = snapshot;
    this.lastPersistedVersion = new AtomicReference<HashedVersion>(deltasAccess.getEndVersion());
    this.persistSnapshotDeltasCountThreshold = persistSnapshotOnDeltasCount;
    this.contributions = contributions;
  }

  @Override
  public WaveletName getWaveletName() {
    return deltasAccess.getWaveletName();
  }

  @Override
  public ReadableWaveletData getSnapshot() {
    return snapshot;
  }

  @Override
  public HashedVersion getCurrentVersion() {
    return (snapshot == null) ? versionZero : snapshot.getHashedVersion();
  }

  @Override
  public HashedVersion getLastPersistedVersion() {
    HashedVersion version = lastPersistedVersion.get();
    return (version == null) ? versionZero : version;
  }

  @Override
  public HashedVersion getHashedVersion(long version) {
    final Entry<HashedVersion, WaveletDeltaRecord> cachedEntry =
        lookupCached(cachedDeltas, version);
    if (version == 0) {
      return versionZero;
    } else if (snapshot == null) {
      return null;
    } else if (version == snapshot.getVersion()) {
      return snapshot.getHashedVersion();
    } else {
      WaveletDeltaRecord delta;
      try {
        delta = lookup(version);
      } catch (IOException e) {
        throw new RuntimeIOException(new IOException(format("Version : %d", version), e));
      }
      if (delta == null && cachedEntry != null) {
        return cachedEntry.getKey();
      } else {
       return delta != null ? delta.getAppliedAtVersion() : null;
      }
    }
  }

  @Override
  public TransformedWaveletDelta getTransformedDelta(
      final HashedVersion beginVersion) {
    WaveletDeltaRecord delta = cachedDeltas.get(beginVersion);
    if (delta != null) {
      return delta.getTransformedDelta();
    } else {
      WaveletDeltaRecord nowDelta;
      try {
        nowDelta = lookup(beginVersion.getVersion());
      } catch (IOException e) {
        throw new RuntimeIOException(new IOException(format("Begin version : %s",
            beginVersion.toString()), e));
      }
      return nowDelta != null ? nowDelta.getTransformedDelta() : null;
    }
  }

  @Override
  public TransformedWaveletDelta getTransformedDeltaByEndVersion(final HashedVersion endVersion) {
    Preconditions.checkArgument(endVersion.getVersion() > 0, "end version %s is not positive",
        endVersion);
    Entry<HashedVersion, WaveletDeltaRecord> transformedEntry =
        cachedDeltas.lowerEntry(endVersion);
    final WaveletDeltaRecord cachedDelta =
        transformedEntry != null ? transformedEntry.getValue() : null;
    if (snapshot == null) {
      return null;
    } else {
      WaveletDeltaRecord deltaRecord = getDeltaRecordByEndVersion(endVersion);
      TransformedWaveletDelta delta;
      if (deltaRecord == null && cachedDelta != null
          && cachedDelta.getResultingVersion().equals(endVersion)) {
        delta = cachedDelta.getTransformedDelta();
      } else {
        delta = deltaRecord != null ? deltaRecord.getTransformedDelta() : null;
      }
      return delta;
    }
  }

  @Override
  public void getTransformedDeltaHistory(final HashedVersion startVersion,
    final HashedVersion endVersion, final Receiver<TransformedWaveletDelta> receiver) {
    try {


      readDeltasInRangeSmart(deltasAccess, cachedDeltas, startVersion, endVersion,
          new Receiver<WaveletDeltaRecord>() {
            @Override
            public boolean put(WaveletDeltaRecord delta) {

              if (delta.getAppliedAtVersion().getVersion() == startVersion.getVersion())
                Preconditions.checkArgument(delta.getAppliedAtVersion().equals(startVersion),
                    "invalid start version");


              if (delta.getResultingVersion().getVersion() == endVersion.getVersion())
                Preconditions.checkArgument(delta.getResultingVersion().equals(endVersion),
                    "invalid end version");

              return receiver.put(delta.getTransformedDelta());
            }
          });


    } catch (IOException e) {
      throw new RuntimeIOException(new IOException(format("Start version : %s, end version: %s",
          startVersion.toString(), endVersion.toString()), e));
    }
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(
      HashedVersion beginVersion) {
    WaveletDeltaRecord delta = cachedDeltas.get(beginVersion);
    if (delta != null) {
      return delta.getAppliedDelta();
    } else {
      WaveletDeltaRecord record = null;
      try {
        record = lookup(beginVersion.getVersion());
      } catch (IOException e) {
        throw new RuntimeIOException(new IOException(format("Begin version : %s",
            beginVersion.toString()), e));
      }
      return record != null ? record.getAppliedDelta() : null;
    }
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDeltaByEndVersion(
      final HashedVersion endVersion) {
    Preconditions.checkArgument(endVersion.getVersion() > 0,
        "end version %s is not positive", endVersion);
    Entry<HashedVersion, WaveletDeltaRecord> appliedEntry =
        cachedDeltas.lowerEntry(endVersion);
    final ByteStringMessage<ProtocolAppliedWaveletDelta> cachedDelta =
        appliedEntry != null ? appliedEntry.getValue().getAppliedDelta() : null;
    WaveletDeltaRecord deltaRecord = getDeltaRecordByEndVersion(endVersion);
    ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta;
    if (deltaRecord == null && isDeltaBoundary(endVersion)) {
      appliedDelta = cachedDelta;
    } else {
      appliedDelta = deltaRecord != null ? deltaRecord.getAppliedDelta() : null;
    }
    return appliedDelta;
  }

  @Override
  public void getAppliedDeltaHistory(HashedVersion startVersion, HashedVersion endVersion,
      final Receiver<ByteStringMessage<ProtocolAppliedWaveletDelta>> receiver) {
    Preconditions.checkArgument(startVersion.getVersion() < endVersion.getVersion());
    try {

      readDeltasInRangeSmart(deltasAccess, cachedDeltas, startVersion, endVersion,
          new Receiver<WaveletDeltaRecord>() {
        @Override
        public boolean put(WaveletDeltaRecord delta) {

          if (delta.getAppliedAtVersion().getVersion() == startVersion.getVersion())
            Preconditions.checkArgument(delta.getAppliedAtVersion().equals(startVersion),
                "invalid start version");


          if (delta.getResultingVersion().getVersion() == endVersion.getVersion())
            Preconditions.checkArgument(delta.getResultingVersion().equals(endVersion),
                "invalid end version");

          return receiver.put(delta.getAppliedDelta());
        }
      });

    } catch (IOException e) {
      throw new RuntimeIOException(new IOException(format("Start version : %s, end version: %s",
          startVersion.toString(), endVersion.toString()), e));
    }
  }

  @Override
  public void appendDelta(WaveletDeltaRecord deltaRecord)
      throws OperationException {
    HashedVersion currentVersion = getCurrentVersion();
    Preconditions.checkArgument(currentVersion.equals(deltaRecord.getAppliedAtVersion()),
        "Applied version %s doesn't match current version %s", deltaRecord.getAppliedAtVersion(),
        currentVersion);



      if (deltaRecord.getAppliedAtVersion().getVersion() == 0) {
        Preconditions.checkState(lastPersistedVersion.get() == null);
        snapshot = WaveletDataUtil.buildWaveletFromFirstDelta(getWaveletName(), deltaRecord.getTransformedDelta());
        contributions = new WaveletContributions(deltasAccess.getWaveletName());
        contributions.apply(deltaRecord.getTransformedDelta());
      } else {
        // Avoid to update snapshot when it has being persisted
        synchronized (persistLock) {
          WaveletDataUtil.applyWaveletDelta(deltaRecord.getTransformedDelta(), snapshot);
          contributions.apply(deltaRecord.getTransformedDelta());
        }
      }


    // Now that we built the snapshot without any exceptions, we record the delta.
    cachedDeltas.put(deltaRecord.getAppliedAtVersion(), deltaRecord);

    // Increment counter controlling snapshot persistence
    deltasCountBeforeSnapshotStore++;
  }

  @Override
  public ListenableFuture<Void> persist(final HashedVersion version) {
    Preconditions.checkArgument(version.getVersion() > 0,
        "Cannot persist non-positive version %s", version);
    Preconditions.checkArgument(isDeltaBoundary(version),
        "Version to persist %s matches no delta", version);
    synchronized (persistLock) {
      if (latestVersionToPersist != null) {
        // There's a persist task in flight.
        if (version.getVersion() <= latestVersionToPersist.getVersion()) {
          LOG.info("Attempt to persist version " + version
              + " smaller than last version requested " + latestVersionToPersist);
        } else {
          latestVersionToPersist = version;
        }
        if (nextPersistTask == null) {
          nextPersistTask = ListenableFutureTask.<Void>create(persisterTask);
        }
        return nextPersistTask;
      } else {
        latestVersionToPersist = version;
        ListenableFutureTask<Void> resultTask = ListenableFutureTask.<Void>create(persisterTask);
        persistExecutor.execute(resultTask);
        return resultTask;
      }
    }
  }

  @Override
  public void flush(HashedVersion version) {

    HashedVersion toDeleteVersion = cachedDeltas.lowerKey(version);
    int count = 0;
    while (toDeleteVersion != null) {
      count++;
      WaveletDeltaRecord delta = cachedDeltas.remove(toDeleteVersion);
      toDeleteVersion = delta != null ? delta.getAppliedAtVersion() : null;
    }

    if (LOG.isFineLoggable()) {
      LOG.fine(snapshot.getWaveId() + " / " + snapshot.getWaveletId() + " Flushed " + count
          + " cached deltas up to version " + version + ". Cache size is " + cachedDeltas.size());
    }
  }

  @Override
  public void close() {
  }

  /**
   * @return An entry keyed by a hashed version with the given version number,
   *         if any, otherwise null.
   */
  private WaveletDeltaRecord lookup(long version) throws IOException {
    return deltasAccess.getDelta(version);
  }

  private WaveletDeltaRecord getDeltaRecordByEndVersion(HashedVersion endVersion) {
    long version = endVersion.getVersion();
    try {
      return deltasAccess.getDeltaByEndVersion(version);
    } catch (IOException e) {
      throw new RuntimeIOException(new IOException(format("Version : %d", version), e));
    }
  }

  private boolean isDeltaBoundary(HashedVersion version) {
    Preconditions.checkNotNull(version, "version is null");
    return version.equals(getCurrentVersion()) || cachedDeltas.containsKey(version);
  }

  @Override
  public ReadableWaveletContributions getContributions() {
    return contributions;
  }

}
