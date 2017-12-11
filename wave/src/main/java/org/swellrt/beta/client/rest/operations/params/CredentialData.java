package org.swellrt.beta.client.rest.operations.params;

import org.swellrt.beta.client.rest.ServerOperation;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface CredentialData extends ServerOperation.Options {

  @JsProperty
  public String getId();

  @JsProperty
  public String getOldPassword();

  @JsProperty
  public String getNewPassword();

  @JsProperty
  public String getToken();

  /** recovery email */
  @JsProperty
  public String getEmail();

  /** recovery url */
  @JsProperty
  public String getUrl();

}
