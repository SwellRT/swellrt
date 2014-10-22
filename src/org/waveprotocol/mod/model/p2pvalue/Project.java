package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.wave.model.wave.SourcesEvents;



/**
 *
 *
 * @author pablojan@gmail.com
 *
 */
public interface Project extends SourcesEvents<Project.Listener> {


  // Meta data

  String getDocumentId();

  // Project data

  void setName(String name);

  String getName();

  void setStatus(String status);

  String getStatus();

  void setDescription(String description);

  String getDescription();

  // Tasks

  int getNumTasks();

  Iterable<Task> getTasks();

  Task addTask(Task.Initialiser task);

  void removeTask(Task task);

  Task getTask(int index);



  public interface Listener {

    void onStatusChanged(String name);

    void onTaskAdded(Task task);

    void onTaskRemoved(Task task);

  }

}
