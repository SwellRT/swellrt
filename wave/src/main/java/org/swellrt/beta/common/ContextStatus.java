package org.swellrt.beta.common;

/**
 * Provide logic to check health of the Swell runtime context.
 *
 */
public interface ContextStatus {

  /**
   * @throws SException if the associated wave can't be used.
   */
	public void check() throws SException;

}
