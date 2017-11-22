package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;

/**
 * Generic interface for classes that generates views of SNode objects.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface SViewBuilder {

  /**
   * Build a view of a SNode tree.
   *
   * @return a view of the SObject
   * @throws SException
   */
  public Object build() throws SException;

}
