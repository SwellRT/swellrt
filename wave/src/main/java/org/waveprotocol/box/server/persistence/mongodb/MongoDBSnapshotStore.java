package org.waveprotocol.box.server.persistence.mongodb;

import org.waveprotocol.box.common.comms.WaveClientRpc.WaveletSnapshot;
import org.waveprotocol.box.server.common.SnapshotSerializer;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.box.server.waveserver.DeltaStore;
import org.waveprotocol.wave.model.id.InvalidIdException;
import org.waveprotocol.wave.model.id.ModernIdSerialiser;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.model.wave.data.WaveletData;
import org.waveprotocol.wave.util.logging.Log;

import com.google.protobuf.InvalidProtocolBufferException;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;

/**
 * A MongoDB-backed store of Wavelet snapshots.
 * The aim is to avoid the whole processing of deltas when a
 * wavelet is loaded into server's memory for the first time.
 *
 * This class is not thread-safe.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class MongoDBSnapshotStore {

  private static final Log LOG = Log.get(MongoDBSnapshotStore.class);

  protected static final String WAVE_ID_FIELD  = "waveid";
  protected static final String WAVELET_ID_FIELD = "waveletid";
  protected static final String VERSION_FIELD = "version";
  protected static final String VERSION_HASH_FIELD = "versionhash";
  protected static final String LASTMOD_FIELD = "lastmod";

  protected static final String SNAPSHOT_DATA = "data";


  /** Name of the MongoDB collection to store Deltas */
  protected static final String SNAPSHOT_COLLECTION = "snapshots";


  private final MongoCollection<BasicDBObject> collection;

  /**
   * Get a reference to the snapshots store.
   *
   * @param database
   * @return
   */
  public static MongoDBSnapshotStore create(MongoDatabase database) {
      Preconditions.checkArgument(database != null, "Unable to get reference to mongoDB snapshots collection");
    MongoCollection<BasicDBObject> collection = database.getCollection(SNAPSHOT_COLLECTION,
        BasicDBObject.class);
      return new MongoDBSnapshotStore(collection);
  }

  /**
   * Construct a new snapshots store.
   *
   * @param database the database connection object
   */
  protected MongoDBSnapshotStore(MongoCollection<BasicDBObject> collection) {
    this.collection = collection;
  }

  protected void deleteSnapshot(WaveletName waveletName) throws PersistenceException {

    BasicDBObject criteria = new BasicDBObject();
    criteria.put(WAVE_ID_FIELD, ModernIdSerialiser.INSTANCE.serialiseWaveId(waveletName.waveId));
    criteria.put(WAVELET_ID_FIELD, ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletName.waveletId));

    try {
      // Using Journaled Write Concern
      // (http://docs.mongodb.org/manual/core/write-concern/#journaled)
      collection.withWriteConcern(WriteConcern.JOURNALED).deleteMany(criteria);
    } catch (MongoException e) {
      throw new PersistenceException(e);
    }
  }

  /**
   * Store a snapshot
   *
   * @param waveletData
   * @param hashedVersion
   * @throws PersistenceException
   */
  public void store(ReadableWaveletData waveletData) throws PersistenceException {

    String waveId = ModernIdSerialiser.INSTANCE.serialiseWaveId(waveletData.getWaveId());
    String waveletId = ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletData.getWaveletId());


    // store new snapshot
    BasicDBObject dbo = new BasicDBObject();
    dbo.put(WAVE_ID_FIELD, ModernIdSerialiser.INSTANCE.serialiseWaveId(waveletData.getWaveId()));
    dbo.put(WAVELET_ID_FIELD, ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletData.getWaveletId()));

    dbo.put(VERSION_FIELD, waveletData.getHashedVersion().getVersion());
    dbo.put(VERSION_HASH_FIELD, waveletData.getHashedVersion().getHistoryHash());
    dbo.put(LASTMOD_FIELD,waveletData.getLastModifiedTime());


    WaveletSnapshot snapshot = SnapshotSerializer.serializeWavelet(waveletData, waveletData.getHashedVersion());
    dbo.put(SNAPSHOT_DATA,snapshot.toByteArray());

    try {
      collection.insertOne(dbo);
    } catch (MongoException e) {
      LOG.warning("Error storing wavelet snapshot for "+waveId+"/"+waveletId, e);
      throw new PersistenceException(e);
    }

    try {
        collection.deleteMany(Filters.and(
            Filters.eq(WAVE_ID_FIELD,
                ModernIdSerialiser.INSTANCE.serialiseWaveId(waveletData.getWaveId())),
            Filters.eq(WAVELET_ID_FIELD,
                ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletData.getWaveletId())),
            Filters.lt(VERSION_FIELD, waveletData.getHashedVersion().getVersion())));

    } catch (MongoException e) {
      LOG.warning("Error deleting outdated wavelet snapshots for "+waveId+"/"+waveletId, e);
      throw new PersistenceException(e);
    }



    LOG.fine("Stored snaphost for "+waveId+"/"+waveletId+" version "+ waveletData.getHashedVersion().getVersion());
  }


  public DeltaStore.Snapshot load(WaveletName waveletName) throws PersistenceException {

    String waveId = ModernIdSerialiser.INSTANCE.serialiseWaveId(waveletName.waveId);
    String waveletId = ModernIdSerialiser.INSTANCE.serialiseWaveletId(waveletName.waveletId);

    // find last snapshot stored
    DBObject query = new BasicDBObject();
    query.put(WAVE_ID_FIELD, waveId);
    query.put(WAVELET_ID_FIELD, waveletId);

    BasicDBObject snapshotDBObject = null;

    try {
      snapshotDBObject = collection.find(Filters.and(
          Filters.eq(WAVE_ID_FIELD, waveId),
          Filters.eq(WAVELET_ID_FIELD, waveletId))).first();
    } catch (MongoException e) {
      LOG.warning("Error querying wavelet snapshots for "+waveId+"/"+waveletId, e);
      throw new PersistenceException(e);
    }

    if (snapshotDBObject == null)
      return null;


    WaveletSnapshot snapshot = null;
    try {
      snapshot = WaveletSnapshot.parseFrom((byte[]) snapshotDBObject.get(SNAPSHOT_DATA));
    } catch (InvalidProtocolBufferException e) {
      throw new PersistenceException(e);
    }

    try {

      final WaveletData waveletData = SnapshotSerializer.deserializeWavelet(snapshot, waveletName.waveId);

      return new DeltaStore.Snapshot() {

        @Override
        public WaveletData getWaveletData() {

          return waveletData;
        }

      };

    } catch (OperationException e) {
      throw new PersistenceException(e);
    } catch (InvalidParticipantAddress e) {
      throw new PersistenceException(e);
    } catch (InvalidIdException e) {
      throw new PersistenceException(e);
    }
  }
}
