package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;

import jsinterop.annotations.JsType;

/**
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(namespace = "swell", name = "ObservableObject")
public interface SObservableNode {

  public void addListener(SMutationHandler h, String path) throws SException;

  public void listen(SMutationHandler h) throws SException;

  public void removeListener(SMutationHandler h, String path) throws SException;

  public void unlisten(SMutationHandler h) throws SException;

}
