package org.waveprotocol.wave.model.document.util;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "IRange", isNative = true)
public interface IRange {

  @JsProperty
  int getStart();

  @JsProperty
  int getEnd();

}
