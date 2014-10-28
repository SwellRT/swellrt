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
       },

       getNumTasks: function() {
         return delegate.@org.waveprotocol.mod.model.p2pvalue.Project::getNumTasks()();
       },

       getTask: function(index) {
         var _task = delegate.@org.waveprotocol.mod.model.p2pvalue.Project::getTask(I)(index);
         return @org.waveprotocol.mod.wavejs.js.p2pvalue.ProjectJS::getTaskJS(Lorg/waveprotocol/mod/model/p2pvalue/Task;)(_task);
       },

       getTasks: function() {
         var _tasks = delegate.@org.waveprotocol.mod.model.p2pvalue.Project::getTasks()();
         return @org.waveprotocol.mod.wavejs.js.p2pvalue.ProjectJS::tasksToJsArray(Ljava/lang/Iterable;)(_tasks);
       },

       removeTask: function(task) {
         delegate.@org.waveprotocol.mod.model.p2pvalue.Project::removeTask(Lorg/waveprotocol/mod/model/p2pvalue/Task;)(task);
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


  private static final JsArray<JavaScriptObject> tasksToJsArray(Iterable<Task> tasks) {

    JsArray<JavaScriptObject> array = WaveJSUtils.createJsArray();

    for (Task t : tasks)
      array.push(getTaskJS(t));

    return array;
  }

  private static final TaskJS getTaskJS(Task task) {

    TaskJS taskJS = TaskJS.create(task);
    task.addListener(taskJS);
    return taskJS;

  }

  @Override
  public final void onStatusChanged(String name) {
    // TODO Auto-generated method stub

  }

  @Override
  public final void onTaskAdded(Task task) {
    // TODO Auto-generated method stub

  }

  @Override
  public final void onTaskRemoved(Task task) {
    // TODO Auto-generated method stub

  }

}
