package org.waveprotocol.mod.wavejs.js.p2pvalue;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.mod.model.p2pvalue.Community;
import org.waveprotocol.mod.model.p2pvalue.Project;
import org.waveprotocol.mod.wavejs.js.adt.AdapterJS;


public class CommunityJS extends JavaScriptObject implements Community.Listener {

  private static AdapterJS projectAdapterJS = new ProjectAdapterJS();



  public native static CommunityJS create(Community delegate) /*-{

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
          delegate.@org.waveprotocol.mod.model.p2pvalue.Community::setName(Ljava/lang/String;)(name);
         },

         getName: function() {
          return delegate.@org.waveprotocol.mod.model.p2pvalue.Community::getName()();
         }

    }; // jso

    var _projects = delegate.@org.waveprotocol.mod.model.p2pvalue.Community::getProjects()();
    var _adapter = @org.waveprotocol.mod.wavejs.js.p2pvalue.CommunityJS::projectAdapterJS;
    jso.projects = @org.waveprotocol.mod.wavejs.js.adt.ObservableListJS::create(Lorg/waveprotocol/wave/model/adt/ObservableElementList;Lorg/waveprotocol/mod/wavejs/js/adt/AdapterJS;)
                (_projects, _adapter);

    return jso;

  }-*/;


  protected CommunityJS() {

  }

  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;



  @Override
  public final void onNameChanged(String name) {

  }


  @Override
  public final void onProjectAdded(Project project) {

  }


  @Override
  public final void onProjectRemoved(String projectId) {

  }

}
