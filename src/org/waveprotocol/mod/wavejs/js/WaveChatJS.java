package org.waveprotocol.mod.wavejs.js;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import org.waveprotocol.mod.model.showcase.chat.ChatMessage;
import org.waveprotocol.mod.model.showcase.chat.ChatPresenceStatus;
import org.waveprotocol.mod.model.showcase.chat.ObservableChat;
import org.waveprotocol.mod.model.showcase.chat.WaveChat;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.List;
import java.util.Set;

public class WaveChatJS extends JavaScriptObject implements ObservableChat.Listener {


  public native static WaveChatJS create(WaveChat delegate) /*-{


     var jsobject = {

       callbackMap: new Object(),

       eventHandlers: new Object(),

       send: function(text) {

           delegate.@org.waveprotocol.mod.model.showcase.chat.WaveChat::send(Ljava/lang/String;)(text);
         },

      getParticipants: function() {

            var _participants = delegate.@org.waveprotocol.mod.model.showcase.chat.WaveChat::getParticipants()();
            return @org.waveprotocol.mod.wavejs.js.WaveChatJS::getParticipantsAdapter(Ljava/util/Set;)(_participants);
         },

      addParticipant:  function(address) {

            return delegate.@org.waveprotocol.mod.model.showcase.chat.WaveChat::addParticipant(Ljava/lang/String;)(address);

      },

      removeParticipant: function(address) {

             return delegate.@org.waveprotocol.mod.model.showcase.chat.WaveChat::removeParticipant(Ljava/lang/String;)(address);

      },

      setStatus: function(status) {

            if (status == "online") {
              delegate.@org.waveprotocol.mod.model.showcase.chat.WaveChat::setStatusOnline()();
            }
            else if (status == "writing") {
              delegate.@org.waveprotocol.mod.model.showcase.chat.WaveChat::setStatusWriting()();
            }

      },

      getNumMessages: function() {

            return delegate.@org.waveprotocol.mod.model.showcase.chat.WaveChat::getNumMessages()();
      },

      getMessages: function(from, to) {

          var _messages = delegate.@org.waveprotocol.mod.model.showcase.chat.WaveChat::getMessages(II)(from, to);
          return @org.waveprotocol.mod.wavejs.js.WaveChatJS::getMessagesAdapter(Ljava/util/List;)(_messages);
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

  @SuppressWarnings("rawtypes")
  protected final static native JsArray getJsArray(int size) /*-{
    return new Array(size);
  }-*/;


  protected static JsArrayString getParticipantsAdapter(Set<ParticipantId> participants) {

    JsArrayString array = getJsArrayString(participants.size());
    for (ParticipantId p: participants)
      array.push(p.getAddress());

    return array;
  }

  protected static JsArray<ChatMessageJS> getMessagesAdapter(List<ChatMessage> messages) {

    @SuppressWarnings({"unchecked"})
    JsArray<ChatMessageJS> array = getJsArray(messages.size());
    for (ChatMessage m : messages)
      array.push(ChatMessageJS.create(m));

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

    JsArrayString jsStatus = getJsArrayString(2);
    jsStatus.push(participant.getAddress());

    if (status.isWriting()) {
      jsStatus.push("writing");
    } else if (status.isOnline()) {
      jsStatus.push("online");
    } else {
      jsStatus.push("offline");
    }

    this.fireEvent("onParticipantStatusChanged", jsStatus);

  }



}
