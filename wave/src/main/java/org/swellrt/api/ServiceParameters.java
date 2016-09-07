package org.swellrt.api;

import com.google.gwt.core.client.JavaScriptObject;

public class ServiceParameters {

  public static native String toJSON(JavaScriptObject o) /*-{

    return JSON.stringify(o);

  }-*/;
  
}
