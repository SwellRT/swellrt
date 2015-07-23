package org.swellrt.server.box;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;

import org.swellrt.model.ModelToMongoVisitor;
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
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.HashMap;

import javax.inject.Inject;

/**
 * A simplistic implementation of SwellRT persistence to mongoDB
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SwellRtIndexerDispatcherImpl implements SwellRtIndexerDispatcher {

  private static final Log LOG = Log.get(SwellRtIndexerDispatcherImpl.class);

  private final WaveMap waveMap;
  private final WaveletProvider waveletProvider;
  private DBCollection store;

  @Inject
  public SwellRtIndexerDispatcherImpl(MongoDbProvider mongoDbProvider,
 WaveMap waveMap,
      WaveletProvider waveletProvider) {
    try {
      this.store = mongoDbProvider.getDBCollection(SwellRtModule.MONGO_COLLECTION);
    } catch (Exception e) {
      LOG.warning("Unable to get MongoDB collection. SwellRT indexing won't work!", e);
      this.store = null;
    }
    this.waveletProvider = waveletProvider;
    this.waveMap = waveMap;
  }

  HashMap<WaveletId, ReadableWaveletData> uncommittedWavelet =
      new HashMap<WaveletId, ReadableWaveletData>();

  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, DeltaSequence deltas) {
    // Keep the latest version of wavelets in the uncommitted map of wavelets
    ReadableWaveletData previousWavelet = uncommittedWavelet.get(wavelet.getWaveletId());
    if (previousWavelet == null
        || previousWavelet.getHashedVersion().getVersion() < wavelet.getHashedVersion()
            .getVersion()) {
      uncommittedWavelet.put(wavelet.getWaveletId(), wavelet);
    }
  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version) {

    ReadableWaveletData wavelet = uncommittedWavelet.get(waveletName.waveletId);
    if (wavelet != null) {
      if (wavelet.getHashedVersion().equals(version)) {
        uncommittedWavelet.remove(waveletName.waveletId);
        store(wavelet);
      } else {
        LOG.warning("Wavelet committed but version to store not found");
      }
    } else {
      LOG.warning("Wavelet committed but data not found");
    }
  }

  protected void store(ReadableWaveletData wavelet) {
    if (store == null) return;
    try {
      BasicDBObject keyObj = (BasicDBObject) store.findOne(new BasicDBObject("wave_id", wavelet.getWaveId().serialise()), new BasicDBObject("_id",1) );
      if (keyObj == null || keyObj.isEmpty())
        store.insert(ModelToMongoVisitor.getDBObject(UnmutableModel.create(wavelet)));
      else
        store.update(keyObj, ModelToMongoVisitor.getDBObject(UnmutableModel.create(wavelet)), true,
          false);

      LOG.info("Indexed " + wavelet.getWaveId().toString() + " as data model");
    } catch (Exception e) {
      LOG.warning("Error indexing " + wavelet.getWaveId().toString(), e);
    }
  }

  protected boolean isStored(WaveletName waveletName) {
    if (store == null) return false;
    return store.count(new BasicDBObject("wave_id", waveletName.waveId.serialise())) > 0;
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

            if (!isStored(waveletName)) {
              store(waveletProvider.getSnapshot(waveletName).snapshot);
            }
          }
        }
      }

    }

  }
}
