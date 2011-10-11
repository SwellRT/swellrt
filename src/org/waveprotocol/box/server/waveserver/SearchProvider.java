/**
 * Copyright 2010 Google Inc.
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

import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.wave.data.WaveViewData;

import java.util.Collection;

/**
 * A provider of search results. SearchProviders can be queried, and reply with a set of
 * ReadableWaveletData objects which match the query.
 * 
 * @author josephg@gmail.com (Joseph Gentle)
 */
public interface SearchProvider {
  /**
   * Run a search query.
   * 
   * @param user the user executing the query
   * @param query the query string
   * @param startAt The offset in the results to return
   * @param numResults The number of results from startAt to return
   * @return the wavelets which match the specified query
   */
  Collection<WaveViewData> search(
      ParticipantId user, String query, int startAt, int numResults);
}
