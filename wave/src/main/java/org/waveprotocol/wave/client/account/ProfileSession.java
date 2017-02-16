package org.waveprotocol.wave.client.account;

import org.waveprotocol.wave.client.common.util.RgbColor;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt.Service", name = "ProfileSession")
public interface ProfileSession {
  
  @JsProperty
  String getSessionId();
  
  @JsProperty
  RgbColor getColor();
  
  @JsProperty
  boolean isOnline();
  
  @JsIgnore
  void setOnline();
    
  @JsIgnore
  void setOffline();
  
  @JsProperty
  Profile getProfile();
}
