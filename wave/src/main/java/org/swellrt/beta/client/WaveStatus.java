package org.swellrt.beta.client;

import org.swellrt.beta.common.SException;

public interface WaveStatus {

  /**  
   * @throws SException if the associated wave can't be used.
   */
	public void check() throws SException;
	
}
