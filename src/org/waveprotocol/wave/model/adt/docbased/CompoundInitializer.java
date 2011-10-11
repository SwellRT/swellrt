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
 */

package org.waveprotocol.wave.model.adt.docbased;

import java.util.Map;

/**
 * An initializer which composes a number of component initializers.
 *
 * @author anorth@google.com (Alex North)
 */
public final class CompoundInitializer implements Initializer {
  private final Initializer[] components;

  public CompoundInitializer(Initializer... components) {
    this.components = components;
  }

  @Override
  public void initialize(Map<String, String> target) {
    for (Initializer initializer : components) {
      initializer.initialize(target);
    }
  }
}
