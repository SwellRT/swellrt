package org.waveprotocol.wave.model.extended.type.chat;

import org.waveprotocol.wave.model.wave.ParticipantId;


/**
 * 
 * @author pablojan (Pablo Ojanguren)
 * 
 */
public class ChatMessage {


  class Initialiser {

    public ChatMessage message;

    public Initialiser(ChatMessage message) {
      this.message = message;
    }

  }

  public static final String TYPE_TEXT = "text";
  public static final String TYPE_INFO = "info";

  private final String type;
  private final String hash;
  private final String text;
  private final Long timestamp;
  private final ParticipantId creator;



  public ChatMessage.Initialiser getInitialiser() {
    return new Initialiser(this);
  }


  public ChatMessage(String hash, String text, Long timestamp, ParticipantId creator) {
    this.hash = hash;
    this.text = text;
    this.timestamp = timestamp;
    this.creator = creator;
    this.type = TYPE_TEXT;
  }

  public ChatMessage(String hash, String text, Long timestamp, ParticipantId creator, String type) {
    this.hash = hash;
    this.text = text;
    this.timestamp = timestamp;
    this.creator = creator;
    this.type = type;
  }


  public String getHash() {
    return hash;
  }

  public String getText() {
    return text;
  }

  public Long getTimestamp() {
    return timestamp;
  }

  public ParticipantId getCreator() {
    return creator;
  }

  public String getType() {
    return type;
  }

}
