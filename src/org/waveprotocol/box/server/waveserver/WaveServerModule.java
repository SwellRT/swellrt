/**
 * Copyright 2009 Google Inc.
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
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.persistence.PersistenceException;
import org.waveprotocol.wave.crypto.CachedCertPathValidator;
import org.waveprotocol.wave.crypto.CertPathStore;
import org.waveprotocol.wave.crypto.DefaultCacheImpl;
import org.waveprotocol.wave.crypto.DefaultTimeSource;
import org.waveprotocol.wave.crypto.DefaultTrustRootsProvider;
import org.waveprotocol.wave.crypto.DisabledCertPathValidator;
import org.waveprotocol.wave.crypto.TimeSource;
import org.waveprotocol.wave.crypto.TrustRootsProvider;
import org.waveprotocol.wave.crypto.VerifiedCertChainCache;
import org.waveprotocol.wave.crypto.WaveCertPathValidator;
import org.waveprotocol.wave.crypto.WaveSignatureVerifier;
import org.waveprotocol.wave.model.id.IdURIEncoderDecoder;
import org.waveprotocol.wave.model.id.WaveletName;
import org.waveprotocol.wave.model.version.HashedVersionFactory;
import org.waveprotocol.wave.model.version.HashedVersionFactoryImpl;
import org.waveprotocol.wave.util.escapers.jvm.JavaUrlCodec;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Guice Module for the prototype Server.
 *
 */
public class WaveServerModule extends AbstractModule {
  // TODO(soren): move to global config file
  private static final int LISTENER_EXECUTOR_THREAD_COUNT = 2;
  private static final int WAVELET_LOAD_EXECUTOR_THREAD_COUNT = 2;
  private static final IdURIEncoderDecoder URI_CODEC =
      new IdURIEncoderDecoder(new JavaUrlCodec());
  private static final HashedVersionFactory HASH_FACTORY = new HashedVersionFactoryImpl(URI_CODEC);

  private final Executor waveletLoadExecutor =
      Executors.newFixedThreadPool(WAVELET_LOAD_EXECUTOR_THREAD_COUNT);
  private final boolean enableFederation;

  public WaveServerModule(boolean enableFederation) {
    this.enableFederation = enableFederation;
  }

  @Override
  protected void configure() {
    bind(TimeSource.class).to(DefaultTimeSource.class).in(Singleton.class);

    if (enableFederation) {
      bind(SignatureHandler.class)
      .toProvider(SigningSignatureHandler.SigningSignatureHandlerProvider.class);
    } else {
      bind(SignatureHandler.class)
      .toProvider(NonSigningSignatureHandler.NonSigningSignatureHandlerProvider.class);
    }

    try {
      bind(WaveSignatureVerifier.class).toConstructor(WaveSignatureVerifier.class.getConstructor(
          WaveCertPathValidator.class, CertPathStore.class));
      bind(VerifiedCertChainCache.class).to(DefaultCacheImpl.class).in(Singleton.class);
      bind(DefaultCacheImpl.class).toConstructor(
          DefaultCacheImpl.class.getConstructor(TimeSource.class));
    } catch (NoSuchMethodException e) {
      throw new IllegalStateException(e);
    }

    bind(WaveletNotificationDispatcher.class).in(Singleton.class);
    bind(WaveBus.class).to(WaveletNotificationDispatcher.class);
    bind(WaveletNotificationSubscriber.class).to(WaveletNotificationDispatcher.class);
    bind(TrustRootsProvider.class).to(DefaultTrustRootsProvider.class).in(Singleton.class);
    bind(CertificateManager.class).to(CertificateManagerImpl.class).in(Singleton.class);
    bind(DeltaAndSnapshotStore.class).to(DeltaStoreBasedSnapshotStore.class).in(Singleton.class);
    bind(WaveMap.class).in(Singleton.class);
    bind(SearchProvider.class).to(WaveMap.class).in(Singleton.class);
    bind(WaveletProvider.class).to(WaveServerImpl.class).asEagerSingleton();
    bind(HashedVersionFactory.class).toInstance(HASH_FACTORY);
    bind(Executor.class).annotatedWith(Names.named("listener_executor")).toInstance(
        Executors.newFixedThreadPool(LISTENER_EXECUTOR_THREAD_COUNT));
  }

  @Provides
  @SuppressWarnings("unused")
  private LocalWaveletContainer.Factory provideLocalWaveletContainerFactory(
      final DeltaStore deltaStore) {
    return new LocalWaveletContainer.Factory() {
      @Override
      public LocalWaveletContainer create(WaveletNotificationSubscriber notifiee,
          WaveletName waveletName, String waveDomain) {
        return new LocalWaveletContainerImpl(waveletName, notifiee, loadWaveletState(
            waveletLoadExecutor, deltaStore, waveletName), waveDomain);
      }
    };
  }

  @Provides
  @SuppressWarnings("unused")
  private RemoteWaveletContainer.Factory provideRemoteWaveletContainerFactory(
      final DeltaStore deltaStore) {
    return new RemoteWaveletContainer.Factory() {
      @Override
      public RemoteWaveletContainer create(WaveletNotificationSubscriber notifiee,
          WaveletName waveletName, String waveDomain) {
        return new RemoteWaveletContainerImpl(waveletName, notifiee,
            loadWaveletState(waveletLoadExecutor, deltaStore, waveletName));
      }
    };
  }

  @Provides
  @SuppressWarnings("unused")
  private WaveCertPathValidator provideWaveCertPathValidator(
      @Named(CoreSettings.WAVESERVER_DISABLE_SIGNER_VERIFICATION) boolean disableSignerVerification,
      TimeSource timeSource, VerifiedCertChainCache certCache,
      TrustRootsProvider trustRootsProvider) {
    if (disableSignerVerification) {
      return new DisabledCertPathValidator();
    } else {
      return new CachedCertPathValidator(certCache, timeSource, trustRootsProvider);
    }
  }

  /**
   * Returns a future whose result is the state of the wavelet after it has
   * been loaded from storage.
   * Any failure is reported as a {@link PersistenceException}.
   */
  @VisibleForTesting
  static ListenableFuture<DeltaStoreBasedWaveletState> loadWaveletState(
      Executor executor, final DeltaStore deltaStore, final WaveletName waveletName) {
    ListenableFutureTask<DeltaStoreBasedWaveletState> task =
        new ListenableFutureTask<DeltaStoreBasedWaveletState>(
            new Callable<DeltaStoreBasedWaveletState>() {
              @Override
              public DeltaStoreBasedWaveletState call() throws PersistenceException {
                // One executor per wave is inefficient; see comment in
                // DeltaStoreBasedWaveletState.
                Executor persistExecutor = Executors.newSingleThreadExecutor();
                return DeltaStoreBasedWaveletState.create(deltaStore.open(waveletName),
                    persistExecutor);
              }
            });
    executor.execute(task);
    return task;
  }
}
