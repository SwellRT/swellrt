package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;

/**
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public interface SObservable {

  public void addListener(SHandlerFunc h, String path) throws SException;

  public void removeListener(SHandlerFunc h, String path) throws SException;

}
