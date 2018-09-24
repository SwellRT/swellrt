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

import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.persistence.FileNotFoundPersistenceException;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.box.server.waveserver.DeltaStoreTransient;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.util.logging.Log;

import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
/**
 * A MongoDB based Delta Store implementation using a <b>deltas</b>
 * collection and a snapshots collection.
 *
 * Each deltas is stored as a MongoDb document.
 *
 * For snapshots store details check out {@link MongoDBSnapshotStore}
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class MongoDbDeltaStore implements DeltaStore, DeltaStoreTransient {

  private static final Log LOG = Log.get(MongoDbDeltaStore.class);

  /** Name of the MongoDB collection to store Deltas */
  private static final String DELTAS_COLLECTION = "deltas";

  /** MongoDB Collection for deltas */
  private final MongoCollection<BasicDBObject> deltasCollection;

  /** A specific class handling snapshots */
  private final MongoDBSnapshotStore snapshotStore;

  /**
   * Creates a mongoDB based delta/snapshot store.
   *
   * @param database
   * @return
   */
  public static MongoDbDeltaStore create(MongoDatabase database) {

    MongoCollection<BasicDBObject> deltasCollection = database.getCollection(DELTAS_COLLECTION,
        BasicDBObject.class);
    checkDeltasCollectionIndexes(deltasCollection);
    cleanTransientDeltaStores(deltasCollection);

    MongoDBSnapshotStore snapshotStore = MongoDBSnapshotStore.create(database);

    return new MongoDbDeltaStore(deltasCollection, snapshotStore);
  }

  /**
   * Transient wavelets should only store data that could be delete eventually.
   * However, deletion process must work in coordination with federated servers
   * to keep data consistency among servers. <br>
   * By now, it is only safe to delete those transient wavelets but not shared
   * with other domains.
   *
   * @param deltasCollection
   */
  private static void cleanTransientDeltaStores(MongoCollection<BasicDBObject> deltasCollection) {

    /*
     * try {
     *
     * LOG.warning("Deleting transient wavelets data");
     *
     * // Using Journaled Write Concern //
     * (http://docs.mongodb.org/manual/core/write-concern/#journaled)
     * deltasCollection.withWriteConcern(WriteConcern.JOURNALED) .deleteMany(
     * Filters.regex(MongoDbDeltaStoreUtil.FIELD_WAVELET_ID,
     * Pattern.compile("\\+" + IdConstants.TRANSIENT_WAVELET_PREFIX))); } catch
     * (MongoException e) {
     * LOG.warning("Exception cleaning up transient data from database", e); }
     */

  }

  private static void checkDeltasCollectionIndexes(
      MongoCollection<BasicDBObject> deltasCollection) {
    // List<DBObject> indexInfo = deltasCollection.getIndexInfo();

    LOG.warning(
        "For prod environment, set mongoDB indexes in 'deltas' collection for 'waveid', 'waveletid' and 'transformed.resultingversion' fields");

    BasicDBObject newIndex = new BasicDBObject();
    newIndex.put("transformed.resultingversion.version", 1);
    deltasCollection.createIndex(newIndex);

    newIndex = new BasicDBObject();
    newIndex.put("waveid", 1);
    newIndex.put("waveletid", 1);
    deltasCollection.createIndex(newIndex);
    deltasCollection.createIndex(newIndex);
  }


  /**
   * Construct a new store instance
   *
   * @param database the database connection object
   */
  private MongoDbDeltaStore(MongoCollection<BasicDBObject> deltasCollection, MongoDBSnapshotStore snapshotStore) {
    this.deltasCollection = deltasCollection;
    this.snapshotStore = snapshotStore;
  }



  @Override
  public DeltasAccess open(WaveletName waveletName) throws PersistenceException {
    return new MongoDbDeltaCollection(waveletName, deltasCollection, snapshotStore);
  }

  @Override
  public void delete(WaveletName waveletName) throws PersistenceException,
      FileNotFoundPersistenceException {

    BasicDBObject criteria = new BasicDBObject();
    criteria.put(MongoDbDeltaStoreUtil.FIELD_WAVE_ID, waveletName.waveId.serialise());
    criteria.put(MongoDbDeltaStoreUtil.FIELD_WAVELET_ID, waveletName.waveletId.serialise());

    try {
      // Using Journaled Write Concern
      // (http://docs.mongodb.org/manual/core/write-concern/#journaled)
      deltasCollection.withWriteConcern(WriteConcern.JOURNALED).deleteMany(criteria);
    } catch (MongoException e) {
      throw new PersistenceException(e);
    }

    // Also delete wavelet snapshots
    snapshotStore.deleteSnapshot(waveletName);

  }

  @Override
  public ImmutableSet<WaveletId> lookup(WaveId waveId) throws PersistenceException {


    BasicDBObject query = new BasicDBObject();
    query.put(MongoDbDeltaStoreUtil.FIELD_WAVE_ID, waveId.serialise());

    BasicDBObject projection = new BasicDBObject();
    projection.put(MongoDbDeltaStoreUtil.FIELD_WAVELET_ID, 1);


    final ImmutableSet.Builder<WaveletId> builder = ImmutableSet.builder();
    try {
      deltasCollection.find(query).projection(projection).forEach(new Block<BasicDBObject>() {

        @Override
        public void apply(BasicDBObject t) {
          builder
              .add(WaveletId.deserialise((String) t.get(MongoDbDeltaStoreUtil.FIELD_WAVELET_ID)));
        }

      });
    } catch (MongoException e) {
      throw new PersistenceException(e);
    }

    return builder.build();
  }

  @Override
  public ExceptionalIterator<WaveId, PersistenceException> getWaveIdIterator()
      throws PersistenceException {

    ImmutableSet.Builder<WaveId> builder = ImmutableSet.builder();

    try {

      deltasCollection.distinct(MongoDbDeltaStoreUtil.FIELD_WAVE_ID, String.class)
          .forEach((Block<String>) (String value) -> {
            builder.add(WaveId.deserialise(value));
          });

    } catch (MongoException e) {
      throw new PersistenceException(e);
    }


    return ExceptionalIterator.FromIterator.create(builder.build().iterator());
  }

}
