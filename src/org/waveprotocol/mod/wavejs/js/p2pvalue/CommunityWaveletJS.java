package org.waveprotocol.mod.wavejs.js.p2pvalue;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.mod.model.p2pvalue.CommunityWavelet;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A JavaScript Wrapper for the CommunityWavelet class
 *
 * @author pablojan@gmail.com
 *
 */
public class CommunityWaveletJS extends JavaScriptObject implements CommunityWavelet.Listener {


  public native static CommunityWaveletJS create(CommunityWavelet delegate) /*-{

    var jso = {

     callbackMap: new Object(),

     eventHandlers: new Object(),

     registerEventHandler: function(event, handler) {
      this.eventHandlers[event] = handler;
     },

     unregisterEventHandler: function(event, handler) {
      this.eventHandlers[event] = null;
     },

     setName: function(name) {
       delegate.@org.waveprotocol.mod.model.p2pvalue.CommunityWavelet::setName(Ljava/lang/String;)(name);
     },

     getName: function() {
       return delegate.@org.waveprotocol.mod.model.p2pvalue.CommunityWavelet::getName()();
     },

     getNumProjects: function {
        return delegate.@org.waveprotocol.mod.model.p2pvalue.CommunityWavelet::getNumProjects()();
     },

     getProjects: function(from, to) {
        var _projects = delegate.@org.waveprotocol.mod.model.p2pvalue.CommunityWavelet::getProjects(II)(from, to);

     },

     getParticipants: function() {

     },

     addParticipant: function() {

     },

     removeParticipant: function() {

     }




    }; // jso

    return jso;

  }-*/;


  public final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;

  @Override
  public void onAddParticipant(ParticipantId participant) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onRemoveParticipant(ParticipantId participant) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onProjectAdded(String projectId) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onProjectRemoved(String projectId) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onNameChanged(String name) {
    // TODO Auto-generated method stub

  }

}
