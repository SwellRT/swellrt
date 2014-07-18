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
 * The third stage of client code.
 * <p>
 * This stage includes editing capabilities.
 *
 */
public interface StageThree {

  StageTwo getStageTwo();


  /**
   * Default implementation of the stage three configuration. Each component is
   * defined by a factory method, any of which may be overridden in order to
   * stub out some dependencies. Circular dependencies are not detected.
   *
   */
  public class DefaultProvider extends AsyncHolder.Impl<StageThree> implements StageThree {
    // External dependencies
    protected final StageTwo stageTwo;

    //
    // Synchronously constructed dependencies.
    //
    public DefaultProvider(StageTwo stageTwo) {
      this.stageTwo = stageTwo;
    }

    /**
     * Creates the second stage.
     */
    @Override
    protected void create(final Accessor<StageThree> whenReady) {
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

    @Override
    public final StageTwo getStageTwo() {
      return stageTwo;
    }





    protected String getLocalDomain() {
      return null;
    }

    /**
     * Installs parts of stage three that have dependencies.
     * <p>
     * This method is only called once all asynchronously loaded components of
     * stage three are ready.
     * <p>
     * Subclasses may override this to change the set of installed features.
     */
    protected void install() {

    }
  }
}
