package org.swellrt.beta.client;

import org.swellrt.beta.common.SException;

public interface ServiceStatus {

  /**  
   * @throws SException if the associated service can't be used.
   */
	public void check() throws SException;
	
}
