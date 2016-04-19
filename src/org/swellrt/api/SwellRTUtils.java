package org.swellrt.api;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.Random;

import org.swellrt.api.js.generic.AdapterTypeJS;
import org.swellrt.model.generic.FileType;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.Type;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;
import org.waveprotocol.wave.model.waveref.WaveRef;
import org.waveprotocol.wave.util.escapers.GwtWaverefEncoder;

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

  // public static native void addField(JavaScriptObject object, String name,
  // Object value) /*-{
  // object[name] = value;
  // }-*/;

  public static native void addField(JavaScriptObject object, String name, int value) /*-{
    object[name] = value;
  }-*/;

  public static native void addField(JavaScriptObject object, String name, Long value) /*-{
    object[name] = value;
  }-*/;

  public static native void addField(JavaScriptObject object, String name, String value) /*-{
    object[name] = value;
  }-*/;

  public static native void addField(JavaScriptObject object, String name, boolean value) /*-{
    object[name] = value;
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

  /**
   * Get the URL with only protocol and server parts of the server. The URL
   * doesn't end with /
   *
   * @return the base URL of the SwellRT server
   */
  public static String getBaseUrl() {
    String u = GWT.getModuleBaseURL();

    int last = -1;
    for (int i = 0; i < 3; i++) {
      last = u.indexOf("/", last + 1);
    }

    if (last != -1)
      u = u.substring(0, last);

    return u;
  }

  public static native String getSessionId() /*-{
    return $wnd.__session.sessionid;
  }-*/;

  /**
   * Return the window id generated when SwellRT is loaded in a browser's
   * window. See {@link WaveClient#onReady()}
   *
   * @return
   */
  public static native String getWindowId() /*-{
    try {
      return $wnd.sessionStorage.getItem("x-swellrt-window-id");
    } catch (e) {
      return null;
    }
  }-*/;

  /**
   * A factory of {@link RequestBuilder} objects tthat performs common
   * initializations.
   *
   * @param method
   * @param url
   * @return
   */
  public static RequestBuilder newRequestBuilder(Method method, String url) {
    RequestBuilder rb = new RequestBuilder(method, url);
    rb.setIncludeCredentials(true);
    String windowId = getWindowId();
    if (windowId != null) rb.setHeader("X-window-id", windowId);
    return rb;
  }

  /**
   * A factory of XMLHttpRequest objects that performs common initializations.
   *
   * @return
   */
  public static native JavaScriptObject newXMLHttpRequest() /*-{

    var request = new XMLHttpRequest();
    request.withCredentials = true;

    try {
      request.setRequestHeader("X-window.id", $wnd.sessionStorage.getItem("x-swellrt-window-id") );
    } catch (e) {

    }

    return request;
  }-*/;

  public static String buildAttachmentUploadUrl(String simpleAttachmentId) {
    return getBaseUrl() + "/attachment/" + simpleAttachmentId + getSessionURLparameter();
  }

  public static String getSessionURLparameter() {
    if (Cookies.getCookie(SwellRT.SESSION_COOKIE_NAME) == null) {
      return ";" + SwellRT.SESSION_PATH_PARAM + "=" + getSessionId();
    }
    return "";
  }

  public static String buildAttachmentUrl(FileType file) {
    Preconditions.checkArgument(file != null, "File can't be null");
    Preconditions.checkArgument(file.getValue() != null, "File content can't be null");
    Preconditions.checkArgument(file.getValue().getId() != null, "File id can't be null");
    Preconditions.checkArgument(file.getModel() != null, "File is not in a model");


    return getBaseUrl() + "/attachment/" + file.getValue().getId() + getSessionURLparameter()
        + "?waveRef=" + encodeWaveRefUri(file.getModel().getWaveRef());
  }

  public static String encodeWaveRefUri(WaveRef waveRef) {
    return URL.encode(GwtWaverefEncoder.encodeToUriQueryString(waveRef));
  }

  public static String encodeWaveRefUri(Model model) {
    return encodeWaveRefUri(model.getWaveRef());
  }

}
