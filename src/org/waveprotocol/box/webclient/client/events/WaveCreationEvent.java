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
package org.waveprotocol.box.webclient.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class WaveCreationEvent extends GwtEvent<WaveCreationEventHandler> {
  public static final Type<WaveCreationEventHandler> TYPE = new Type<WaveCreationEventHandler>();

  public static final WaveCreationEvent CREATE_NEW_WAVE = new WaveCreationEvent();

  private WaveCreationEvent() {
  }

  @Override
  protected void dispatch(WaveCreationEventHandler handler) {
    handler.onCreateRequest(this);
  }

  @Override
  public com.google.gwt.event.shared.GwtEvent.Type<WaveCreationEventHandler> getAssociatedType() {
    return TYPE;
  }
}
