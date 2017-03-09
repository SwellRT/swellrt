package org.waveprotocol.wave.client.account;

import org.waveprotocol.wave.client.common.util.RgbColor;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "ProfileSession")
public interface ProfileSession {
  
  @JsProperty(name = "id")
  String getId();
  
  @JsProperty
  RgbColor getColor();
  
  @JsProperty
  boolean isOnline();
  
  @JsIgnore
  void trackActivity(double timestamp);
  
  @JsIgnore
  void trackActivity();
  
  @JsIgnore
  void setOffline();
  
  @JsProperty
  Profile getProfile();

  @JsProperty
  double getLastActivityTime();
}
