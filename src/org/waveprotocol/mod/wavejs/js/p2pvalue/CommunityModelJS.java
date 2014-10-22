package org.waveprotocol.mod.wavejs.js.p2pvalue;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.mod.model.p2pvalue.Community;
import org.waveprotocol.mod.model.p2pvalue.CommunityModel;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A JavaScript Wrapper for the CommunityWavelet class
 *
 * @author pablojan@gmail.com
 *
 */
public class CommunityModelJS extends JavaScriptObject implements CommunityModel.Listener {


  public native static CommunityModelJS create(CommunityModel delegate) /*-{

    var jso = {

     callbackMap: new Object(),

     eventHandlers: new Object(),

     registerEventHandler: function(event, handler) {
      this.eventHandlers[event] = handler;
     },

     unregisterEventHandler: function(event, handler) {
      this.eventHandlers[event] = null;
     },

     getCommunity: function() {
      var _community = delegate.@org.waveprotocol.mod.model.p2pvalue.CommunityModel::getCommunity()();
      return @org.waveprotocol.mod.wavejs.js.p2pvalue.CommunityModelJS::getCommunityJS(Lorg/waveprotocol/mod/model/p2pvalue/Community;)(_community);
     },

     getParticipants: function() {
      var _participants = delegate.@org.waveprotocol.mod.model.p2pvalue.CommunityModel::getParticipants()();
      return @org.waveprotocol.mod.wavejs.WaveJSUtils::toJsArray(Ljava/lang/Iterable;)(_participants);
     },

     addParticipant: function(address) {
      return delegate.@org.waveprotocol.mod.model.p2pvalue.CommunityModel::addParticipant(Ljava/lang/String;)(address);
     },

     removeParticipant: function(address) {
      return delegate.@org.waveprotocol.mod.model.p2pvalue.CommunityModel::removeParticipant(Ljava/lang/String;)(address);
     }

    }; // jso

    return jso;

  }-*/;


  protected CommunityModelJS() {

  }


  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;


  private static final CommunityJS getCommunityJS(Community community) {
    CommunityJS communityJS = CommunityJS.create(community);
    community.addListener(communityJS);
    return communityJS;
  }


  @Override
  public final void onAddParticipant(ParticipantId participant) {
    fireEvent("onAddParticipant", participant.getAddress());
  }


  @Override
  public final void onRemoveParticipant(ParticipantId participant) {
    fireEvent("onRemoveParticipant", participant.getAddress());
  }


}
