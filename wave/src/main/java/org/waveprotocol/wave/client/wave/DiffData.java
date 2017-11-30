package org.waveprotocol.wave.client.wave;

import jsinterop.annotations.JsType;

@JsType(isNative = true)
public class DiffData {

  @JsType(isNative = true)
  public static class Values {

    public String author;

  }

  @JsType(isNative = true)
  public interface WaveletDiffData {

    DiffData[] get(String blipId);

  }

  public int start;
  public int end;
  public Values values;

}
