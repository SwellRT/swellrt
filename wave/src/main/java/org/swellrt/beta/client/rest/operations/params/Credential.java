package org.swellrt.beta.client.rest.operations.params;

import org.swellrt.beta.client.rest.ServiceOperation;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface Credential extends ServiceOperation.Options {

  @JsProperty
  public String getId();

  @JsProperty
  public String getPassword();

  @JsProperty
  public boolean getRemember();

  @JsProperty
  public void setId(String id);

  @JsProperty
  public void setPassword(String password);

  @JsProperty
  public void setRemember(boolean remember);

}
