package org.waveprotocol.mod.wavejs.js.showcase.chat;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.waveprotocol.mod.model.showcase.chat.ChatMessage;
import org.waveprotocol.mod.wavejs.WaveJSUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

public class ChatMessageJS extends JavaScriptObject {


  public native static ChatMessageJS create(ChatMessage delegate) /*-{

      var jsobject = new Object();

      jsobject.text = delegate.@org.waveprotocol.mod.model.showcase.chat.ChatMessage::getText()();
      jsobject.timestamp = delegate.@org.waveprotocol.mod.model.showcase.chat.ChatMessage::getTimestamp()();
      jsobject.type = delegate.@org.waveprotocol.mod.model.showcase.chat.ChatMessage::getType()();

      var _creator = delegate.@org.waveprotocol.mod.model.showcase.chat.ChatMessage::getCreator()();
      jsobject.creator = @org.waveprotocol.mod.wavejs.js.showcase.chat.ChatMessageJS::getParticipantAdapter(Lorg/waveprotocol/wave/model/wave/ParticipantId;)(_creator);

      return jsobject;


  }-*/;


  public static JsArray<JavaScriptObject> create(Iterable<ChatMessage> delegators) {

    JsArray<JavaScriptObject> array = WaveJSUtils.createJsArray();
    for (ChatMessage delegate : delegators)
      array.push(ChatMessageJS.create(delegate));

    return array;
  }

  protected ChatMessageJS() {

  }


  protected static String getParticipantAdapter(ParticipantId participant) {

        return participant.getAddress();
  }

}