package org.swellrt.beta.client.rest.operations.params;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface LogDocDelta {

  @JsProperty
  public String getVersion();

  @JsProperty
  public String getAuthor();

  @JsProperty
  public double getTime();

  @JsProperty
  public Object[] getOps();

}
