package org.waveprotocol.wave.client.account;

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
public interface ServerAccountData {

  @JsProperty
  public String getId();

  @JsProperty
  public String getEmail();

  @JsProperty
  public String getLocale();

  @JsProperty
  public String getAvatarUrl();

  @JsProperty
  public String getName();

  @JsProperty
  public String getSessionId();

  @JsProperty
  public String getTransientSessionId();

  @JsProperty
  public String getDomain();

}
