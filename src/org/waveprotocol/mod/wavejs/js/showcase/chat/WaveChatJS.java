package org.waveprotocol.mod.wavejs.js.showcase.chat;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import org.waveprotocol.mod.model.showcase.chat.ChatMessage;
import org.waveprotocol.mod.model.showcase.chat.ChatPresenceStatus;
import org.waveprotocol.mod.model.showcase.chat.ObservableChat;
import org.waveprotocol.mod.model.showcase.chat.WaveChat;
import org.waveprotocol.mod.wavejs.WaveJSUtils;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A JavaScript Wrapper for the WaveChat class
 *
 * @author pablojan@gmail.com
 *
 */
public class WaveChatJS extends JavaScriptObject implements ObservableChat.Listener {


  public native static WaveChatJS create(WaveChat delegate) /*-{


     var jso = {

       callbackMap: new Object(),

       eventHandlers: new Object(),

       send: function(text) {

           delegate.@org.waveprotocol.mod.model.showcase.chat.WaveChat::send(Ljava/lang/String;)(text);
         },

      getParticipants: function() {

            var _participants = delegate.@org.waveprotocol.mod.model.showcase.chat.WaveChat::getParticipants()();
            return @org.waveprotocol.mod.wavejs.WaveJSUtils::toJsArray(Ljava/lang/Iterable;)(_participants);
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
          return @org.waveprotocol.mod.wavejs.js.showcase.chat.ChatMessageJS::create(Ljava/lang/Iterable;)(_messages);
      },


      registerEventHandler: function(event, handler) {

        this.eventHandlers[event] = handler;

      },

      unregisterEventHandler: function(event, handler) {

        this.eventHandlers[event] = null;

      }


    }; // jso

    return jso;

  }-*/;



  protected WaveChatJS() {

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

    JsArrayString jsStatus = WaveJSUtils.createJsArrayString();
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
