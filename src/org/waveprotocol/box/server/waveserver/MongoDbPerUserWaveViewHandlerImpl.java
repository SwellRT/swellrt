package org.waveprotocol.box.server.waveserver;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
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
import org.waveprotocol.wave.model.id.IdUtil;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.id.WaveletId;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.ReadableWaveletData;
import org.waveprotocol.wave.util.logging.Log;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

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

  /**
   * Index a wavelet if it has not been updated during last X seconds. This is a
   * throttle for the index store.
   */
  private static final long CONTENT_WAVELET_INDEX_DELAY_SECONDS = 20;

  private final Cache<WaveletName, ReadableWaveletData> waveletDelayCache;

  @Inject
  public MongoDbPerUserWaveViewHandlerImpl(
      @Named(CoreSettings.WAVE_SERVER_DOMAIN) final String waveDomain,
      @IndexExecutor Executor executor, MongoDbIndexStore indexStore) {

    this.waveDomain = waveDomain;
    this.executor = executor;
    this.indexStore = indexStore;

    waveletDelayCache =
        CacheBuilder.newBuilder()
            .expireAfterWrite(CONTENT_WAVELET_INDEX_DELAY_SECONDS, TimeUnit.SECONDS)
            .removalListener(new RemovalListener<WaveletName, ReadableWaveletData>() {

              @Override
              public void onRemoval(
                  final RemovalNotification<WaveletName, ReadableWaveletData> notification) {

                if (!notification.getCause().equals(RemovalCause.EXPIRED)) return;

                LOG.info("Wavelet " + notification.getKey() + " for indexing after delay period");

                ListenableFutureTask<Void> task =
                    ListenableFutureTask.<Void> create(new Callable<Void>() {

                      @Override
                      public Void call() throws Exception {
                        MongoDbPerUserWaveViewHandlerImpl.this.indexStore.indexWavelet(notification
                            .getValue());
                        return null;
                      }
                    });
                MongoDbPerUserWaveViewHandlerImpl.this.executor.execute(task);
              }
            }).<WaveletName, ReadableWaveletData> build();

  }

  @Override
  public ListenableFuture<Void> onParticipantAdded(WaveletName waveletName,
      ParticipantId participant) {

    // No op.
    SettableFuture<Void> task = SettableFuture.create();
    task.set(null);
    return task;
  }

  @Override
  public ListenableFuture<Void> onParticipantRemoved(WaveletName waveletName,
      ParticipantId participant) {

    // No op.
    SettableFuture<Void> task = SettableFuture.create();
    task.set(null);
    return task;
  }

  @Override
  public ListenableFuture<Void> onWaveUpdated(final ReadableWaveletData waveletData) {

    Preconditions.checkNotNull(waveletData);

    ListenableFutureTask<Void> task;
    WaveletName wname = WaveletName.of(waveletData.getWaveId(), waveletData.getWaveletId());


    // if it is a new wavelet, process it right now
    // or it is a user data wavelet, process it right now
    if (waveletData.getVersion() < 140 ||
        IdUtil.isUserDataWavelet(waveletData.getWaveletId())) {

      LOG.info("Wavelet  " + wname + " for indexing inmediately");

      task = ListenableFutureTask.<Void> create(new Callable<Void>() {

        @Override
        public Void call() throws Exception {
          indexStore.indexWavelet(waveletData);
          return null;
        }
      });
      executor.execute(task);
      return task;

    } else if (IdUtil.isConversationRootWaveletId(waveletData.getWaveletId())) {
      // Changes in conversation wavelets are not processed directly
      // to avoid fire a index process for each delta change. We wait until the
      // wavelet hasn't changed for a time.
      LOG.info("Wavelet  " + wname + " for delayed indexing");

      // For development
      task = ListenableFutureTask.<Void> create(new Callable<Void>() {

        @Override
        public Void call() throws Exception {
          indexStore.indexWavelet(waveletData);
          return null;
        }
      });

      /*
       * waveletDelayCache.put(WaveletName.of(waveletData.getWaveId(),
       * waveletData.getWaveletId()), waveletData);
       */
    }

    SettableFuture<Void> noOpTask = SettableFuture.create();
    noOpTask.set(null);
    return noOpTask;
  }

  @Override
  public Multimap<WaveId, WaveletId> retrievePerUserWaveView(ParticipantId user) {

    return null;
  }

  @Override
  public ListenableFuture<Void> onWaveInit(WaveletName waveletName) {
    // No op.
    SettableFuture<Void> task = SettableFuture.create();
    task.set(null);
    return task;
  }




}
