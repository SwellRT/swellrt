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
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;

import org.waveprotocol.wave.client.widget.common.ImplPanel;
import org.waveprotocol.wave.client.widget.popup.CenterPopupPositioner;
import org.waveprotocol.wave.client.widget.popup.PopupChrome;
import org.waveprotocol.wave.client.widget.popup.PopupChromeFactory;
import org.waveprotocol.wave.client.widget.popup.PopupFactory;
import org.waveprotocol.wave.client.widget.popup.TitleBar;
import org.waveprotocol.wave.client.widget.popup.UniversalPopup;

/**
 * Selector for gadgets, allowing selection from a list and entering a custom
 * gadget URL.
 *
 * @author danilatos@google.com (Daniel Danilatos)
 */
public class GadgetSelectorWidget extends Composite {
  public interface Listener {
    void onSelect(String url);
  }

  interface Binder extends UiBinder<ImplPanel, GadgetSelectorWidget> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField ImplPanel self;
  @UiField InputElement gadgetUrl;
  @UiField Button useCustom;
  @UiField FlowPanel options;
  private Listener listener;

  public GadgetSelectorWidget() {
    initWidget(self = BINDER.createAndBindUi(this));
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  /**
   * Shows in a popup, and returns the popup.
   */
  public UniversalPopup showInPopup() {
    PopupChrome chrome = PopupChromeFactory.createPopupChrome();
    UniversalPopup popup = PopupFactory.createPopup(
        null, new CenterPopupPositioner(), chrome, true);

    TitleBar titleBar = popup.getTitleBar();
    titleBar.setTitleText("Select Gadget");
    popup.add(GadgetSelectorWidget.this);

    popup.show();
    gadgetUrl.focus();
    return popup;
  }

  public void clear() {
    options.clear();
  }

  public void addFeaturedOptions() {
    addOption(
        "http://wave-api.appspot.com/public/gadgets/areyouin/gadget.xml",
        "Yes/No/Maybe",
        "http://sharedspaces.googlelabs.com/gallery/image/78001/thumbnail",
        "Simple and versatile poll gadget",
        null);
    addOption(
        "http://google-wave-resources.googlecode.com/svn/trunk/samples/extensions/gadgets/mappy/map_v2.xml",
        "Map Gadget",
        "http://sharedspaces.googlelabs.com/gallery/image/79001/thumbnail",
        "Collaborate on a map of placemarks, paths, and shapes with other participants. " +
        "Great for planning events and trips.",
        null);
    addOption(
        "http://www.waffle.dk/waffle.xml",
        "Waffle",
        "http://sharedspaces.googlelabs.com/gallery/image/81001/thumbnail",
        "The easy way to plan an event. Just choose a few dates and all the participants can " +
        "vote on their preferred options.",
        "By Mikkel Staunsholm (www.waffle.dk)");
    addOption(
        "http://testorax.appspot.com/flash/SDColcrop.xml",
        "Colcrop",
        "http://sharedspaces.googlelabs.com/gallery/image/169001/thumbnail",
        "Very addictive game!!! Play against a friend or the computer. Cover as many cells as " +
        "you can.",
        "Alexis Vuillemin");
    // TODO: Add more
  }

  public void addOption(final String gadgetUrl,
      String title, String imageUrl, String description, String author) {
    GadgetInfoWidget option = new GadgetInfoWidget();
    option.setTitle(title);
    option.setImage(imageUrl);
    option.setDescription(description);
    option.setAuthor(author);
    option.setListener(new GadgetInfoWidget.Listener() {
      @Override public void onSelect() {
        select(gadgetUrl);
      }
    });
    options.add(option);
  }

  @UiHandler("useCustom")
  void onClickCustom(ClickEvent event) {
    select(gadgetUrl.getValue());
  }

  private void select(String url) {
    if (listener != null) {
      listener.onSelect(url);
    }
  }
}
