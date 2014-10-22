package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.wave.model.wave.SourcesEvents;

import java.util.List;

public interface Community extends SourcesEvents<Community.Listener> {


  // Meta data

  String getDocumentId();

  // Community data

  void setName(String name);

  String getName();


  // Projects

  List<Project> getProjects();

  Project addProject();

  void removeProject(String projectId);


  // Events

  public interface Listener {

    void onNameChanged(String name);

    void onProjectAdded(Project project);

    void onProjectRemoved(String projectId);

  }

}
