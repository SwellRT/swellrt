package org.waveprotocol.mod.model.p2pvalue;


/**
 * 
 * 
 * @author pablojan@gmail.com
 * 
 */
public interface Project {

  void setName(String name);

  String getName();

  void setStatus(String status);

  String getStatus();

  void setDescription(String description);

  String getDescription();

  // Tasks

  int numOfTasks();

  Task getTask(int index);

  void addTask(Task.Initialiser task);

  void removeTask(Task task);

  int getTaskIndex(Task task);

  Iterable<? extends Task> getTasks();


  public interface Listener {

    void onStatusChanged(String name);

    void onTaskAdded(Task task);

    void onTaskRemoved(Task task);

  }

}
