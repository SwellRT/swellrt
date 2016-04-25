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
package org.waveprotocol.box.server.shutdown;

import org.waveprotocol.wave.util.logging.Log;

import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Registers and executed by specified priority shutdown tasks.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
public class ShutdownManager extends Thread {

  interface NamedShutdownable extends Shutdownable {
    String getName();
  }

  private static final Log LOG = Log.get(ShutdownManager.class);
  private static ShutdownManager instance;

  private final SortedMap<ShutdownPriority, Set<NamedShutdownable>> tasks = new TreeMap<>();

  private ShutdownManager() {
    super(ShutdownManager.class.getSimpleName());
  }

  public static synchronized ShutdownManager getInstance() {
    if (instance == null) {
      instance = new ShutdownManager();
    }
    return instance;
  }

  /**
   * Requsters shutdown task.
   *
   * @param shutdownHandler the handler to execute on shutdown.
   * @param taskName the name of task.
   * @param priority the priority determines shutdown order.
   */
  public synchronized void register(final Shutdownable shutdownHandler, final String taskName,
                                    ShutdownPriority priority) {
    if (tasks.isEmpty()) {
      Runtime.getRuntime().addShutdownHook(this);
    }
    Set<NamedShutdownable> priorityTasks = tasks.get(priority);
    if (priorityTasks == null) {
      tasks.put(priority, priorityTasks = new HashSet<>());
    }
    priorityTasks.add(new NamedShutdownable() {

      @Override
      public String getName() {
        return taskName;
      }

      @Override
      public void shutdown() throws Exception {
        shutdownHandler.shutdown();
      }
    });
  }

  /**
   * Executes on Java shutdown hook.
   */
  @Override
  public void run() {
    LOG.info("Shutdown hook is fired.");
    shutdown();
  }

  private synchronized void shutdown() {
    LOG.info("Start of shutdown procedure.");
    for (ShutdownPriority priority : tasks.keySet()) {
      LOG.info("Shutdown priority class " + priority.name());
      for (NamedShutdownable task : tasks.get(priority)) {
        LOG.info("Shutdown of " + task.getName() + " ...");
        try {
          task.shutdown();
        } catch (Exception ex) {
          LOG.severe("Shutdown of " + task.getName() + " error", ex);
        }
      }
    }
    LOG.info("End of shutdown procedure.");
  }
}
