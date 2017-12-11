package org.swellrt.beta.client.rest.operations.params;

import org.swellrt.beta.client.rest.ServiceOperation;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * The profile data which can be retrieved from server.
 * <p>
 * Client classes must adapt this data to handy types.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
@JsType(isNative = true)
public interface Account extends ServiceOperation.Response, ServiceOperation.Options {

  @JsProperty
  public String getId();

  @JsProperty
  public String getEmail();

  @JsProperty
  public String getLocale();

  @JsProperty
  public String getAvatarUrl();

  @JsProperty
  public String getSessionId();

  @JsProperty
  public String getTransientSessionId();

  @JsProperty
  public String getDomain();

  @JsProperty
  public String getName();

  /** used only as input */
  @JsProperty
  public String getPassword();

  /** used only as input */
  @JsProperty
  public String getAvatarData();

}
