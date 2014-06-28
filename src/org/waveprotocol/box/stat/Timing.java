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
package org.waveprotocol.box.stat;

import com.google.gwt.core.shared.GWT;
import java.util.Map;

/**
 * Request-scoped timing.
 *
 * @author akaplanov@gmail.com (A. Kaplanov)
 */
@SuppressWarnings({"rawtypes"})
public class Timing {
  static private final StatRecorder statsRecorder = new StatRecorder();
  static private final StatRenderer renderer = new StatRenderer();
  static private RequestScope scope = new SingleThreadedRequestScope();

  static private boolean enabled = true;

  /**
   * Gets recorder of statistic.
   */
  public static StatRecorder getStatsRecorder() {
    return statsRecorder;
  }

  /**
   * Enables/disables statistic.
   */
  static public void setEnabled(boolean enabled) {
    Timing.enabled = enabled;
  }

  /**
   * Gets enable state.
   */
  static public boolean isEnabled() {
    return enabled;
  }

  /**
   * Initializes scope.
   */
  static public void enterScope() {
    if (enabled) {
      scope.enter();
    }
  }

  /**
   * Initializes scope with specified data.
   */
  static public void enterScope(Map<Class, RequestScope.Value> scopeData) {
    if (enabled) {
      scope.enter(scopeData);
    }
  }

  /**
   * Clears scope.
   */
  static public void exitScope() {
    scope.exit();
  }

  /**
   * Sets scope.
   */
  static public void setScope(RequestScope scope) {
    Timing.scope = scope;
  }

  /**
   * Gets scope.
   */
  static public RequestScope getScope() {
    return scope;
  }

  /**
   * Creates and starts the timer with specified name.
   */
  static public Timer start(String name) {
    if (enabled) {
      return getExecutionTree().start(name);
    }
    return null;
  }

  /**
   * Creates and starts the timer with specified name and threshold.
   */
  static public Timer start(String name, int threshold) {
    if (enabled) {
      return getExecutionTree().start(name, threshold);
    }
    return null;
  }

  /**
   * Creates and starts the timer for request with specified name.
   */
  static public Timer startRequest(String name) {
    if (enabled) {
      return getExecutionTree().startRequest(name);
    }
    return null;
  }

  /**
   * Creates and starts the timer for request with specified name and threshold.
   */
  static public Timer startRequest(String name, int threshold) {
    if (enabled) {
      return getExecutionTree().startRequest(name, threshold);
    }
    return null;
  }

  /**
   * Stops the timer.
   */
  static public void stop(Timer timer) {
    if (timer != null && timer.isActive()) {
      timer.stop(getExecutionTree());
    }
  }

  /**
   * Stops the timer on specified time.
   */
  static public void stop(Timer timer, long stopTime) {
    if (timer != null) {
      timer.stop(getExecutionTree(), stopTime);
    }
  }

  /**
   * Records statistics for specified name and interval.
   */
  static public void record(String name, int interval) {
    if (enabled) {
      getExecutionTree().record(name, interval);
    }
  }

  /**
   * Records statistics for specified name, interval and threshold.
   */
  static public void record(String name, int interval, int threshold) {
    if (enabled) {
      getExecutionTree().record(name, interval, threshold);
    }
  }

  /**
   * Records statistics for specified request name and threshold.
   */
  static public void recordRequest(String name, int interval) {
    if (enabled) {
      getExecutionTree().recordRequest(name, interval);
    }
  }

  /**
   * Records statistics for specified request name, interval and threshold.
   */
  static public void recordRequest(String name, int interval, int threshold) {
    if (enabled) {
      getExecutionTree().recordRequest(name, interval, threshold);
    }
  }

  /**
   * Clears the statistics.
   */
  static public void clearStatistics() {
    if (GWT.isClient()) {
      statsRecorder.getGlobalStore().clear();
    } else {
      statsRecorder.getSessionStore().clear();
    }
  }

  /**
   * Renders statistics for all program.
   */
  static public String renderGlobalStatistics() {
    return renderer.renderHtml(
            statsRecorder.getGlobalStore().getMeasurements(),
            statsRecorder.getGlobalStore().getProfiledRequests());
  }

  /**
   * Renders statistics for current session. Server only.
   */
  static public String renderSessionStatistics() {
    return renderer.renderHtml(
            statsRecorder.getSessionStore().getMeasurements(),
            statsRecorder.getSessionStore().getProfiledRequests());
  }

  /**
   * Renders tracked statistic.
   */
  static public String renderStats() {
    return renderer.renderHtml(Statistic.getStats());
  }

  /**
   * Renders title.
   */
  static public String renderTitle(String title, int level) {
    return renderer.renderTitle(title, level);
  }

  /**
   * Gets execution tree.
   */
  static private ExecutionTree getExecutionTree() {
    return scope.get(ExecutionTree.class);
  }
}
