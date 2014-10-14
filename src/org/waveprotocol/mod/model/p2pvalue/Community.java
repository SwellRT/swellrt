package org.waveprotocol.mod.model.p2pvalue;

import org.waveprotocol.wave.model.wave.SourcesEvents;

public interface Community extends SourcesEvents<Community.Listener> {


  void setName(String name);

  String getName();



  public interface Listener {

    void onNameChanged(String name);
  }

}
