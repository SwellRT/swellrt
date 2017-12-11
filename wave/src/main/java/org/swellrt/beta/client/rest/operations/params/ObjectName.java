package org.swellrt.beta.client.rest.operations.params;

import org.swellrt.beta.client.rest.ServiceOperation;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface ObjectName extends ServiceOperation.Options {

  /** a Wave id */
  @JsProperty
  public String getId();

  /** a Wave name */
  @JsProperty
  public String getName();

}
