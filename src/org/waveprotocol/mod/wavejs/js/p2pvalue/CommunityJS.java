package org.waveprotocol.mod.wavejs.js.p2pvalue;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.waveprotocol.mod.model.p2pvalue.Community;
import org.waveprotocol.mod.model.p2pvalue.Project;
import org.waveprotocol.mod.wavejs.WaveJSUtils;

public class CommunityJS extends JavaScriptObject implements Community.Listener {


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
         },

         getProjects: function() {
          var _projects = delegate.@org.waveprotocol.mod.model.p2pvalue.Community::getProjects()();
          return @org.waveprotocol.mod.wavejs.js.p2pvalue.CommunityJS::projectsToJsArray(Ljava/lang/Iterable;)(_projects);
         },

         addProject: function() {
          var _project = delegate.@org.waveprotocol.mod.model.p2pvalue.Community::addProject()();
          return @org.waveprotocol.mod.wavejs.js.p2pvalue.CommunityJS::getProjectJS(Lorg/waveprotocol/mod/model/p2pvalue/Project;)(_project);
         },

         removeProject: function(projectid) {
          delegate.@org.waveprotocol.mod.model.p2pvalue.Community::removeProject(Ljava/lang/String;)(projectid);
         }


    }; // jso

    return jso;

  }-*/;


  protected CommunityJS() {

  }

  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;


  private static final JsArray<JavaScriptObject> projectsToJsArray(Iterable<Project> projects) {

    JsArray<JavaScriptObject> array = WaveJSUtils.createJsArray();

    for (Project p : projects)
      array.push(getProjectJS(p));

    return array;
  }

  private static final ProjectJS getProjectJS(Project project) {

    ProjectJS projectJS = ProjectJS.create(project);
    project.addListener(projectJS);
    return projectJS;

  }

  @Override
  public final void onNameChanged(String name) {
    fireEvent("onNameChanged", name);
  }


  @Override
  public final void onProjectAdded(Project project) {
    fireEvent("onProjectAdded", getProjectJS(project));
  }


  @Override
  public final void onProjectRemoved(String projectId) {
    fireEvent("onProjectRemoved", projectId);
  }

}
