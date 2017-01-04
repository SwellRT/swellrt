package org.swellrt.beta.model;

/**
 * 
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface SObservable {
  
  public void addHandler(SHandler h);
  
  public void deleteHandler(SHandler h);
  
}
