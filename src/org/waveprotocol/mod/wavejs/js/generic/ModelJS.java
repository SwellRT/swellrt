package org.waveprotocol.mod.wavejs.js.generic;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.mod.model.generic.Model;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A JavaScript Wrapper for the CommunityWavelet class
 *
 * @author pablojan@gmail.com
 *
 */
public class ModelJS extends JavaScriptObject implements Model.Listener {


  //private final static AdapterJS itemAdapterJS = new ItemAdapterJS();


  public native static ModelJS create(Model delegate) /*-{

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
      var _participants = delegate.@org.waveprotocol.mod.model.generic.Model::getParticipants()();
      return @org.waveprotocol.mod.wavejs.WaveJSUtils::toJsArray(Ljava/lang/Iterable;)(_participants);
     },

     addParticipant: function(address) {
      return delegate.@org.waveprotocol.mod.model.generic.Model::addParticipant(Ljava/lang/String;)(address);
     },

     removeParticipant: function(address) {
      return delegate.@org.waveprotocol.mod.model.generic.Model::removeParticipant(Ljava/lang/String;)(address);
     },

     createMap: function() {
       var _map = delegate.@org.waveprotocol.mod.model.generic.Model::createMap()();
       return @org.waveprotocol.mod.wavejs.js.generic.AdapterTypeJS::adapt(Lorg/waveprotocol/mod/model/generic/Type;)(_map);
     },

     createString: function(strValue) {
       var _str = delegate.@org.waveprotocol.mod.model.generic.Model::createString(Ljava/lang/String;)(strValue);
       return @org.waveprotocol.mod.wavejs.js.generic.AdapterTypeJS::adapt(Lorg/waveprotocol/mod/model/generic/Type;)(_str);
     },

     createList: function() {
       var _list = delegate.@org.waveprotocol.mod.model.generic.Model::createList()();
       return @org.waveprotocol.mod.wavejs.js.generic.AdapterTypeJS::adapt(Lorg/waveprotocol/mod/model/generic/Type;)(_list);
     }

    }; // jso

    var _root =  delegate.@org.waveprotocol.mod.model.generic.Model::getRoot()();
    jso.root = @org.waveprotocol.mod.wavejs.js.generic.MapTypeJS::create(Lorg/waveprotocol/mod/model/generic/MapType;)(_root);

    return jso;

  }-*/;


  protected ModelJS() {

  }



  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;


  @Override
  public final void onAddParticipant(ParticipantId participant) {
    fireEvent("PARTICIPANT_ADDED", participant.getAddress());
  }


  @Override
  public final void onRemoveParticipant(ParticipantId participant) {
    fireEvent("PARTICIPANT_REMOVED", participant.getAddress());
  }


}
