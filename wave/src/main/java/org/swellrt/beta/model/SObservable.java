package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;

/**
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface SObservable {

  public void addListener(SHandler h, String event, String path) throws SException;

  public void removeListener(SHandler h, String event, String path);

}
