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


package org.swellrt.android.service.scheduler;

import java.util.Timer;
import java.util.TimerTask;



/**
 * Android/Java SimpleTimer implementation
 *
 * Based on original version (GWT-based) from
 * Apache Wave danilatos@google.com (Daniel Danilatos)
 *
 * @author pablojan@gmail.com
 */
public class AndroidSimpleTimer implements SimpleTimer {

  /**
   * Factory for creating simple-timer
   *
   * TODO(danilatos): Disallow creation of more than one timer, assuming its use
   * will be in the one and only scheduler?
   */
  public static final SimpleTimer.Factory FACTORY = new SimpleTimer.Factory() {
    public SimpleTimer create(TimerTask task) {
      return new AndroidSimpleTimer(task);
    }
  };


  private final Timer timer = new Timer();

  /** The task to schedule */
  private final TimerTask task;

  private AndroidSimpleTimer(TimerTask task) {
    this.task = task;
  }

  /** {@inheritDoc} */
  public double getTime() {
    return System.currentTimeMillis();
  }

  /** {@inheritDoc} */
  public void schedule() {
    timer.schedule(task, 0);
  }

  /** {@inheritDoc} */
  public void cancel() {
    timer.cancel();
  }

  /** {@inheritDoc} */
  public void schedule(double when) {
    int interval = (int) (when - getTime());
    timer.schedule(task, interval >= 1 ? interval : 1);
  }

}
