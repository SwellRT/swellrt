package org.swellrt.server.box;

import org.waveprotocol.box.server.waveserver.WaveBus.Subscriber;
import org.waveprotocol.box.server.waveserver.WaveServerException;


public interface SwellRtIndexerDispatcher extends Subscriber {

  /**
   * Add wavelets to the SwellRT model index on server start up.
   * 
   * @throws WaveServerException
   */
  public void initialize() throws WaveServerException;

}
