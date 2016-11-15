package org.swellrt.beta.client.operation.data;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface ProfileData {

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
  public String getDomain();
  
}
