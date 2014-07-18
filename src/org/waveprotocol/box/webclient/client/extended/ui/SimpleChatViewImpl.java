package org.waveprotocol.box.webclient.client.extended.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SimpleChatViewImpl extends Composite implements SimpleChatView, HasWidgets {


  interface Binder extends UiBinder<HTMLPanel, SimpleChatViewImpl> {
  }

  private final static Binder uiBinder = GWT.create(Binder.class);


  // Listener (aka Presenter)
  private SimpleChatView.Listener listener;

  private HTMLPanel rootPanel;

  Map<ParticipantId, Element> participantElements;

  @UiField
  DivElement participants;

  @UiField
  TextBox newParticipantBox;

  @UiField
  Button newParticipantButton;

  @UiField
  DivElement chatzone;

  @UiField
  TextBox textBox;

  @UiField
  Button sendButton;


  //
  // Set up
  //

  public SimpleChatViewImpl() {
    rootPanel = uiBinder.createAndBindUi(this);
    initWidget(rootPanel);
    this.participantElements = new HashMap<ParticipantId, Element>();
  }

  //
  // View Behaviour
  //


  @Override
  public void setListener(SimpleChatView.Listener listener) {
    this.listener = listener;
  }

  @Override
  public void addChatLines(List<String> lines) {
    for (String line : lines)
      addChatLine(line);
  }

  @Override
  public void addChatLine(String line) {
    Element chatLine = DOM.createDiv();
    chatLine.setInnerText(line);
    this.chatzone.appendChild(chatLine);
  }

  @Override
  public void clearChatLines() {
    NodeList<Node> nodes = this.chatzone.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      this.chatzone.removeChild(nodes.getItem(i));
    }
  }


  @Override
  public void addParticipant(ParticipantId participantId) {
    Element participantElement = DOM.createSpan();
    participantElement.setInnerText(participantId.toString() + " ");
    this.participants.appendChild(participantElement);
    this.participantElements.put(participantId, participantElement);
  }

  @Override
  public void removeParticipant(ParticipantId participantId) {
    Element participantElement = this.participantElements.get(participantId);
    if (participantElement != null) participantElement.removeFromParent();
  }

  //
  // Ui Handlers
  //

  @UiHandler("sendButton")
  void handleSendButtonClick(ClickEvent e) {
    if (!this.textBox.getValue().isEmpty()) {
      this.listener.onNewMessage(this.textBox.getValue());
      this.textBox.setValue(""); // clear the input box
    }
  }

  @UiHandler("newParticipantButton")
  void handleNewParticipantButtonClick(ClickEvent e) {
    if (!this.newParticipantBox.getValue().isEmpty()) {
      this.listener.onAddParticipant(this.newParticipantBox.getValue());
      this.newParticipantBox.setValue("");
    }
  }


  //
  // HasWidgtes
  //

  @Override
  public void add(Widget w) {
    rootPanel.add(w);
  }


  @Override
  public void clear() {
    rootPanel.clear();

  }


  @Override
  public Iterator<Widget> iterator() {
    return rootPanel.iterator();
  }


  @Override
  public boolean remove(Widget w) {
    return rootPanel.remove(w);
  }



}
