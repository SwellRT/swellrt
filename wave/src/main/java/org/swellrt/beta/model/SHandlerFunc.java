package org.swellrt.beta.model;

import jsinterop.annotations.JsFunction;

@JsFunction
public interface SHandlerFunc {

  /** 
   * Return false to prevent recursive upwards propagation to parent node.
   * @param e
   * @return
   */
  public boolean exec(SEvent e);
  
}
