package org.waveprotocol.box.server.swell.indexer;

import java.util.concurrent.Executor;

import org.waveprotocol.box.common.DeltaSequence;
import org.waveprotocol.box.server.waveserver.WaveBus.Subscriber;
import org.waveprotocol.box.server.waveserver.WaveletProvider;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersion;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;


public class WaveletToMongoIndexer implements Subscriber {

  private static final Log LOG = Log.get(WaveletToMongoIndexer.class);

  private final Executor executor;
  private final WaveletProvider waveletProvider;

  public WaveletToMongoIndexer(Executor mongoIndexerExecutor, WaveletProvider waveletProvider) {
    this.executor = mongoIndexerExecutor;
    this.waveletProvider = waveletProvider;
  }

  @Override
  public void waveletUpdate(ReadableWaveletData wavelet, DeltaSequence deltas) {

  }

  @Override
  public void waveletCommitted(WaveletName waveletName, HashedVersion version) {
    // TODO Auto-generated method stub

  }

}
