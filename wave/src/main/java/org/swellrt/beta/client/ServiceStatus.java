package org.swellrt.beta.client;

import org.swellrt.beta.common.SException;

public interface ServiceStatus {

  /**
   * @throws SException
   *           if the associated service can't be used.
   */
  public void check() throws SException;

  /**
   * Raise an exception from a WaveContext to the ServiceContext.
   * 
   * @param waveId
   * @param ex
   */
  public void raise(String waveId, SException ex);

}
