package org.swellrt.beta.model.js;

import org.swellrt.beta.model.SNode;
import org.swellrt.beta.model.SNodeAccessControl;
import org.swellrt.beta.model.SPrimitive;
import org.waveprotocol.wave.model.util.Preconditions;

import com.google.gwt.core.client.JavaScriptObject;

public class SNodeJs {

  public interface Func {
    public void apply(String key, Object value);
  }

  protected static native boolean isObject(Object jso) /*-{
    return typeof jso == "object";
  }-*/;

  protected static native boolean isArray(Object jso) /*-{
    return Array.isArray(jso);
  }-*/;

  protected static native boolean isPrimitive(Object jso) /*-{

    if (typeof jso == "number" ||
    typeof jso == "boolean" ||
    typeof jso == "string") {

    return true;

    }

    return false;

   }-*/;


  protected static native boolean isNumber(Object jso) /*-{
    return typeof jso == "number";
   }-*/;


  protected static native boolean isBoolean(Object jso) /*-{
    return typeof jso == "boolean";
   }-*/;

  protected static native boolean isString(Object jso) /*-{
    return typeof jso == "string";
  }-*/;


  protected static native double asNumber(Object jso) /*-{
    return jso;
  }-*/;


  protected static native boolean asBoolean(Object jso) /*-{
    return jso;
  }-*/;

  protected static native String asString(Object jso) /*-{
    return jso;
  }-*/;


  static native void iterateObject(JavaScriptObject jso, Func f) /*-{

   for (var property in jso) {
     if (jso.hasOwnProperty(property)) {
       f.@org.swellrt.beta.model.js.SNodeJs.Func::apply(Ljava/lang/String;Ljava/lang/Object;)(property, jso[property]);
     }
   }

   }-*/;

  static native void iterateArray(JavaScriptObject jso, Func f) /*-{

    for (var i = 0; i < jso.length; i++) {
      f.@org.swellrt.beta.model.js.SNodeJs.Func::apply(Ljava/lang/String;Ljava/lang/Object;)(i+"", jso[i]);
    }

  }-*/;

  static native Object getArrayElement(JavaScriptObject jso, int index) /*-{
    return jso[index];
  }-*/;

  static native int arrayLength(JavaScriptObject jso) /*-{
    return jso.length;
  }-*/;




  public static SNode castToSNode(Object object) {

    Preconditions.checkArgument(object != null, "Null argument");

    if (isNumber(object)) {

      return new SPrimitive(asNumber(object), new SNodeAccessControl());

    } else if (isBoolean(object)) {

      return new SPrimitive(asBoolean(object), new SNodeAccessControl());

    } else if (isString(object)) {

      return new SPrimitive(asString(object), new SNodeAccessControl());

    } else if (isObject(object)) {

      return SMapJs.create((JavaScriptObject) object);

    } else if (isArray(object)) {

      return SListJs.create((JavaScriptObject) object);

    }

    throw new IllegalStateException("Unable to cast object to JS native SNode");
  }

}
