package org.swellrt.beta.client.platform.web.editor;

import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.model.document.util.Range;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsType;

/**
 * A range of text.
 */
@JsType(isNative = true, namespace = "swell", name = "Range")
public class SRange {

  @JsOverlay
  public final static SRange create(Range range) {
    JsoView jsv = JsoView.create();
    jsv.setNumber("start", range.getStart());
    jsv.setNumber("end", range.getEnd());
    Object notyped = jsv;
    return (SRange) notyped;
  }

  @JsOverlay
  public final static SRange create(int start, int end) {
    JsoView jsv = JsoView.create();
    jsv.setNumber("start", start);
    jsv.setNumber("end", end);
    Object notyped = jsv;
    return (SRange) notyped;
  }

  public int start;
  public int end;

  @JsOverlay
  public final Range asRange() {
    return new Range(start, end);
  }

}
