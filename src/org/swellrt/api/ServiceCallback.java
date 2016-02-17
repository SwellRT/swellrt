package org.swellrt.api;

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


    public static native JavaScriptResponse error(String json) /*-{
      var r;
      try {
        r = JSON.parse(json);
      } catch (e) {};
      if (!r)
        r = { error : json };

      return r;
    }-*/;

    protected JavaScriptResponse() {}


  }


  protected ServiceCallback() {
  }


  public final native void onComplete(JavaScriptResponse response) /*-{
    this(response);
  }-*/;

}
