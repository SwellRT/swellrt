package org.waveprotocol.mod.wavejs;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Random;

import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Utility methods for working with JavaScript
 *
 * @author pablojan@gmail.com
 *
 */
public class WaveJSUtils {


  static final char[] WEB64_ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
      .toCharArray();

  public static native JsArray<JavaScriptObject> createJsArray() /*-{
    return new Array();
  }-*/;

  public static native JsArrayString createJsArrayString() /*-{
    return new Array();
  }-*/;

  public static native void removeJsArrayElement(JsArray<JavaScriptObject> array, int index) /*-{
     array.splice(index,1);
  }-*/;

  public static JsArrayString toJsArray(Iterable<ParticipantId> participants) {

    JsArrayString array = createJsArrayString();
    for (ParticipantId p : participants)
      array.push(p.getAddress());

    return array;
  }

  public static native JavaScriptObject toJs(String s) /*-{
     return s;
  }-*/;


  public static String nextBase64(int length) {
    StringBuilder result = new StringBuilder(length);
    int bits = 0;
    int bitCount = 0;
    while (result.length() < length) {
      if (bitCount < 6) {
        bits = Random.nextInt();
        bitCount = 32;
      }
      result.append(WEB64_ALPHABET[bits & 0x3F]);
      bits >>= 6;
      bitCount -= 6;
    }
    return result.toString();
  }

}
