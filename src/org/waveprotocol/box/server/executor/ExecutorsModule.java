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

package org.waveprotocol.box.server.executor;

import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.waveprotocol.box.server.CoreSettings;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.ClientServerExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.ContactExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.DeltaPersistExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.SolrExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.XmppExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.IndexExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.ListenerExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.LookupExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.RobotConnectionExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.RobotGatewayExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.StorageContinuationExecutor;
import org.waveprotocol.box.server.executor.ExecutorAnnotations.WaveletLoadExecutor;

/**
 * Module with executors.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class ExecutorsModule extends AbstractModule {

  @Inject
  public ExecutorsModule() {
  }

  @Override
  public void configure() {
    bind(RequestScopeExecutor.class);
    bind(ScheduledRequestScopeExecutor.class);
  }

  @Provides
  @Singleton
  @ClientServerExecutor
  protected Executor provideClientServerExecutor(Provider<RequestScopeExecutor> executorProvider) {
    return provideThreadPoolExecutor(executorProvider, -1, ClientServerExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @DeltaPersistExecutor
  protected Executor provideDeltaPersistExecutor(Provider<RequestScopeExecutor> executorProvider,
      @Named(CoreSettings.DELTA_PERSIST_EXECUTOR_THREAD_COUNT) int threadCount) {
    return provideThreadPoolExecutor(executorProvider, threadCount, DeltaPersistExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @IndexExecutor
  protected Executor provideIndexExecutor(Provider<RequestScopeExecutor> executorProvider) {
    return provideThreadPoolExecutor(executorProvider, 1, IndexExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @ListenerExecutor
  protected Executor provideListenerExecutor(Provider<RequestScopeExecutor> executorProvider,
      @Named(CoreSettings.LISTENER_EXECUTOR_THREAD_COUNT) int threadCount) {
    return provideThreadPoolExecutor(executorProvider, threadCount, ListenerExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @LookupExecutor
  protected Executor provideLookupExecutor(Provider<RequestScopeExecutor> executorProvider,
      @Named(CoreSettings.LOOKUP_EXECUTOR_THREAD_COUNT) int threadCount) {
    return provideThreadPoolExecutor(executorProvider, threadCount, LookupExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @StorageContinuationExecutor
  protected Executor provideStorageContinuationExecutor(Provider<RequestScopeExecutor> executorProvider,
      @Named(CoreSettings.STORAGE_CONTINUATION_EXECUTOR_THREAD_COUNT) int threadCount) {
    return provideThreadPoolExecutor(executorProvider, threadCount, StorageContinuationExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @WaveletLoadExecutor
  protected Executor provideWaveletLoadExecutor(Provider<RequestScopeExecutor> executorProvider,
      @Named(CoreSettings.WAVELET_LOAD_EXECUTOR_THREAD_COUNT) int threadCount) {
    return provideThreadPoolExecutor(executorProvider, threadCount, WaveletLoadExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @ContactExecutor
  protected ScheduledExecutorService provideContactExecutor(Provider<ScheduledRequestScopeExecutor> executorProvider) {
    return provideScheduledThreadPoolExecutor(executorProvider, 1, ContactExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @RobotConnectionExecutor
  protected ScheduledExecutorService provideRobotConnectionExecutor(Provider<ScheduledRequestScopeExecutor> executorProvider,
      @Named(CoreSettings.ROBOT_CONNECTION_THREAD_COUNT) int threadCount) {
    return provideScheduledThreadPoolExecutor(executorProvider, threadCount, RobotConnectionExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @RobotGatewayExecutor
  protected Executor provideRobotGatewayExecutor(Provider<RequestScopeExecutor> executorProvider,
      @Named(CoreSettings.ROBOT_GATEWAY_THREAD_COUNT) int threadCount) {
    return provideThreadPoolExecutor(executorProvider, threadCount, RobotGatewayExecutor.class.getSimpleName());
  }

  @Provides
  @Singleton
  @XmppExecutor
  protected ScheduledExecutorService provideXmppExecutor(Provider<ScheduledRequestScopeExecutor> executorProvider) {
    return provideScheduledThreadPoolExecutor(executorProvider, 1, XmppExecutor.class.getSimpleName());
  }
  
  @Provides
  @Singleton
  @SolrExecutor
  protected Executor provideSolrExecutor(Provider<RequestScopeExecutor> executorProvider,
      @Named(CoreSettings.SOLR_THREAD_COUNT) int threadCount) {
    return provideThreadPoolExecutor(executorProvider, threadCount, SolrExecutor.class.getSimpleName());
  }
  
  private Executor provideThreadPoolExecutor(Provider<RequestScopeExecutor> executorProvider,
      int threadCount, String name) {
    if (threadCount == 0) {
      return MoreExecutors.sameThreadExecutor();
    }
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name).build();
    ExecutorService executor;
    if (threadCount < 0) {
      executor = Executors.newCachedThreadPool(threadFactory);
    } else if (threadCount == 1) {
      executor = Executors.newSingleThreadExecutor(threadFactory);
    } else {
      executor = Executors.newFixedThreadPool(threadCount, threadFactory);
    }
    RequestScopeExecutor scopeExecutor = executorProvider.get();
    scopeExecutor.setExecutor(executor, name);
    return scopeExecutor;
  }

  private ScheduledExecutorService provideScheduledThreadPoolExecutor(
      Provider<ScheduledRequestScopeExecutor> executorProvider, int threadCount, String name) {
    ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(name).build();
    ScheduledExecutorService executor;
    if (threadCount == 1) {
      executor = Executors.newSingleThreadScheduledExecutor(threadFactory);
    } else {
      executor = Executors.newScheduledThreadPool(threadCount, threadFactory);
    }
    ScheduledRequestScopeExecutor scopeExecutor = executorProvider.get();
    scopeExecutor.setExecutor(executor, name);
    return scopeExecutor;
  }
}
