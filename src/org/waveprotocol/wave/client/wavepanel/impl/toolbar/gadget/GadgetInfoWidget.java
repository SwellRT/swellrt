/**
 * Copyright 2011 Google Inc.
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

package org.waveprotocol.wave.client.wavepanel.impl.toolbar.gadget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;

import org.waveprotocol.wave.client.widget.common.ImplPanel;

/**
 * A widget for displaying summary info about a gadget.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
class GadgetInfoWidget extends Composite {
  interface Listener {
    void onSelect();
  }

  interface Binder extends UiBinder<ImplPanel, GadgetInfoWidget> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField ImplPanel self;
  @UiField ImageElement image;
  @UiField Element title;
  @UiField Element description;
  @UiField Element author;
  private Listener listener;

  public GadgetInfoWidget() {
    initWidget(self = BINDER.createAndBindUi(this));
  }

  public void setImage(String url) {
    image.setSrc(url);
  }

  /**
   * Sets the visible title
   *
   * Semantics differ from UiObject method.
   */
  @Override
  public void setTitle(String text) {
    title.setInnerText(text);
  }

  public void setDescription(String text) {
    description.setInnerText(text);
  }

  public void setAuthor(String name) {
    if (name == null || name.isEmpty()) {
      author.setInnerText("");
    } else {
      author.setInnerText("By " + name);
    }
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @UiHandler("self")
  void onClick(ClickEvent e) {
    if (listener != null) {
      listener.onSelect();
    }
  }
}
