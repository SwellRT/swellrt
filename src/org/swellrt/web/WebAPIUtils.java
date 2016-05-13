package org.swellrt.web;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Random;

/**
 * A compilation of utility method used in the SwellRT Web API
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class WebAPIUtils {


  static final char[] WEB64_ALPHABET =
      "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_".toCharArray();

  public static String getRandomBase64(int length) {

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

  public static native JavaScriptObject createCallbackError(String errorCode) /*-{
    var error = new Object();
    error.code = errorCode;
    return error;
   }-*/;



  public static native JavaScriptObject createCallbackSuccess(Object object) /*-{
    return object;
   }-*/;

  public static native JavaScriptObject createCallbackSuccess(Object a, Object b) /*-{
    return {  first: a, second: b };
  }-*/;



}
