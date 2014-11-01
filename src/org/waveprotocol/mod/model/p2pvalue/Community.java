package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.wave.SourcesEvents;

public interface Community extends SourcesEvents<Community.Listener> {


  // Community data

  void setName(String name);

  String getName();


  // Projects

  ObservableElementList<Project, Project.Initializer> getProjects();


  // Events

  public interface Listener {

    void onNameChanged(String name);

    void onProjectAdded(Project project);

    void onProjectRemoved(String projectId);

  }

}
