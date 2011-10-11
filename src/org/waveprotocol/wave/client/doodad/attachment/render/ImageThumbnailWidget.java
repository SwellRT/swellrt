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

package org.waveprotocol.wave.client.doodad.attachment.render;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.dom.client.StyleInjector;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ErrorEvent;
import com.google.gwt.event.dom.client.ErrorHandler;
import com.google.gwt.event.dom.client.LoadEvent;
import com.google.gwt.event.dom.client.LoadHandler;
import com.google.gwt.event.dom.client.MouseOutEvent;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverEvent;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.CssResource.NotStrict;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.google.gwt.resources.client.ImageResource.RepeatStyle;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.client.common.util.DomHelper;
import org.waveprotocol.wave.client.common.util.UserAgent;
import org.waveprotocol.wave.client.common.webdriver.DebugClassHelper;
import org.waveprotocol.wave.client.scheduler.ScheduleCommand;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.widget.button.ButtonFactory;
import org.waveprotocol.wave.client.widget.button.ToggleButton.ToggleButtonListener;
import org.waveprotocol.wave.client.widget.button.ToggleButtonWidget;
import org.waveprotocol.wave.client.widget.button.icon.IconButtonTemplate.IconButtonStyle;
import org.waveprotocol.wave.client.widget.progress.ProgressWidget;

/**
 * Widget that implements a thumbnail structure.
 * Package-private as used only by ImageThumbnail.
 *
 */
// TODO(user): replace with no widgets, only lightweight elements
class ImageThumbnailWidget extends Composite implements ImageThumbnailView {

  /** ClientBundle */
  interface Resources extends ClientBundle {
    /** Css */
    interface Css extends CssResource {
      /** Class for the thumbnail as a whole */
      String imageThumbnail();
      /** Class for the actual image */
      String image();
      /** Class applied to the progress widget */
      String progress();
      /** Class for the progress button */
      String thumbSizeButton();
    }

    /** Css resource */
    @Source("Thumbnail.css")
    @NotStrict  // TODO(user): make Strict by including all classes in the CssResource
    public Css css();

