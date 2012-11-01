package org.waveprotocol.wave.client.wavepanel.impl.edit;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ImageElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.ui.Composite;

import org.waveprotocol.wave.client.widget.common.ImplPanel;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A widget for displaying participants
 *
 * @author wavegrove@gmail.com
 */
class ParticipantWidget extends Composite {
  interface Listener {
    void onMark(ParticipantId participant);
    void onUnMark(ParticipantId participant);
  }

  interface Binder extends UiBinder<ImplPanel, ParticipantWidget> {
  }

  private static final Binder BINDER = GWT.create(Binder.class);

  @UiField ImplPanel self;
  @UiField Element title;
  @UiField ImageElement image;
  private Listener listener;

  private boolean marked;
  ParticipantId participant;

  public ParticipantWidget() {
    initWidget(self = BINDER.createAndBindUi(this));
    mark();
  }

  public void setImage(String url) {
    image.setSrc(url);
  }

  /**
   * Sets the visible title.
   *
   * Semantics differ from UiObject method.
   */
  @Override
  public void setTitle(String text) {
    title.setInnerText(text);
  }

  public void setParticipant(ParticipantId participant) {
    this.participant = participant;
    setTitle(participant.toString());
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  @UiHandler("self")
  void onClick(ClickEvent e) {
    if (marked) {
      unMark();
    } else {
      mark();
    }
  }

  public void mark() {
    marked = true;
    self.getElement().getStyle().setBackgroundColor("#789e35");
    if (listener != null) {
      listener.onMark(participant);
    }
  }

  public void unMark() {
    marked = false;
    self.getElement().getStyle().setBackgroundColor("#fff");
    if (listener != null) {
      listener.onUnMark(participant);
    }
  }
}
