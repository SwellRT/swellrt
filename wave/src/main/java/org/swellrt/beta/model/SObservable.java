package org.swellrt.beta.model;

/**
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface SObservable {
  
  public void listen(SHandler h);
  
  public void unlisten(SHandler h);
  
}
