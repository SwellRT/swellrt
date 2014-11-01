package org.waveprotocol.mod.wavejs.js.p2pvalue;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.mod.model.p2pvalue.Project;
import org.waveprotocol.mod.wavejs.js.adt.AdapterJS;

public class ProjectAdapterJS implements AdapterJS {

  @Override
  public JavaScriptObject adaptToJS(Object o) {

    Project project = (Project) o;
    ProjectJS projectJS = ProjectJS.create(project);
    project.addListener(projectJS);

    return projectJS;
  }

  @Override
  public Object initFromJS(JavaScriptObject initialState) {
    // TODO Auto-generated method stub
    return null;
  }

}
