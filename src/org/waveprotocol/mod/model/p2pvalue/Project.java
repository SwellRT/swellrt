package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.mod.model.p2pvalue.docindex.DocIndexed;
import org.waveprotocol.wave.model.adt.ObservableElementList;
import org.waveprotocol.wave.model.wave.SourcesEvents;



/**
 *
 *
 * @author pablojan@gmail.com
 *
 */
public interface Project extends DocIndexed, SourcesEvents<Project.Listener> {

  public class Initializer {

    public String name, status, description;

    Initializer(String name, String status, String description) {
      this.name = name;
      this.status = status;
      this.description = description;
    }

  }

  // Project data

  void setName(String name);

  String getName();

  void setStatus(String status);

  String getStatus();

  void setDescription(String description);

  String getDescription();


  // Tasks

  ObservableElementList<Task, Task.Initialiser> getTasks();



  public interface Listener {

    void onStatusChanged(String name);

  }

}
