package org.swellrt.beta.client.wave;

import com.google.gwt.user.client.Command;

public interface WaveStages {

  void load(Command whenFinished);

  boolean isLoaded();

}
