package org.swellrt.server.box;

import com.google.common.base.Preconditions;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;

import org.swellrt.model.unmutable.UnmutableModel;
import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.common.ExceptionalIterator;
import org.waveprotocol.box.server.persistence.mongodb.MongoDbProvider;
import org.waveprotocol.box.server.waveserver.WaveMap;
import org.waveprotocol.box.server.waveserver.WaveServerException;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.operation.wave.TransformedWaveletDelta;
import org.waveprotocol.wave.model.operation.wave.WaveletBlipOperation;
import org.waveprotocol.wave.model.operation.wave.WaveletOperation;
import org.waveprotocol.wave.model.util.ConcurrentSet;
import org.waveprotocol.wave.model.util.Pair;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.inject.Inject;

/**
 * A simplistic implementation of SwellRT persistence to mongoDB.
 *
 * TODO(pablojan) check multi thread behavior on data structures. *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ModelIndexerDispatcherImpl implements ModelIndexerDispatcher {



  private static final Log LOG = Log.get(ModelIndexerDispatcherImpl.class);

  private final WaveMap waveMap;
  private final WaveletProvider waveletProvider;
  /** Store snaphots of whole models */
  private DBCollection modelStore;
  /** Store change log of model's documents */
  private DBCollection modelLogStore;

  @Inject
  public ModelIndexerDispatcherImpl(MongoDbProvider mongoDbProvider,
 WaveMap waveMap,
      WaveletProvider waveletProvider) {
    try {
      this.modelStore = mongoDbProvider.getDBCollection(ModelIndexerModule.MONGO_COLLECTION_MODELS);
      this.modelLogStore =
          mongoDbProvider.getDBCollection(ModelIndexerModule.MONGO_COLLECTION_MODELS_LOG);
    } catch (Exception e) {
      LOG.warning("Unable to get MongoDB collection. SwellRT indexing won't work!", e);
      this.modelStore = null;
    }
    this.waveletProvider = waveletProvider;
    this.waveMap = waveMap;
  }

  /**
   * A list of Wavelets to be stored in mongo on commit.
   */
  ConcurrentMap<WaveletName, ReadableWaveletData> uncommittedWavelet =
      new ConcurrentHashMap<WaveletName, ReadableWaveletData>();

  /**
   * Deltas to store grouped by author
   */
  ConcurrentMap<WaveletName, ConcurrentSet<DeltaSequence>> uncommitedDeltas =
      new ConcurrentHashMap<WaveletName, ConcurrentSet<DeltaSequence>>();


  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, DeltaSequence deltas) {

    WaveletName waveletName = WaveletName.of(wavelet.getWaveId(), wavelet.getWaveletId());
    /**
     * Wavelet views are updated.
     */
    uncommittedWavelet.put(waveletName, wavelet);

    /**
     * Deltas are accumulated.
     */
    if (!uncommitedDeltas.containsKey(waveletName))
      uncommitedDeltas.put(waveletName, new ConcurrentSet<DeltaSequence>());

    if (deltas != null)
      uncommitedDeltas.get(waveletName).add(deltas);
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version) {

    ReadableWaveletData wavelet = uncommittedWavelet.get(waveletName);

    // Extract deltas only up to the commited hashed version
    ConcurrentSet<DeltaSequence> deltas = uncommitedDeltas.get(waveletName);
    ArrayList<TransformedWaveletDelta> committedDeltas = new ArrayList<TransformedWaveletDelta>();
    deltas.lock();
    for (DeltaSequence ds : deltas) {
      if (ds.getEndVersion().getVersion() <= version.getVersion()) {
        committedDeltas.addAll(ds);
        deltas.remove(ds);
      }
    }
    deltas.unlock();


    if (wavelet != null) {
      uncommittedWavelet.remove(waveletName);

      try {
        index(wavelet, committedDeltas);
      } catch (RuntimeException e) {
        LOG.info("Error indexing model " + waveletName.toString() + ", " + e.getMessage());
      }
    } else {
      LOG.warning("Wavelet committed but data not found");
    }
  }



  protected void index(ReadableWaveletData wavelet, List<TransformedWaveletDelta> deltas) {
    Preconditions.checkNotNull(modelStore);
    Preconditions.checkNotNull(modelLogStore);

    WaveletName waveletName = WaveletName.of(wavelet.getWaveId(), wavelet.getWaveletId());

    UnmutableModel model = UnmutableModel.create(wavelet);

    if (model == null) {
      LOG.warning("Unable to build a data model from wavelet " + waveletName.toString());
      return;
    }


    Pair<BasicDBObject, Map<String, String>> visitResult = ModelIndexerVisitor.run(model);

    storeDataModel(waveletName, visitResult.first);

    if (deltas != null) storeDeltas(waveletName, deltas, visitResult.second);

  }

  /**
   * A remove-insert logic to store static view of collaborative data models
   *
   */
  protected void storeDataModel(WaveletName waveletName, BasicDBObject dataModelDBObject) {
    //
    // Store data model snapshot
    //
    try {
      BasicDBObject keyObj =
          (BasicDBObject) modelStore.findOne(
              new BasicDBObject("wave_id", waveletName.waveId.serialise()),
              new BasicDBObject("_id", 1));
      if (keyObj == null || keyObj.isEmpty()) {
        modelStore.insert(dataModelDBObject);
      }
      else
        modelStore.update(keyObj, dataModelDBObject, true,
          false);

      LOG.info("Data model indexed successfully " + waveletName.toString());
    } catch (Exception e) {
      LOG.warning("Error indexing data model " + waveletName.toString(), e);
    }
  }


  protected void storeDeltas(WaveletName waveletName, List<TransformedWaveletDelta> deltas,
      Map<String, String> blipIdToPathMap) {

    //
    // Store versions
    //

    // Store per blip changes grouped by author. Example:
    //
    // blipId=1 author=tom startversion=1 endversion=100
    // blipId=1 author=sam startversion=101 endversion=110
    // blipId=1 author=tom startversion=111 endversion=200
    //
    // Two consequtive entries will always have different authors

    try {

      for (TransformedWaveletDelta d: deltas) {

        // Chech if delta has blip ops
        boolean hasBlipOp = false;
        String blipId = null;

        for (WaveletOperation op: d) {

          if (op instanceof WaveletBlipOperation) {
            hasBlipOp = true;
            blipId = ((WaveletBlipOperation) op).getBlipId();
            break;
          }

        }

        // Store the delta if it affects a mapped data model object
        if (hasBlipOp && blipIdToPathMap.containsKey(blipId)) {

          String deltaAuthor = d.getAuthor().getAddress();

          BasicDBObject lowerDelta = getLowerDelta(waveletName, blipId, d);
          BasicDBObject upperDelta = getUpperDelta(waveletName, blipId, d);
          BasicDBObject includingDelta = getIncludingDelta(waveletName, blipId, d);

          if (lowerDelta == null && upperDelta == null) {

            if (includingDelta == null) {
              // insert delta
              BasicDBObject deltaDB = buildDeltaDBObject(waveletName,
                  blipId,
                  d.getAppliedAtVersion(), d.getApplicationTimestamp(),
                  d.getResultingVersion().getVersion(), d.getApplicationTimestamp(),
                  blipIdToPathMap.get(blipId),
                  deltaAuthor);

              modelLogStore.insert(WriteConcern.ACKNOWLEDGED, deltaDB);

            }

          } else {


            BasicDBObject deltaDB = buildDeltaDBObject(waveletName,
                blipId,
                d.getAppliedAtVersion(), d.getApplicationTimestamp(),
                d.getResultingVersion().getVersion(), d.getApplicationTimestamp(),
                blipIdToPathMap.get(blipId),
                deltaAuthor);


            if (lowerDelta != null && deltaAuthor.equals(lowerDelta.getString("author"))) {
               // delta <- join(lowerDelta, delta)

              deltaDB.put("startversion", lowerDelta.get("startversion"));
              deltaDB.put("starttimestamp", lowerDelta.get("starttimestamp"));

              // Remove old lower delta
              modelLogStore.remove(lowerDelta, WriteConcern.ACKNOWLEDGED);
            }


            if (upperDelta != null && deltaAuthor.equals(upperDelta.getString("author"))) {

              deltaDB.put("endversion", lowerDelta.get("endversion"));
              deltaDB.put("endtimestamp", lowerDelta.get("endtimestamp"));

              // Remove old upper delta
              modelLogStore.remove(upperDelta, WriteConcern.ACKNOWLEDGED);
            }

            // insert delta

            modelLogStore.insert(WriteConcern.ACKNOWLEDGED, deltaDB);

          }


        }

      } // For deltas

    } catch (Exception e) {
      LOG.warning("Error storing data model version log " + waveletName.toString(), e);
    }

  }


  protected BasicDBObject buildDeltaDBObject(WaveletName waveletName, String blipId, long startVersion, long startTimestamp, long endVersion, long endTimestamp, String path, String author) {
    BasicDBObject o = new BasicDBObject();
    o.put("waveid", waveletName.waveId.serialise());
    o.put("waveletid", waveletName.waveletId.serialise());
    o.put("blipid", blipId);
    o.put("startversion", startVersion);
    o.put("endversion", endVersion);
    o.put("starttimestamp", startTimestamp);
    o.put("endtimestamp", endTimestamp);
    o.put("path", path);
    o.put("author", author);

    return o;
  }


  protected BasicDBObject getLowerDelta(WaveletName waveletName, String blipId,
      TransformedWaveletDelta delta) {

    BasicDBObject query = new BasicDBObject();
    query.put("waveid", waveletName.waveId.serialise());
    query.put("waveletid", waveletName.waveletId.serialise());
    query.put("blipid", blipId);
    query.put("endversion", new BasicDBObject("$lte", delta.getAppliedAtVersion()));

    BasicDBObject order = new BasicDBObject();
    order.put("endversion", -1);

    return (BasicDBObject) modelLogStore.findOne(query, new BasicDBObject(), order);

  }

  protected BasicDBObject getUpperDelta(WaveletName waveletName, String blipId,
      TransformedWaveletDelta delta) {

    BasicDBObject query = new BasicDBObject();
    query.put("waveid", waveletName.waveId.serialise());
    query.put("waveletid", waveletName.waveletId.serialise());
    query.put("blipid", blipId);
    query.put("startversion", new BasicDBObject("$gte", delta.getResultingVersion().getVersion()));

    BasicDBObject order = new BasicDBObject();
    order.put("startversion", 1);

    return (BasicDBObject) modelLogStore.findOne(query, new BasicDBObject(), order);

  }

  protected BasicDBObject getIncludingDelta(WaveletName waveletName, String blipId,
      TransformedWaveletDelta delta) {

    BasicDBObject query = new BasicDBObject();
    query.put("waveid", waveletName.waveId.serialise());
    query.put("waveletid", waveletName.waveletId.serialise());
    query.put("blipid", blipId);
    query.put("startversion", new BasicDBObject("$lte", delta.getAppliedAtVersion()));
    query.put("endversion", new BasicDBObject("$gte", delta.getResultingVersion().getVersion()));
    BasicDBObject order = new BasicDBObject();
    order.put("startversion", -1);

    return (BasicDBObject) modelLogStore.findOne(query, new BasicDBObject(), order);

  }


  protected boolean isModelStored(WaveletName waveletName) {
    if (modelStore == null) return false;
    return modelStore.count(new BasicDBObject("wave_id", waveletName.waveId.serialise())) > 0;
  }

  /**
   * Add to the index all non already added data model Wavelets
   *
   * @throws WaveServerException
   */
  @Override
  public void initialize() throws WaveServerException {

    waveMap.loadAllWavelets();

    ExceptionalIterator<WaveId, WaveServerException> witr = waveletProvider.getWaveIds();
    while (witr.hasNext()) {
      WaveId waveId = witr.next();

      if (waveId.getId().startsWith("s+")) {
        for (WaveletId waveletId : waveletProvider.getWaveletIds(waveId)) {

          if (waveletId.getId().equals("swl+root")) {
            WaveletName waveletName = WaveletName.of(waveId, waveletId);

            if (!isModelStored(waveletName)) {

              try {
                index(waveletProvider.getSnapshot(waveletName).snapshot, null);
              } catch (RuntimeException e) {
                LOG.info("Error indexing model " + waveletName.toString(), e);
              }


            }
          }
        }
      }

    }

  }


}
