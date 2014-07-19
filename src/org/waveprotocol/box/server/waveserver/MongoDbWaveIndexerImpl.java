package org.waveprotocol.box.server.waveserver;

import com.google.inject.Inject;

import org.waveprotocol.box.server.persistence.mongodb.MongoDbIndexStore;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.util.logging.Log;

public class MongoDbWaveIndexerImpl extends AbstractWaveIndexer {

  private static final Log LOG = Log.get(MongoDbWaveIndexerImpl.class);

  private final MongoDbIndexStore indexStore;
  private final ReadableWaveletDataProvider waveletReaderProvider;

  @Inject
  public MongoDbWaveIndexerImpl(WaveMap waveMap, WaveletProvider waveletProvider,
      ReadableWaveletDataProvider waveletReaderProvider,
      MongoDbIndexStore indexStore) {
    super(waveMap, waveletProvider);
    this.indexStore = indexStore;
    this.waveletReaderProvider = waveletReaderProvider;
  }

  @Override
  protected void processWavelet(WaveletName waveletName) {
    try {
      indexStore.indexWavelet(waveletReaderProvider.getReadableWaveletData(waveletName));
    } catch (WaveServerException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void postIndexHook() {
    // TODO Auto-generated method stub

  }

}
