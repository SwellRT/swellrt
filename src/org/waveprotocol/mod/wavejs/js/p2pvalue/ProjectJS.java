package org.waveprotocol.mod.wavejs.js.p2pvalue;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.waveprotocol.mod.model.p2pvalue.Project;
import org.waveprotocol.mod.model.p2pvalue.Task;
import org.waveprotocol.mod.wavejs.WaveJSUtils;

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

     setName: function(name) {
       delegate.@org.waveprotocol.mod.model.p2pvalue.Project::setName(Ljava/lang/String;)(name);
     },

     getName: function() {
       return delegate.@org.waveprotocol.mod.model.p2pvalue.Project::getName()();
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


  public static JsArray<JavaScriptObject> create(Iterable<Project> delegators) {

    JsArray<JavaScriptObject> array = WaveJSUtils.createJsArray();
    for (Project delegate : delegators)
      array.push(ProjectJS.create(delegate));

    return array;
  }

  public final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;

  @Override
  public void onStatusChanged(String name) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onTaskAdded(Task task) {
    // TODO Auto-generated method stub

  }

  @Override
  public void onTaskRemoved(Task task) {
    // TODO Auto-generated method stub

  }

}
