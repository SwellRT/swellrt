package org.waveprotocol.wavejs.js;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.wave.model.extended.type.chat.ChatMessage;
import org.waveprotocol.wave.model.wave.ParticipantId;

public class ChatMessageJS extends JavaScriptObject {


  public native static ChatMessageJS create(ChatMessage delegate) /*-{

      var jsobject = new Object();

      jsobject.text = delegate.@org.waveprotocol.wave.model.extended.type.chat.ChatMessage::getText()();
      jsobject.timestamp = delegate.@org.waveprotocol.wave.model.extended.type.chat.ChatMessage::getTimestamp()();
      jsobject.type = delegate.@org.waveprotocol.wave.model.extended.type.chat.ChatMessage::getType()();

      var _creator = delegate.@org.waveprotocol.wave.model.extended.type.chat.ChatMessage::getCreator()();
      jsobject.creator = @org.waveprotocol.wavejs.js.ChatMessageJS::getParticipantAdapter(Lorg/waveprotocol/wave/model/wave/ParticipantId;)(_creator);

      return jsobject;


  }-*/;

  protected ChatMessageJS() {

  }

  protected static String getParticipantAdapter(ParticipantId participant) {

        return participant.getAddress();
  }

}