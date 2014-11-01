package org.waveprotocol.mod.wavejs.js.p2pvalue;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.mod.model.p2pvalue.Project;

/**
 * A JavaScript wrapper for the Project class
 *
 * @author pablojan@gmail.com
 *
 */
public class ProjectJS extends JavaScriptObject implements Project.Listener {



    public native static ProjectJS create(Project delegate) /*-{

      var jso = {

       callbackMap: new Object(),

       eventHandlers: new Object(),

       registerEventHandler: function(event, handler) {
        this.eventHandlers[event] = handler;
       },

       unregisterEventHandler: function(event, handler) {
        this.eventHandlers[event] = null;
       },

       getId: function() {
         return delegate.@org.waveprotocol.mod.model.p2pvalue.Project::getDocumentId()();
       },

       setName: function(name) {
         delegate.@org.waveprotocol.mod.model.p2pvalue.Project::setName(Ljava/lang/String;)(name);
       },

       getName: function() {
         return delegate.@org.waveprotocol.mod.model.p2pvalue.Project::getName()();
       },

       setStatus: function(status) {
         delegate.@org.waveprotocol.mod.model.p2pvalue.Project::setStatus(Ljava/lang/String;)(status);
       },

       getStatus: function() {
         return delegate.@org.waveprotocol.mod.model.p2pvalue.Project::getStatus()();
       },

       setDescription: function(description) {
         delegate.@org.waveprotocol.mod.model.p2pvalue.Project::setDescription(Ljava/lang/String;)(description);
       },

       getDescription: function() {
         return delegate.@org.waveprotocol.mod.model.p2pvalue.Project::getDescription()();
       }

      }; // jso

      return jso;

  }-*/;


  protected ProjectJS() {

  }


  public final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;



  @Override
  public final void onStatusChanged(String name) {
    // TODO Auto-generated method stub

  }



}
