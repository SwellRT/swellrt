package org.swellrt.beta.client.rest.operations.params;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface LogDocRevision {

  @JsProperty
  public String getAppliedAt();

  @JsProperty
  public String getResulting();

  @JsProperty
  public String getAuthor();

  @JsProperty
  public double getTime();

  @JsProperty
  public Object getOp();

}
