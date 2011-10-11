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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.util.WaveletDataUtil;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
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

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simplistic {@link DeltaStore}-backed wavelet state implementation
 * which keeps the entire delta history in memory.
 *
 * TODO(soren): only keep in memory what's not persisted
 *
 * TODO(soren): rewire this class to be backed by {@link WaveletStore} and
 * read the snapshot from there instead of computing it in the
 * DeltaStoreBasedWaveletState constructor
 *
 * TODO(soren): refine the persist() logic to make it batch successive
 * writes to storage, when write latency exceeds the intervals between
 * calls to persist()
 *
 * @author soren@google.com (Soren Lassen)
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
    // Note that the logic in persist() depends on persistExecutor being single-threaded.
    // TODO(soren): finesse the logic in persist() so it
    // doesn't require it to be single-threaded, because it would be useful to be
    // able to use a shared executor with a thread-count set to the appropriate level
    // of write parallelism for the storage subsystem.
    if (deltasAccess.isEmpty()) {
      return new DeltaStoreBasedWaveletState(deltasAccess, ImmutableList.<WaveletDeltaRecord>of(),
          null, persistExecutor);
    } else {
      try {
        ImmutableList<WaveletDeltaRecord> deltas = readAll(deltasAccess);
        WaveletData snapshot = WaveletDataUtil.buildWaveletFromDeltas(deltasAccess.getWaveletName(),
            Iterators.transform(deltas.iterator(), TRANSFORMED));
        return new DeltaStoreBasedWaveletState(deltasAccess, deltas, snapshot, persistExecutor);
      } catch (IOException e) {
        throw new PersistenceException("Failed to read stored deltas", e);
      } catch (OperationException e) {
        throw new PersistenceException("Failed to compose stored deltas", e);
      }
    }
  }

  /**
   * Reads all deltas from
   */
  private static ImmutableList<WaveletDeltaRecord> readAll(WaveletDeltaRecordReader reader)
      throws IOException{
    Preconditions.checkArgument(!reader.isEmpty());
    ImmutableList.Builder<WaveletDeltaRecord> result = ImmutableList.builder();
    HashedVersion endVersion = reader.getEndVersion();
    long version = 0;
    while (version < endVersion.getVersion()) {
      WaveletDeltaRecord delta = reader.getDelta(version);
      result.add(delta);
      version = delta.getResultingVersion().getVersion();
    }
    return result.build();
  }

  /**
   * @return An entry keyed by a hashed version with the given version number,
   *         if any, otherwise null.
   */
  private static <T> Map.Entry<HashedVersion, T> lookup(
      NavigableMap<HashedVersion, T> map, long version) {
    // Smallest key with version number >= version.
    HashedVersion key = HashedVersion.unsigned(version);
    Map.Entry<HashedVersion, T> entry = map.ceilingEntry(key);
    return (entry != null && entry.getKey().getVersion() == version) ? entry : null;
  }

  private final Executor persistExecutor;
  private final HashedVersion versionZero;
  private final DeltaStore.DeltasAccess deltasAccess;

  /** Keyed by appliedAtVersion. */
  private final NavigableMap<HashedVersion, ByteStringMessage<ProtocolAppliedWaveletDelta>>
      appliedDeltas = Maps.newTreeMap();

  /** Keyed by appliedAtVersion. */
  private final NavigableMap<HashedVersion, TransformedWaveletDelta> transformedDeltas =
      Maps.newTreeMap();

  /** Is null if the wavelet state is empty. */
  private WaveletData snapshot;

  /**
   * Last version persisted with a call to persist(), or null if never called.
   * It's an atomic reference so we can set in one thread (which
   * asynchronously writes deltas to storage) and read it in another,
   * simultaneously.
   */
  private final AtomicReference<HashedVersion> lastPersistedVersion;

  /**
   * Constructs a wavelet state with the given deltas and snapshot.
   * The deltas must be the contents of deltasAccess, and they
   * must be contiguous from version zero.
   * The snapshot must be the composition of the deltas, or null if there
   * are no deltas. The constructed object takes ownership of the
   * snapshot and will mutate it if appendDelta() is called.
   */
  @VisibleForTesting
  DeltaStoreBasedWaveletState(DeltaStore.DeltasAccess deltasAccess,
      List<WaveletDeltaRecord> deltas, WaveletData snapshot, Executor persistExecutor) {
    Preconditions.checkArgument(deltasAccess.isEmpty() == deltas.isEmpty());
    Preconditions.checkArgument(deltas.isEmpty() == (snapshot == null));
    this.persistExecutor = persistExecutor;
    this.versionZero = HASH_FACTORY.createVersionZero(deltasAccess.getWaveletName());
    this.deltasAccess = deltasAccess;
    for (WaveletDeltaRecord delta : deltas) {
      HashedVersion hashedVersion = delta.getAppliedAtVersion();
      appliedDeltas.put(hashedVersion, delta.getAppliedDelta());
      transformedDeltas.put(hashedVersion, delta.getTransformedDelta());
    }
    this.snapshot = snapshot;
    this.lastPersistedVersion = new AtomicReference<HashedVersion>(deltasAccess.getEndVersion());
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
    if (version == 0) {
      return versionZero;
    } else if (snapshot == null) {
      return null;
    } else if (version == snapshot.getVersion()) {
      return snapshot.getHashedVersion();
    } else {
      Map.Entry<HashedVersion, TransformedWaveletDelta> entry = lookup(transformedDeltas, version);
      return (entry == null) ? null : entry.getKey();
    }
  }

  @Override
  public TransformedWaveletDelta getTransformedDelta(HashedVersion beginVersion) {
    return transformedDeltas.get(beginVersion);
  }

  @Override
  public TransformedWaveletDelta getTransformedDeltaByEndVersion(HashedVersion endVersion) {
    Preconditions.checkArgument(endVersion.getVersion() > 0,
        "end version %s is not positive", endVersion);
    if (snapshot == null) {
      return null;
    } else if (endVersion.equals(snapshot.getHashedVersion())) {
      return transformedDeltas.lastEntry().getValue();
    } else {
      TransformedWaveletDelta delta = transformedDeltas.lowerEntry(endVersion).getValue();
      return delta.getResultingVersion().equals(endVersion) ? delta : null;
    }
  }

  @Override
  public DeltaSequence getTransformedDeltaHistory(HashedVersion startVersion,
      HashedVersion endVersion) {
    Preconditions.checkArgument(startVersion.getVersion() < endVersion.getVersion(),
        "Start version %s should be smaller than end version %s", startVersion, endVersion);
    NavigableMap<HashedVersion, TransformedWaveletDelta> deltas =
        transformedDeltas.subMap(startVersion, true, endVersion, false);
    return
        (!deltas.isEmpty() &&
         deltas.firstKey().equals(startVersion) &&
         deltas.lastEntry().getValue().getResultingVersion().equals(endVersion))
        ? DeltaSequence.of(deltas.values())
        : null;
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(
      HashedVersion beginVersion) {
    return appliedDeltas.get(beginVersion);
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDeltaByEndVersion(
      HashedVersion endVersion) {
    Preconditions.checkArgument(endVersion.getVersion() > 0,
        "end version %s is not positive", endVersion);
    return isDeltaBoundary(endVersion) ? appliedDeltas.lowerEntry(endVersion).getValue() : null;

  }

  @Override
  public Collection<ByteStringMessage<ProtocolAppliedWaveletDelta>> getAppliedDeltaHistory(
      HashedVersion startVersion, HashedVersion endVersion) {
    Preconditions.checkArgument(startVersion.getVersion() < endVersion.getVersion());
    return (isDeltaBoundary(startVersion) && isDeltaBoundary(endVersion))
        ? appliedDeltas.subMap(startVersion, endVersion).values()
        : null;
  }

  @Override
  public void appendDelta(HashedVersion appliedAtVersion,
      TransformedWaveletDelta transformedDelta,
      ByteStringMessage<ProtocolAppliedWaveletDelta> appliedDelta)
      throws OperationException {
    HashedVersion currentVersion = getCurrentVersion();
    Preconditions.checkArgument(currentVersion.equals(appliedAtVersion),
        "Applied version %s doesn't match current version %s", appliedAtVersion, currentVersion);

    if (appliedAtVersion.getVersion() == 0) {
      Preconditions.checkState(lastPersistedVersion.get() == null);
      snapshot = WaveletDataUtil.buildWaveletFromFirstDelta(getWaveletName(), transformedDelta);
    } else {
      WaveletDataUtil.applyWaveletDelta(transformedDelta, snapshot);
    }

    // Now that we built the snapshot without any exceptions, we record the delta.
    transformedDeltas.put(appliedAtVersion, transformedDelta);
    appliedDeltas.put(appliedAtVersion, appliedDelta);
  }

  @Override
  public ListenableFuture<Void> persist(final HashedVersion version) {
    Preconditions.checkArgument(version.getVersion() > 0,
        "Cannot persist non-positive version %s", version);
    Preconditions.checkArgument(isDeltaBoundary(version),
        "Version to persist %s matches no delta", version);

    // The following logic relies on persistExecutor being single-threaded,
    // so no two tasks execute in parallel.
    ListenableFutureTask<Void> resultTask = new ListenableFutureTask<Void>(
        new Callable<Void>() {
          @Override
          public Void call() throws PersistenceException {
            HashedVersion last = lastPersistedVersion.get();
            if (last != null && version.getVersion() <= last.getVersion()) {
              LOG.info("Attempt to persist version " + version
                  + " smaller than last persisted version " + last);
              // done, version is already persisted
            } else {
              ImmutableList.Builder<WaveletDeltaRecord> deltas = ImmutableList.builder();
              HashedVersion v = (last == null) ? versionZero : last;
              do {
                WaveletDeltaRecord d =
                    new WaveletDeltaRecord(v, appliedDeltas.get(v), transformedDeltas.get(v));
                deltas.add(d);
                v = d.getResultingVersion();
              } while (v.getVersion() < version.getVersion());
              Preconditions.checkState(v.equals(version));
              deltasAccess.append(deltas.build());
              Preconditions.checkState(last == lastPersistedVersion.get(),
                  "lastPersistedVersion changed while we were writing to storage");
              lastPersistedVersion.set(version);
            }
            return null;
          }
        });
    persistExecutor.execute(resultTask);
    return resultTask;
  }

  @Override
  public void close() {
  }

  private boolean isDeltaBoundary(HashedVersion version) {
    Preconditions.checkNotNull(version, "version is null");
    return version.equals(getCurrentVersion()) || transformedDeltas.containsKey(version);
  }
}