    /** Thumbnail images */
    @Source("thumb-n-2.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource chromeNorth();

    @Source("thumb-ne-2.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeNorthEast();

    @Source("thumb-e-2.png")
    @ImageOptions(repeatStyle = RepeatStyle.Vertical, flipRtl = true)
    ImageResource chromeEast();

    @Source("thumb-se-2.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeSouthEast();

    @Source("thumb-s-2.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource chromeSouth();

    @Source("thumb-sw-2.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeSouthWest();

    @Source("thumb-w-2.png")
    @ImageOptions(repeatStyle = RepeatStyle.Vertical, flipRtl = true)
    ImageResource chromeWest();

    @Source("thumb-nw-2.png")
    @ImageOptions(flipRtl = true)
    ImageResource chromeNorthWest();

    @Source("thumb-c-2.png")
    @ImageOptions(repeatStyle = RepeatStyle.Horizontal)
    ImageResource chromeCenter();

    @Source("error.png")
    @ImageOptions(flipRtl = true)
    ImageResource errorAttachment();

    /** loading images are animated GIF images.
     *  DataResource is used instead of ImageResource to prevent conversion to PNG.
     */
    @Source("slow_loading.gif")
    @ImageOptions(flipRtl = true)
    DataResource chromeLoadingSlow();

    @Source("slow_loading_fast.gif")
    @ImageOptions(flipRtl = true)
    DataResource chromeLoadingFast();

    @Source("att_loading.gif")
    @ImageOptions(flipRtl = true)
    DataResource chromeLoadingAttachment();
  }

  /** UiBinder */
  interface Binder extends UiBinder<HTMLPanel, ImageThumbnailWidget> {}
  private static final Binder BINDER = GWT.create(Binder.class);

  /**
   * Singleton instance of resource bundle
   */
  static final Resources.Css css = GWT.<Resources>create(Resources.class).css();
  static {
    StyleInjector.inject(css.getText());
  }

  /**
   * Specifies whether or not to set the width of the image container element.
   */
  private static final boolean DO_FRAME_WIDTH_UPDATE = UserAgent.isIE();

  public ImageThumbnailWidget() {
    initWidget(BINDER.createAndBindUi(this));

    // Restore the desired attributes of widget elements in the template (GWT rips them out)

    Element element = getElement();
    addStyleName(css.imageThumbnail());
    // Make the thumbnail as a whole uneditable and unselectable.
    element.setAttribute("contentEditable", "false");
    DomHelper.makeUnselectable(element);

    errorLabel.setVisible(false);
    spin.setVisible(true);

    Element imageElement = image.getElement();
    imageElement.addClassName(css.image());
    imageElement.getStyle().setVisibility(Visibility.HIDDEN);

    button = ButtonFactory.createIconToggleButton(
        IconButtonStyle.PLUS_MINUS, "Options", new ToggleButtonListener() {
          public void onOff() {
            Event.getCurrentEvent().stopPropagation();
            Event.getCurrentEvent().preventDefault();
            if (listener != null) {
              listener.onRequestSetFullSizeMode(false);
            }
          }

          public void onOn() {
            Event.getCurrentEvent().stopPropagation();
            Event.getCurrentEvent().preventDefault();
            if (listener != null) {
              listener.onRequestSetFullSizeMode(true);
            }
          }
        });
    button.addStyleName(css.thumbSizeButton());
    DebugClassHelper.addDebugClass(button.getElement(), "image_toggle");

    menuButtonContainer.add(button);
    final Element buttonContainerElement = menuButtonContainer.getElement();

    buttonContainerElement.getStyle().setDisplay(Display.NONE);

    addDomHandler(new MouseOverHandler() {
      public void onMouseOver(MouseOverEvent event) {
        resizeButtonVisible = true;
        updateResizeButton();
      }
    }, MouseOverEvent.getType());

    addDomHandler(new MouseOutHandler() {
      public void onMouseOut(MouseOutEvent event) {
        resizeButtonVisible = false;
        updateResizeButton();
      }
    }, MouseOutEvent.getType());
  }

  @UiHandler("image")
  void onImageClicked(ClickEvent ignored) {
    handleImageRegionClicked();
  }

  @UiHandler("spin")
  void onSpinClicked(ClickEvent ignored) {
    handleImageRegionClicked();
  }

  @UiHandler("errorLabel")
  void onErrorClicked(ClickEvent ignored) {
    handleImageRegionClicked();
  }

  private void handleImageRegionClicked() {
    if (listener != null) {
      listener.onClickImage();
    }
  }

  /**
   * The double buffer loaded used to load the thumbnail.
   */
  private DoubleBufferImage doubleBufferLoader;

  /**
   * Image widget representing the thumbnail image.
   */
  @UiField Image image;

  /**
   * The container to which we will add our drop down menu button
   */
  @UiField SimplePanel menuButtonContainer;

  /**
   * Caption panel to put the caption.
   */
  @UiField SimplePanel captionPanel;

  /**
   * The Label that contains the spinning wheel
   */
  @UiField Label spin;

  /**
   * The Label that contains the error image
   */
  @UiField Label errorLabel;

  /**
   * The fancy chrome around the thumbnail
   */
  @UiField HTMLPanel chromeContainer;

  /**
   * The progress widget.
   */
  @UiField ProgressWidget progressWidget;

  private String attachmentUrl;

  private String thumbnailUrl;

  private ImageThumbnailViewListener listener;

  private final ToggleButtonWidget button;

  private boolean resizeButtonVisible = false;

  // logical state

  // TODO(danilatos): Move this state out of the display. Ideally displays should
  // be stateless.
  private boolean isFullSize = false;

  private int thumbnailWidth, thumbnailHeight;

  private int attachmentWidth, attachmentHeight;

  @Override
  public void displayDeadImage(String toolTip) {
    this.hideUploadProgress();
    this.spin.setVisible(false);
    this.errorLabel.setTitle(toolTip);
    this.errorLabel.setVisible(true);
  }

  private final Scheduler.Task clearButtonTask = new Scheduler.Task() {
    public void execute() {
      Style style = menuButtonContainer.getElement().getStyle();
      if (resizeButtonVisible) {
        style.setDisplay(Display.BLOCK);
      } else {
        style.setDisplay(Display.NONE);
      }
    }
  };

  private void updateResizeButton() {
    ScheduleCommand.addCommand(clearButtonTask);
  }

  @Override
  public void setListener(ImageThumbnailViewListener listener) {
    this.listener = listener;
  }

  @Override
  public void setThumbnailSize(int width, int height) {
    this.thumbnailWidth = width;
    this.thumbnailHeight = height;

    if (isFullSize) {
      return;
    }

    image.setPixelSize(width, height);
    //TODO(user,danilatos): Whinge about how declarative UI doesn't let us avoid this hack:
    Style pstyle = image.getElement().getParentElement().getParentElement().getStyle();
    if (width == 0) {
      image.setWidth("");
      pstyle.clearWidth();
    } else {
      pstyle.setWidth(width, Unit.PX);
    }
    if (height == 0) {
      image.setHeight("");
      pstyle.clearHeight();
    } else {
      pstyle.setHeight(height, Unit.PX);
    }

    // NOTE(user): IE requires that the imageCaptionContainer element has a width
    //   in order to correctly center the caption.
    if (DO_FRAME_WIDTH_UPDATE) {
      captionPanel.getElement().getStyle().setWidth(width, Unit.PX);
    }
  }

  @Override
  public void setAttachmentSize(int width, int height) {
    this.attachmentWidth = width;
    this.attachmentHeight = height;
  }

  public void setFullSizeMode(boolean isOn) {
    isFullSize = isOn;
    button.setOn(isOn);
    Style pstyle = image.getElement().getParentElement().getParentElement().getStyle();
    if (isOn) {
      chromeContainer.getElement().getStyle().setDisplay(Display.NONE);

      image.setPixelSize(attachmentWidth, attachmentHeight);
      if (attachmentWidth == 0) {
        image.setWidth("");
        pstyle.clearWidth();
      } else {
        pstyle.setWidth(attachmentWidth, Unit.PX);
      }
      if (attachmentHeight == 0) {
        image.setHeight("");
        pstyle.clearHeight();
      } else {
        pstyle.setHeight(attachmentHeight, Unit.PX);
      }

      setAttachmentUrl(attachmentUrl);
    } else {
      chromeContainer.getElement().getStyle().clearDisplay();
      setThumbnailSize(thumbnailWidth, thumbnailHeight);
      setThumbnailUrl(thumbnailUrl);
    }
  }

  private static class DoubleBufferImage {

    /**
     * An image object used to preload the thumbnail before we display it
     */
    private final Image doubleLoadedImage = new Image();
    private final Image imageToLoad;

    private final Widget spinner;
    private final Widget error;

    /**
     * Create a double buffer loader for a given image widget
     *
     * @param spinner
     * @param imageToLoad
     */
    public DoubleBufferImage(Widget spinner, Widget error, Image imageToLoad) {
      if (UserAgent.isIE()) {
        DomHelper.makeUnselectable(doubleLoadedImage.getElement());
      }
      this.spinner = spinner;
      this.error = error;
      this.imageToLoad = imageToLoad;
    }

    /**
     * Registration of load handler to monitor the doubleLoadedImage to see if
     * it has finished.
     */
    private HandlerRegistration onLoadHandlerRegistration;

    /**
     * Registration of error handler to monitor the doubleLoadedImage to see if
     * it has finished.
     */
    private HandlerRegistration onErrorHandlerRegistration;

    /**
     * Handler for load + error events used by the double buffered image.
     */
    private class DoubleLoadHandler implements LoadHandler, ErrorHandler {
      private final String url;
      private boolean completed = false;

      /***/
      public DoubleLoadHandler(String url) {
        this.url = url;
      }

      /** {@inheritDoc}) */
      public void onError(ErrorEvent e) {
        if (completed) {
          return;
        }
        cleanUp();
        spinner.setVisible(false);
        error.setVisible(true);
      }

      /** {@inheritDoc}) */
      public void onLoad(LoadEvent e) {
        if (completed) {
          return;
        }
        cleanUp();
        spinner.setVisible(false);
        imageToLoad.getElement().getStyle().clearVisibility();
        imageToLoad.setUrl(url);
      }

      private void cleanUp() {
        RootPanel.get().remove(doubleLoadedImage);

        final HandlerRegistration onLoadReg = onLoadHandlerRegistration;
        final HandlerRegistration onErrorReg = onErrorHandlerRegistration;

        // HACK(user): There is a bug in GWT which stops us from removing a listener in HOSTED
        // mode inside the invoke context.  Put the remove in a deferred command to avoid this
        // error
        ScheduleCommand.addCommand(new Scheduler.Task() {
          public void execute() {
            onLoadReg.removeHandler();
            onErrorReg.removeHandler();
          }
        });

        onLoadHandlerRegistration = null;
        onErrorHandlerRegistration = null;

        completed = true;
      }
    }

    /**
     * Get the thumbnail to load its image from the given url.
     * @param url
     */
    public void loadImage(final String url) {
      // Remove the old double loader to stop the last double buffered load.
      if (onLoadHandlerRegistration != null) {
        onLoadHandlerRegistration.removeHandler();
        onErrorHandlerRegistration.removeHandler();

        // We used to set doubleLoadedImage's url to "" here.
        // It turns out to be a really bad thing to do.  Setting an url to null
        // cause Wfe's bootstrap servelet to get called, which overload the server.
        RootPanel.get().remove(doubleLoadedImage);
      }

      // set up the handler to hide spinning wheel when loading has finished
      // We need to have the doubleLoadedImage created even if we are loading the image directly
      // in imageToLoad.  This is done because we don't get a event otherwise.
      DoubleLoadHandler doubleLoadHandler = new DoubleLoadHandler(url);
      onLoadHandlerRegistration = doubleLoadedImage.addLoadHandler(doubleLoadHandler);
      onErrorHandlerRegistration = doubleLoadedImage.addErrorHandler(doubleLoadHandler);

      error.setVisible(false);
      doubleLoadedImage.setVisible(false);
      doubleLoadedImage.setUrl(url);
      RootPanel.get().add(doubleLoadedImage);

      imageToLoad.getElement().getStyle().setVisibility(Visibility.HIDDEN);

      // If image is empty, show the url directly.
      if (imageToLoad.getUrl().length() == 0) {
        imageToLoad.setUrl(url);
      }
    }
  }

  /**
   * Sets the {@code src} attribute of the image element, and performs any other browser-specific
   * actions to make the image display nicely.
   *
   * {@inheritDoc}
   */
  public void setThumbnailUrl(String url) {
    // TODO(patcoleman): concurrency issue here, in the sense that there is nothing enforcing
    // setThumbnailUrl and setAttachmentUrl to be called in the same event loop. That currently
    // is happening, though should be enforced or this code should be changed, otherwise resizing
    // between these two calls will result in no images being loaded :(
    if (url.equals(thumbnailUrl)) {
      return;
    }

    this.thumbnailUrl = url;

    if (!isFullSize) { // load image into buffer if it is to be displayed
      if (doubleBufferLoader == null) {
        doubleBufferLoader = new DoubleBufferImage(spin, errorLabel, image);
      }
      doubleBufferLoader.loadImage(url);
      DOM.setStyleAttribute(image.getElement(), "visibility", "");
    } else {
      Image.prefetch(url);
    }
  }

  /**
   * Set link to follow when the user click on the thumbnail
   *
   * {@inheritDoc}
   */
  public void setAttachmentUrl(String url) {
    this.attachmentUrl = url;

    if (isFullSize) { // load image into buffer if it is to be displayed
      if (doubleBufferLoader == null) {
        doubleBufferLoader = new DoubleBufferImage(spin, errorLabel, image);
      }
      doubleBufferLoader.loadImage(url);
      DOM.setStyleAttribute(image.getElement(), "visibility", "");
    }
  }

  /** {@inheritDoc} */
  @Override
  public void showUploadProgress() {
    progressWidget.setVisible(true);
  }

  /** {@inheritDoc} */
  @Override
  public void hideUploadProgress() {
    progressWidget.setVisible(false);
  }

  /** {@inheritDoc} */
  @Override
  public void setUploadProgress(double progress) {
    progressWidget.setValue(progress);
  }

  /**
   * @return element the caption will be appended to
   */
  public Element getCaptionContainer() {
    return captionPanel.getElement();
  }
}
