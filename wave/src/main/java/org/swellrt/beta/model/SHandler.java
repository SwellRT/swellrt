package org.swellrt.beta.model;

public interface SHandler {

  /** 
   * Return false to prevent recursive upwards propagation to parent node.
   * @param e
   * @return
   */
  public boolean onEvent(SEvent e);
  
}
