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

package org.waveprotocol.wave.client.doodad.form.button;

import org.waveprotocol.wave.client.doodad.form.events.ContentEvents;
import org.waveprotocol.wave.client.editor.ElementHandlerRegistry;
import org.waveprotocol.wave.client.editor.NodeEventHandler;
import org.waveprotocol.wave.client.editor.NodeEventHandlerImpl;
import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.client.editor.content.ContentNode;
import org.waveprotocol.wave.client.editor.content.NodeEventRouter;
import org.waveprotocol.wave.client.editor.content.misc.Caption;
import org.waveprotocol.wave.client.editor.event.EditorEvent;
import org.waveprotocol.wave.client.widget.button.ClickButton;
import org.waveprotocol.wave.model.document.util.Property;
import org.waveprotocol.wave.model.document.util.XmlStringBuilder;

public final class Button {
  private static final String TAGNAME = "button";

  static final Property<ClickButton> BUTTON_LOGIC_PROP = Property.immutable("button_logic");

  private static final ButtonRenderingMutationHandler RENDERING_MUTATION_HANDLER =
      new ButtonRenderingMutationHandler();
  private static final NodeEventHandler NODE_EVENT_HANDLER = new NodeEventHandlerImpl() {
    // TODO(patcoleman): Consider overriding handleBackspaceAfterNode so that buttons are harder to
    // delete.

    @Override
    public boolean handleClick(ContentElement element, EditorEvent event) {
      ContentNode last = element.getLastChild();
      ContentNode events = (last != null && ContentEvents.isContentEvents(last)) ?
          last : null;

      if (events != null && NodeEventRouter.INSTANCE.handleClick(events, event)) {
        return true;
      } else {
        return false;
      }
    }
  };


  /**
   * Registers subclass with ContentElement
   */
  public static void register(
      final ElementHandlerRegistry registry) {
    registry.registerEventHandler(TAGNAME, NODE_EVENT_HANDLER);
    registry.registerRenderingMutationHandler(TAGNAME, RENDERING_MUTATION_HANDLER);
  }

  /**
   * @param caption
   * @param name
   * @return A content xml string containing n button
   */
  public static XmlStringBuilder constructXml(
      XmlStringBuilder caption, String name) {
    return Caption.constructXml(caption)
        .append(ContentEvents.constructXml())
        .wrap(TAGNAME, ContentElement.NAME, name);
  }

  /** Utility class */
  private Button() {
  }
}
