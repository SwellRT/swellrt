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

package org.waveprotocol.wave.migration.helpers;

import org.waveprotocol.wave.model.document.operation.Attributes;
import org.waveprotocol.wave.model.document.operation.DocOpCursor;
import org.waveprotocol.wave.model.document.operation.DocOpCursorDecorator;

/**
 * Strips the "w:" prefix from elements.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class StripWColonFilter extends DocOpCursorDecorator {

  public StripWColonFilter(DocOpCursor target) {
    super(target);
  }

  @Override
  public void elementStart(String type, Attributes attrs) {
    super.elementStart(strip(type), attrs);
  }

  @Override
  public void deleteElementStart(String type, Attributes attrs) {
    super.deleteElementStart(strip(type), attrs);
  }

  private String strip(String type) {
    return type.startsWith("w:") ? type.substring(2) : type;
  }
}
