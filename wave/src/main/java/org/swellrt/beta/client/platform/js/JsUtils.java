package org.swellrt.beta.client.platform.js;

import org.waveprotocol.wave.client.common.util.JsoView;
import org.waveprotocol.wave.client.common.util.OffsetPosition;
import org.waveprotocol.wave.model.document.util.Range;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.IntRange;
import org.waveprotocol.wave.model.util.ReadableStringSet;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;
import org.waveprotocol.wave.model.util.StringSet;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;

public class JsUtils {

  @Deprecated
  public static native boolean isArray(JavaScriptObject o) /*-{
    return Array.isArray(o);
  }-*/;

  @Deprecated
  public static native boolean isString(JavaScriptObject o) /*-{
    return typeof o == 'string';
  }-*/;

  public static native JsArrayString asArray(JavaScriptObject o) /*-{
    if (Array.isArray(o))
      return o;

    return null;
  }-*/;


  public static native String asString(JavaScriptObject o) /*-{
    if (typeof o == 'string')
      return o;

    return null;
  }-*/;

  public static native JavaScriptObject asJso(String s) /*-{
    return s;
  }-*/;

  public static String[] stringSetToArray(ReadableStringSet s) {
    final String[] array = new String[s.countEntries()];


    s.each(new Proc() {

      int c = 0;

      @Override
      public void apply(String element) {
        array[c++] = element;
      }

    });

    return array;
  }

  public static native void addToArray(JavaScriptObject array, Object value) /*-{
      array.push(value);
  }-*/;



  /**
   * Transform a Javascript object (array or string) to
   * a StringSet.
   *
   * @param obj an array or string
   * @return
   */
  public static StringSet toStringSet(JavaScriptObject obj) throws IllegalStateException {

    StringSet set = CollectionUtils.createStringSet();

    // from array
    JsArrayString array = asArray(obj);
    if (array != null) {
      for (int i = 0; i < array.length(); i++)
        set.add(array.get(i));
    }


    // from string
    String s = asString(obj);
    if (s!=null)
      set.add(s);

    return set;
  }

  public static Range nativeToRange(JavaScriptObject jso) {
    if (jso == null)
      return null;

    JsoView jsv = JsoView.as(jso);
    return Range.create(new Double(jsv.getNumber("start")).intValue(),
        new Double(jsv.getNumber("end")).intValue());
  }

  public static JavaScriptObject rangeToNative(Range range) {
    JsoView jsv = JsoView.create();
    jsv.setNumber("start", range.getStart());
    jsv.setNumber("end", range.getEnd());
    return jsv.cast();
  }

  public static JavaScriptObject intRangeToNative(IntRange range) {
    JsoView jsv = JsoView.create();
    jsv.setNumber("start", range.getFirst());
    jsv.setNumber("end", range.getSecond());
    return jsv.cast();
  }

  public static JavaScriptObject offsetPositionToNative(OffsetPosition op) {
    JsoView jsv = JsoView.create();
    jsv.setNumber("left", op.left);
    jsv.setNumber("top", op.top);
    jsv.setObject("offsetParent", op.offsetParent);
    return jsv.cast();
  }

  public static OffsetPosition nativeToOffsetPosition(JavaScriptObject jso) {
    JsoView jsv = JsoView.as(jso);
    int left = new Double(jsv.getNumber("left")).intValue();
    int top = new Double(jsv.getNumber("top")).intValue();
    Element offsetParent = jsv.getObjectUnsafe("offsetParent");
    return new OffsetPosition(left, top, offsetParent);
  }
}
