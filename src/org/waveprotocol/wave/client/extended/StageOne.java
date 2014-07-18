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


package org.waveprotocol.wave.client.extended;

import org.waveprotocol.wave.client.common.util.AsyncHolder;

/**
 * The first stage of Undercurrent code.
 * <p>
 * This exposes minimal features required for basic reading interactions.
 *
 * @see StageZero
 */
public interface StageOne {



  /**
   * Default implementation of the stage one configuration. Each component is
   * defined by a factory method, any of which may be overridden in order to
   * stub out some dependencies. Circular dependencies are not detected.
   *
   */
  public static class DefaultProvider extends AsyncHolder.Impl<StageOne> implements StageOne {


    public DefaultProvider(StageZero previous) {
      // Nothing in stage one depends on anything in stage zero currently, but
      // the dependency is wired up so that it is simple to add such
      // dependencies should they be necessary in the future.
    }

    @Override
    protected final void create(Accessor<StageOne> whenReady) {
      onStageInit();
      install();
      onStageLoaded();
      whenReady.use(this);
    }

    /** Notifies this provider that the stage is about to be loaded. */
    protected void onStageInit() {
    }

    /** Notifies this provider that the stage has been loaded. */
    protected void onStageLoaded() {
    }


    /**
     * Installs parts of stage one that have dependencies.
     * <p>
     * This method is only called once all asynchronously loaded components of
     * stage one are ready.
     * <p>
     * Subclasses may override this to change the set of installed features.
     */
    protected void install() {
      // Eagerly install some features.

    }
  }
}
