package org.waveprotocol.mod.wavejs.js.dummy;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.mod.model.dummy.ListModel;
import org.waveprotocol.mod.wavejs.js.adt.AdapterJS;
import org.waveprotocol.mod.wavejs.js.adt.ObservableListJS;
import org.waveprotocol.wave.model.adt.BasicValue;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A JavaScript Wrapper for the CommunityWavelet class
 *
 * @author pablojan@gmail.com
 *
 */
public class ListModelJS extends JavaScriptObject implements ListModel.Listener {


  private final static AdapterJS itemAdapterJS = new ItemAdapterJS();


  public native static ListModelJS create(ListModel delegate) /*-{

    var jso = {

     callbackMap: new Object(),

     eventHandlers: new Object(),

     registerEventHandler: function(event, handler) {
      this.eventHandlers[event] = handler;
     },

     unregisterEventHandler: function(event, handler) {
      this.eventHandlers[event] = null;
     },

     getParticipants: function() {
      var _participants = delegate.@org.waveprotocol.mod.model.dummy.ListModel::getParticipants()();
      return @org.waveprotocol.mod.wavejs.WaveJSUtils::participantIterableToJs(Ljava/lang/Iterable;)(_participants);
     },

     addParticipant: function(address) {
      return delegate.@org.waveprotocol.mod.model.dummy.ListModel::addParticipant(Ljava/lang/String;)(address);
     },

     removeParticipant: function(address) {
      return delegate.@org.waveprotocol.mod.model.dummy.ListModel::removeParticipant(Ljava/lang/String;)(address);
     }

    }; // jso

    jso.list = @org.waveprotocol.mod.wavejs.js.dummy.ListModelJS::createListJS(Lorg/waveprotocol/mod/model/dummy/ListModel;)(delegate);

    return jso;

  }-*/;


  protected ListModelJS() {

  }


  @SuppressWarnings("unchecked")
  private final static ObservableListJS createListJS(ListModel listModel) {

    ObservableListJS listJS = ObservableListJS.create(listModel.getList(), itemAdapterJS);
    listModel.getList().addListener(listJS);

    return listJS;

  }

  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;


  @Override
  public final void onAddParticipant(ParticipantId participant) {
    fireEvent("onAddParticipant", participant.getAddress());
  }


  @Override
  public final void onRemoveParticipant(ParticipantId participant) {
    fireEvent("onRemoveParticipant", participant.getAddress());
  }


}
