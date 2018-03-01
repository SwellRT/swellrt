package org.waveprotocol.wave.client.wave;

import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(isNative = true)
public interface DiffData<R extends DiffData.Range<V>, V extends DiffData.Values> {

  @JsType(isNative = true)
  public interface Values {

    @JsProperty
    public String getAuthor();

  }

  @JsType(isNative = true)
  public interface Range<V extends DiffData.Values> {

    @JsProperty
    public int getStart();

    @JsProperty
    public int getEnd();

    @JsProperty
    public V getValues();

  }

  @JsProperty
  public String getDocId();

  @JsProperty
  public R[] getRanges();

}
