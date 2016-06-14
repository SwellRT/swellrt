package org.swellrt.server.box.index;

import org.waveprotocol.box.server.waveserver.WaveBus.Subscriber;
import org.waveprotocol.box.server.waveserver.WaveServerException;


public interface ModelIndexerDispatcher extends Subscriber {

  /**
   * Add wavelets to the SwellRT model index on server start up.
   * 
   * @throws WaveServerException
   */
  public void initialize() throws WaveServerException;

}
