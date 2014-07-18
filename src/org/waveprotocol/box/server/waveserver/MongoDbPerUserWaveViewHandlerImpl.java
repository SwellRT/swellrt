package org.waveprotocol.box.server.waveserver;

import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.IndexExecutor;
import org.waveprotocol.box.server.persistence.mongodb.MongoDbIndexStore;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;

/**
 * A mongoDB based Wave view handler.
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 * 
 */
@Singleton
public class MongoDbPerUserWaveViewHandlerImpl implements PerUserWaveViewHandler {

  private static final Log LOG = Log.get(MongoDbPerUserWaveViewHandlerImpl.class);

  private final String waveDomain;
  private final Executor executor;
  private final MongoDbIndexStore indexStore;

  /** TODO remove when full mongodb version is completed */
  private final MemoryPerUserWaveViewHandlerImpl memoryViewHandler;

  @Inject
  public MongoDbPerUserWaveViewHandlerImpl(
      @Named(CoreSettings.WAVE_SERVER_DOMAIN) final String waveDomain,
      @IndexExecutor Executor executor, MongoDbIndexStore indexStore,
      MemoryPerUserWaveViewHandlerImpl memoryViewHandler) {

    this.waveDomain = waveDomain;
    this.executor = executor;
    this.indexStore = indexStore;

    this.memoryViewHandler = memoryViewHandler;

    LOG.info("Started mongoDb wave indexer");
  }

  @Override
  public ListenableFuture<Void> onParticipantAdded(WaveletName waveletName,
      ParticipantId participant) {

    return memoryViewHandler.onParticipantAdded(waveletName, participant);
  }

  @Override
  public ListenableFuture<Void> onParticipantRemoved(WaveletName waveletName,
      ParticipantId participant) {

    return memoryViewHandler.onParticipantRemoved(waveletName, participant);
  }

  @Override
  public ListenableFuture<Void> onWaveUpdated(final ReadableWaveletData waveletData) {


    Preconditions.checkNotNull(waveletData);

    ListenableFutureTask<Void> task = ListenableFutureTask.<Void> create(new Callable<Void>() {

      @Override
      public Void call() throws Exception {
        indexStore.indexWavelet(waveletData);
        return null;
      }
    });
    executor.execute(task);
    return task;

  }

  @Override
  public Multimap<WaveId, WaveletId> retrievePerUserWaveView(ParticipantId user) {

    return memoryViewHandler.retrievePerUserWaveView(user);
  }

  @Override
  public ListenableFuture<Void> onWaveInit(WaveletName waveletName) {
    // No op.
    SettableFuture<Void> task = SettableFuture.create();
    task.set(null);
    return task;
  }




}
