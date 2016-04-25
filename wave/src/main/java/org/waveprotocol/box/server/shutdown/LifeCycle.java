/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.waveprotocol.box.server.shutdown;

import org.waveprotocol.wave.model.util.Preconditions;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Life cycle control.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class LifeCycle {

  private static int SHUTDOWN_TIMEOUT_SEC = 2;

  private final String name;
  private final ShutdownPriority shutdownPriority;
  private final Shutdownable shutdownHandler;
  private final Semaphore semaphore = new Semaphore(Integer.MAX_VALUE);
  private final ShutdownManager shutdownManager;
  private boolean started;

  /**
   * Creates lifecycle.
   *
   * @param name the name of task.
   * @param shutdownPriority determines shutdown order.
   * @param shutdownHandler the handler executed on shutdown.
   */
  public LifeCycle(String name, ShutdownPriority shutdownPriority, Shutdownable shutdownHandler) {
    this(name, shutdownPriority, shutdownHandler, ShutdownManager.getInstance());
  }

  /**
   * Creates lifecycle.
   *
   * @param name the name of task.
   * @param shutdownPriority determines shutdown order.
   * @param shutdownHandler the handler executed on shutdown.
   * @param shutdownManager the shutdown manager.
   */
  public LifeCycle(String name, ShutdownPriority shutdownPriority, Shutdownable shutdownHandler,
                   ShutdownManager shutdownManager) {
    this.name = name;
    this.shutdownPriority = shutdownPriority;
    this.shutdownHandler = shutdownHandler;
    this.shutdownManager = shutdownManager;
  }

  /**
   * Starts lifecycle.
   */
  public synchronized void start() {
    Preconditions.checkArgument(!started, name + " is already started.");
    started = true;
    shutdownManager.register(new Shutdownable() {

      @Override
      public void shutdown() throws Exception {
        synchronized (LifeCycle.this) {
          if (shutdownHandler != null) {
            shutdownHandler.shutdown();
          }
          if (!semaphore.tryAcquire(Integer.MAX_VALUE, SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
            throw new TimeoutException();
          }
          started = false;
        }
      }
    }, name, shutdownPriority);
  }

  /**
   * Enters to execution block of task.
   */
  public synchronized void enter() {
    checkIsStarted();
    try {
      semaphore.acquire();
    } catch (InterruptedException ex) {
      throw new RuntimeException(ex);
    }
  }

  /**
   * Leaves execution block of task.
   */
  public synchronized void leave() {
    semaphore.release();
  }

  private void checkIsStarted() {
    if (!started) {
      throw new IllegalStateException(name + " is not started");
    }
  }
}
