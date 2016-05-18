package org.swellrt.model.js;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

/**
 * A wrapper class to access Javascript objects properties.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class JsMap extends JavaScriptObject {

  public static native  JsMap of(JavaScriptObject jso) /*-{
    return jso;
  }-*/;

  protected JsMap() {

  }

  public final native JsArrayString keys() /*-{
    return Object.keys(this);
  }-*/;


  public final native JavaScriptObject get(String key) /*-{
    return this[key];
  }-*/;


}
