package org.swellrt.beta.client.rest.operations.params;

import org.swellrt.beta.client.rest.ServiceOperation;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface Delta extends ServiceOperation.Options {

  @JsProperty
  public String getVersion();

  @JsProperty
  public String getAuthor();

  @JsProperty
  public double getTime();

  @JsProperty
  public Object getOps();
}
