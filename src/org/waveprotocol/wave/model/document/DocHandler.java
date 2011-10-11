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

package org.waveprotocol.wave.model.document;

import org.waveprotocol.wave.model.document.indexed.DocumentHandler;

/**
 * A DocumentHandler specialized for the non-generic Document interface.
 *
 */
public interface DocHandler extends DocumentHandler<Doc.N, Doc.E, Doc.T> {

  /** Convenience interface for referring to a non-generic EventBundle. */
  public interface DocEventBundle extends DocumentHandler.EventBundle<Doc.N, Doc.E, Doc.T> { }
}
