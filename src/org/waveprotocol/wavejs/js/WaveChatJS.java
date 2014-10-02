package org.waveprotocol.wavejs.js;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import org.waveprotocol.wave.client.extended.type.WaveChat;
import org.waveprotocol.wave.model.extended.type.chat.ChatMessage;
import org.waveprotocol.wave.model.extended.type.chat.ChatPresenceStatus;
import org.waveprotocol.wave.model.extended.type.chat.ObservableChat;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

public class WaveChatJS extends JavaScriptObject implements ObservableChat.Listener {


  public native static WaveChatJS create(WaveChat delegate) /*-{


     var jsobject = {

       callbackMap: new Object(),

       eventHandlers: new Object(),

       send: function(text) {

            $entry(delegate.@org.waveprotocol.wave.client.extended.type.WaveChat::send(Ljava/lang/String;)(text));
         },

      getParticipants: function() {

            var _participants = $entry(delegate.@org.waveprotocol.wave.client.extended.type.WaveChat::getParticipants()());
            return @org.waveprotocol.wavejs.js.WaveChatJS::getParticipantsAdapter(Ljava/util/Set;)(_participants);
         },

      addParticipant:  function(address) {

            $entry(delegate.@org.waveprotocol.wave.client.extended.type.WaveChat::addParticipant(Ljava/lang/String;)(address));

      },

      removeParticipant: function(address) {

             $entry(delegate.@org.waveprotocol.wave.client.extended.type.WaveChat::removeParticipant(Ljava/lang/String;)(address));

      },

      registerEventHandler: function(event, handler) {

        this.eventHandlers[event] = handler;

      },

      unregisterEventHandler: function(event, handler) {

        this.eventHandlers[event] = null;

      }


    }; // jsobject

    return jsobject;

  }-*/;



  protected WaveChatJS() {

  }

  protected final static native JsArrayString getJsArrayString(int size) /*-{
    return new Array(size);
  }-*/;

  protected static JsArrayString getParticipantsAdapter(Set<ParticipantId> participants) {

    JsArrayString array = getJsArrayString(participants.size());
    for (ParticipantId p: participants)
      array.push(p.getAddress());

    return array;
  }


  public final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;


  @Override
  public final void onMessageAdded(ChatMessage message) {
    this.fireEvent("onMessageAdded", message.getText());
  }



  @Override
  public final void onParticipantAdded(ParticipantId participant) {
    this.fireEvent("onParticipantAdded", participant.getAddress());
  }



  @Override
  public final void onParticipantRemoved(ParticipantId participant) {
    this.fireEvent("onParticipantRemoved", participant.getAddress());
  }



  @Override
  public final void onParticipantStatusChanged(ParticipantId participant, ChatPresenceStatus status) {
    // TODO Auto-generated method stub

  }



}
