package org.swellrt.api;

import org.swellrt.api.js.generic.ModelJS;

import com.google.gwt.core.client.JavaScriptObject;

public class ServiceCallback extends JavaScriptObject {

  public static class JavaScriptResponse extends JavaScriptObject {

    public static native JavaScriptResponse error(String code, String cause) /*-{
      var r = new Object();
      r.error = code;
      r.cause = cause;
      return r;
    }-*/;

    public static native JavaScriptResponse success(String json) /*-{
      var r = new Object();
      try {
        r.data = JSON.parse(json);
      } catch (e) {};
      if (!r.data)
        r.data = json;
      return r;
    }-*/;

    public static native JavaScriptResponse success(ModelJS cObject) /*-{
      return cObject;
    }-*/;

    public static native JavaScriptResponse error(String json) /*-{
      var r;
      try {
        r = JSON.parse(json);
      } catch (e) {};
      if (!r)
        r = { error : json };

      return r;
    }-*/;

    public final native String getValue(String field) /*-{
      return this.data[field];
    }-*/;

    protected JavaScriptResponse() {}


  }


  protected ServiceCallback() {
  }


  public final native void onComplete(JavaScriptResponse response) /*-{
    this(response);
  }-*/;




}
