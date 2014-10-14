package org.waveprotocol.mod.wavejs;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;

import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Utility methods for working with JavaScript
 *
 * @author pablojan@gmail.com
 *
 */
public class WaveJSUtils {


  public static native JsArray<JavaScriptObject> createJsArray() /*-{
    return new Array();
  }-*/;

  public static native JsArrayString createJsArrayString() /*-{
    return new Array();
  }-*/;

  public static JsArrayString toJsArray(Iterable<ParticipantId> participants) {

    JsArrayString array = createJsArrayString();
    for (ParticipantId p : participants)
      array.push(p.getAddress());

    return array;
  }

}
