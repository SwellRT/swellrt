package org.swellrt.beta.client.platform.web.editor;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface CaretInfo {

  @JsProperty
  String getSession();

  @JsProperty
  String getParticipant();

  @JsProperty
  int getTimestamp();

  @JsProperty
  int getPosition();
}
