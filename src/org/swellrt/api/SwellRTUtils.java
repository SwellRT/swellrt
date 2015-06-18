package org.swellrt.api;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.Random;

import org.swellrt.api.js.generic.AdapterTypeJS;
import org.swellrt.model.generic.Type;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Utility methods for working with JavaScript
 *
 * @author pablojan@gmail.com
 *
 */
public class SwellRTUtils {


  static final char[] WEB64_ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"
      .toCharArray();

  public static native JsArray<? extends JavaScriptObject> createTypedJsArray() /*-{
  return new Array();
}-*/;

  public static native JsArray<JavaScriptObject> createJsArray() /*-{
    return new Array();
  }-*/;

  public static native JsArrayString createJsArrayString() /*-{
    return new Array();
  }-*/;

  public static native void removeJsArrayElement(JsArray<JavaScriptObject> array, int index) /*-{
     array.splice(index,1);
  }-*/;


  public static native Type getDelegate(JavaScriptObject jso) /*-{
    return jso._delegate;
  }-*/;

  public static JsArray<JavaScriptObject> typeIterableToJs(Iterable<Type> values) {

    JsArray<JavaScriptObject> jsArray = SwellRTUtils.createJsArray();

    for (Type v : values) {
      jsArray.push(AdapterTypeJS.adapt(v));
    }

    return jsArray;

  }


  public static void addStringToJsArray(JsArray<JavaScriptObject> array, String s) {
    JsArrayString strArray = array.<JsArrayString> cast();
    strArray.push(s);
  }


  public static JsArrayString stringIterableToJs(Iterable<String> strings) {
    JsArrayString array = createJsArrayString();
    for (String s : strings)
      array.push(s);

    return array;
  }

  public static JsArrayString participantIterableToJs(Iterable<ParticipantId> participants) {

    JsArrayString array = createJsArrayString();
    for (ParticipantId p : participants)
      array.push(p.getAddress());

    return array;
  }

  @Deprecated
  public static JsArrayString toJsArray(Iterable<ParticipantId> participants) {

    JsArrayString array = createJsArrayString();
    for (ParticipantId p : participants)
      array.push(p.getAddress());

    return array;
  }

  /*
   * public static JavaScriptObject toJs(String s) { return
   * JsonUtils.safeEval(s); }
   */



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
