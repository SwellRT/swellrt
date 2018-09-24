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

package org.waveprotocol.box.server.persistence.mongodb;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.bson.conversions.Bson;
import org.waveprotocol.box.common.Receiver;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.swell.WaveletContributions;
import org.waveprotocol.box.server.waveserver.ByteStringMessage;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.box.server.waveserver.WaveletDeltaRecord;
import org.waveprotocol.wave.federation.Proto.ProtocolAppliedWaveletDelta;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.util.logging.Log;

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;

/**
 * A MongoDB based Delta Access implementation using a simple <b>deltas</b>
 * collection, storing a delta record per each MongoDb document.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class MongoDbDeltaCollection implements DeltaStore.DeltasAccess {

  private static final Log LOG = Log.get(MongoDbDeltaCollection.class);



  /** Wavelet name to work with. */
  private final WaveletName waveletName;

  /** MongoDB Collection object for delta storage */
  private final MongoCollection<BasicDBObject> deltasCollection;

  /** MongoDB based wavelet snapshot store */
  private final MongoDBSnapshotStore snapshotStore;


  public static MongoDbDeltaCollection create(WaveletName waveletName,
      MongoCollection<BasicDBObject> deltasCollection, MongoDBSnapshotStore snapshotStore) {
    return new MongoDbDeltaCollection(waveletName, deltasCollection, snapshotStore);
  }

  /**
   * Construct a new Delta Access object for the wavelet
   *
   * @param waveletName The wavelet name.
   * @param deltaDbCollection The MongoDB deltas collection
   */
  public MongoDbDeltaCollection(WaveletName waveletName, MongoCollection<BasicDBObject> deltasCollection, MongoDBSnapshotStore snapshotStore) {
    this.waveletName = waveletName;
    this.deltasCollection = deltasCollection;
    this.snapshotStore = snapshotStore;
  }

  @Override
  public WaveletName getWaveletName() {
    return waveletName;
  }

  /**
   * Create Filter to match all Wavelet deltas
   *
   * @return bson filter
   */
  protected Bson createWaveletDBQuery() {

    return Filters.and(
        Filters.eq(MongoDbDeltaStoreUtil.FIELD_WAVE_ID, waveletName.waveId.serialise()),
        Filters.eq(MongoDbDeltaStoreUtil.FIELD_WAVELET_ID, waveletName.waveletId.serialise()));
  }

  /**
   * Create Filter to match Wavelet deltas at given "applied at" version.
   *
   * @return bson filter
   */
  protected Bson filterByAppliedAtVersion(long version) {

    return Filters.and(createWaveletDBQuery(),
        Filters.eq(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_APPLIEDATVERSION, version));
  }

  /**
   * Create Filter to match Wavelet deltas at given "resulting" version.
   *
   * @return bson filter
   */
  protected Bson filterByResultingVersion(long version) {

    return Filters.and(createWaveletDBQuery(),
        Filters.eq(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_RESULTINGVERSION_VERSION, version));
  }

  @Override
  public boolean isEmpty() {

    return deltasCollection.count(createWaveletDBQuery()) == 0;
  }

  @Override
  public HashedVersion getEndVersion() {

    // Search the max of delta.getTransformedDelta().getResultingVersion()

    BasicDBObject sort = new BasicDBObject();
    sort.put(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_RESULTINGVERSION_VERSION, -1); // Descending

    BasicDBObject field = new BasicDBObject();
    field.put(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_RESULTINGVERSION, 1);

    BasicDBObject result = deltasCollection.find(createWaveletDBQuery()).projection(field)
        .sort(sort).first();

    return result != null ? MongoDbDeltaStoreUtil
        .deserializeHashedVersion((DBObject) ((DBObject) result
            .get(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED))
            .get(MongoDbDeltaStoreUtil.FIELD_RESULTINGVERSION)) : null;
  }

  @Override
  public WaveletDeltaRecord getDelta(long version) throws IOException {

    BasicDBObject result = deltasCollection.find(filterByAppliedAtVersion(version)).first();

    WaveletDeltaRecord waveletDelta = null;

    if (result != null) try {
      waveletDelta = MongoDbDeltaStoreUtil.deserializeWaveletDeltaRecord(result);
    } catch (PersistenceException e) {
      throw new IOException(e);
    }
    return waveletDelta;
  }

  @Override
  public WaveletDeltaRecord getDeltaByEndVersion(long version) throws IOException {

    DBObject result = deltasCollection.find(filterByResultingVersion(version)).first();

    WaveletDeltaRecord waveletDelta = null;

    if (result != null)
    try {
      waveletDelta = MongoDbDeltaStoreUtil.deserializeWaveletDeltaRecord(result);
    } catch (PersistenceException e) {
      throw new IOException(e);
    }
    return waveletDelta;
  }

  @Override
  public HashedVersion getAppliedAtVersion(long version) throws IOException {


    BasicDBObject result = deltasCollection.find(filterByAppliedAtVersion(version)).first();

    if (result != null)
      return MongoDbDeltaStoreUtil.deserializeHashedVersion((DBObject) result
          .get(MongoDbDeltaStoreUtil.FIELD_APPLIEDATVERSION));
    return null;
  }

  @Override
  public HashedVersion getResultingVersion(long version) throws IOException {

    DBObject result = deltasCollection.find(filterByAppliedAtVersion(version)).first();

    if (result != null)
      return MongoDbDeltaStoreUtil.deserializeHashedVersion((DBObject) result
          .get(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_RESULTINGVERSION));
    return null;
  }

  @Override
  public ByteStringMessage<ProtocolAppliedWaveletDelta> getAppliedDelta(long version)
      throws IOException {

    WaveletDeltaRecord delta = getDelta(version);
    return (delta != null) ? delta.getAppliedDelta() : null;
  }

  @Override
  public TransformedWaveletDelta getTransformedDelta(long version) throws IOException {

    WaveletDeltaRecord delta = getDelta(version);
    return (delta != null) ? delta.getTransformedDelta() : null;
  }

  @Override
  public void close() throws IOException {
    // Does nothing.
  }

  @Override
  public void append(Collection<WaveletDeltaRecord> newDeltas) throws PersistenceException {

    deltasCollection.withWriteConcern(WriteConcern.JOURNALED).insertMany(

        newDeltas.stream().map((Function<? super WaveletDeltaRecord, ? extends BasicDBObject>) (
            WaveletDeltaRecord delta) -> {

          return MongoDbDeltaStoreUtil.serialize(delta, waveletName.waveId.serialise(),
              waveletName.waveletId.serialise());

        }).collect(Collectors.toList())

    );

  }

  /**
   * Use this exception to halt a forEach() call but it is not an actual
   * failure.
   */
  private class DeltaReaderHaltedException extends RuntimeException {
    public final WaveletDeltaRecord lastDelta;

    public DeltaReaderHaltedException(WaveletDeltaRecord lastDelta) {
      this.lastDelta = lastDelta;
    }
  }

  private class DeltaReader implements Block<BasicDBObject> {


    long count = 0;
    WaveletDeltaRecord lastProcDelta = null;
    public Exception exception = null;
    Receiver<WaveletDeltaRecord> receiver = null;
    final boolean ascendingSort;

    public DeltaReader(Receiver<WaveletDeltaRecord> receiver, boolean ascendingSort) {
      this.receiver = receiver;
      this.ascendingSort = ascendingSort;
    }

    public DeltaReader(Receiver<WaveletDeltaRecord> receiver) {
      this.receiver = receiver;
      this.ascendingSort = true;
    }

    @Override
    public void apply(BasicDBObject obj) {

      WaveletDeltaRecord delta;
      try {
        delta = MongoDbDeltaStoreUtil.deserializeWaveletDeltaRecord(obj);
      } catch (PersistenceException e) {
        exception = e;
        return;
      }

      if (lastProcDelta != null) {
        if ( (ascendingSort && !delta.getAppliedAtVersion().equals(lastProcDelta.getResultingVersion())) ||
            (!ascendingSort && !delta.getResultingVersion().equals(lastProcDelta.getAppliedAtVersion()))) {
            LOG.warning("Delta history integrity error? Skipping delta at applied version=" + delta.getAppliedAtVersion().getVersion());
            return;
          }
      }

      boolean halt = !receiver.put(delta);

      lastProcDelta = delta;
      count++;

      if (halt)
        throw new DeltaReaderHaltedException(lastProcDelta);
    }

  }

  @Override
  public long getAllDeltas(Receiver<WaveletDeltaRecord> receiver) throws IOException {


    BasicDBObject sort = new BasicDBObject();
    sort.put(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_RESULTINGVERSION_VERSION, 1);

    DeltaReader block = new DeltaReader(receiver);
    deltasCollection.find(createWaveletDBQuery()).sort(sort).forEach(block);

    if (block.exception != null)
      throw new IOException(block.exception);


    return block.count;
  }

  @Override
  public long getDeltasInRange(long startVersion, long endVersion,
      Receiver<WaveletDeltaRecord> receiver) throws IOException {

    boolean ascendingSort = startVersion < endVersion;

    BasicDBObject sort = new BasicDBObject();
    sort.put(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_RESULTINGVERSION_VERSION,
        ascendingSort ? 1 : -1);

    Bson query = null;
    if (ascendingSort) {

      query = Filters.and(createWaveletDBQuery(),
        Filters.gte(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_APPLIEDATVERSION, startVersion),
        Filters.lte(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_RESULTINGVERSION_VERSION, endVersion));

    } else {

      query = Filters.and(createWaveletDBQuery(),
          Filters.gte(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_APPLIEDATVERSION, endVersion),
          Filters.lte(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_RESULTINGVERSION_VERSION,
              startVersion));
    }

    DeltaReader block = new DeltaReader(receiver, ascendingSort);
    try {
      deltasCollection.find(query).sort(sort).forEach(block);
    } catch (DeltaReaderHaltedException e) {

    }
    if (block.exception != null)
      throw new IOException(block.exception);


    return block.count;
  }

  @Override
  public WaveletDeltaRecord getLastDelta() throws IOException {

    // Search the max of delta.getTransformedDelta().getResultingVersion()


    BasicDBObject sort = new BasicDBObject();
    sort.put(MongoDbDeltaStoreUtil.FIELD_TRANSFORMED_RESULTINGVERSION_VERSION, -1); // Descending


    BasicDBObject result = deltasCollection.find(createWaveletDBQuery()).sort(sort).first();

    try {
      return result != null ? MongoDbDeltaStoreUtil
          .deserializeWaveletDeltaRecord(result) : null;
    } catch (PersistenceException e) {
      throw new IOException(e);
    }

  }

  @Override
  public DeltaStore.Snapshot loadSnapshot() throws PersistenceException {
    return snapshotStore.load(waveletName);
  }

  @Override
  public void storeSnapshot(WaveletData waveletData)
      throws PersistenceException {

    Preconditions.checkArgument(waveletName.equals(WaveletName.of(waveletData.getWaveId(), waveletData.getWaveletId())),
        "Can't store snapshots for different wavelet");

    snapshotStore.store(waveletData);
  }

  @Override
  public WaveletContributions loadContributions() throws PersistenceException {
    LOG.fine("Wavelet contributions storage is not yet available");
    return null;
  }

  @Override
  public WaveletContributions loadContributionsForVersion(long version)
      throws PersistenceException {
    LOG.fine("Wavelet contributions storage is not yet available");
    return null;
  }

  @Override
  public void storeContributions(WaveletContributions contributions) throws PersistenceException {
    LOG.fine("Wavelet contributions storage is not yet available");
  }
}
