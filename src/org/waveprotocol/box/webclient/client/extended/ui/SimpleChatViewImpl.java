package org.waveprotocol.box.webclient.client.extended.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.waveprotocol.wave.model.extended.type.chat.ChatMessage;
import org.waveprotocol.wave.model.extended.type.chat.ChatPresenceStatus;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class SimpleChatViewImpl extends Composite implements SimpleChatView, HasWidgets {


  interface Binder extends UiBinder<HTMLPanel, SimpleChatViewImpl> {
  }

  private final static Binder uiBinder = GWT.create(Binder.class);


  // Listener (aka Presenter)
  private SimpleChatView.Listener listener;

  private HTMLPanel rootPanel;

  Map<ParticipantId, Element> participantElements;

  Map<ParticipantId, ChatPresenceStatus> participantStatus;

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

  private Timer refreshTimer;

  private Long lastKeyPressTime = 0L;

  //
  // Set up
  //

  public SimpleChatViewImpl() {
    rootPanel = uiBinder.createAndBindUi(this);
    initWidget(rootPanel);
    this.participantElements = new HashMap<ParticipantId, Element>();
    this.participantStatus = new HashMap<ParticipantId, ChatPresenceStatus>();

    refreshTimer = new Timer() {
      @Override
      public void run() {
        refreshParticipantStatus();
      }
    };


    this.textBox.addKeyPressHandler(new KeyPressHandler() {

      @Override
      public void onKeyPress(KeyPressEvent event) {

        Long now = System.currentTimeMillis();
        if (now - lastKeyPressTime > 2000) listener.startWriting();

        lastKeyPressTime = System.currentTimeMillis();

      }
    });
  }

  //
  // View Behaviour
  //


  @Override
  public void setListener(SimpleChatView.Listener listener) {
    this.listener = listener;
    this.refreshTimer.scheduleRepeating(3000);
  }

  @Override
  public void unsetListener() {
    this.listener = null;
    this.refreshTimer.cancel();
  }

  @Override
  public void addChatLines(List<ChatMessage> lines) {
    for (ChatMessage line : lines)
      addChatLine(line);
  }

  @Override
  public void addChatLine(ChatMessage line) {
    Element chatLine = DOM.createDiv();
    Date d = new Date(line.getTimestamp());
    chatLine.setInnerText("[" + d.toString() + "] " + line.getText());
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

  public void setParticipants(Set<ParticipantId> participants) {
    this.participants.removeAllChildren();
    this.participantElements.clear();

    for (ParticipantId p : participants)
      addParticipant(p);
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

  @Override
  public void setParticipantStatus(ParticipantId participant, ChatPresenceStatus status) {
    this.participantStatus.put(participant, status);
    displayParticipantStatus(participant);
  }

  protected void refreshParticipantStatus() {

    for (Entry<ParticipantId, ChatPresenceStatus> entry : participantStatus.entrySet()) {
      displayParticipantStatus(entry.getKey());
    }

    Long now = System.currentTimeMillis();
    if (now - lastKeyPressTime > 2000) {
      this.listener.stopWriting();
    }


  }


  protected void displayParticipantStatus(ParticipantId participant) {
    Element e = this.participantElements.get(participant);
    ChatPresenceStatus status = this.participantStatus.get(participant);

    String s = " ";
    if (status != null) {
      if (status.isWriting())
        s = " (writing...)";
      else if (status.isOnline()) s = " (online) ";
    }


    e.setInnerText(participant.getName() + s);
  }



}
