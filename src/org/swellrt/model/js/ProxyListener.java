package org.swellrt.model.js;

import com.google.gwt.core.client.JavaScriptObject;

public class ProxyListener extends JavaScriptObject {


  protected ProxyListener() {

  }

  protected final native void setAdapter(ProxyAdapter adapter) /*-{
    this._adapter = adapter;
  }-*/;

  public final native ProxyAdapter getAdapter() /*-{
    return this._adapter;
  }-*/;

  public final native void trigger(String eventName, JavaScriptObject p) /*-{
    this.apply({}, [eventName, p]);
  }-*/;

  public final native void trigger(String eventName, JavaScriptObject p1, JavaScriptObject p2) /*-{
    this.apply({}, [eventName, p1, p2]);
  }-*/;

  public final native void trigger(String eventName, JavaScriptObject p1, JavaScriptObject p2, JavaScriptObject p3) /*-{
    this.apply({}, [eventName, p1, p2, p3]);
  }-*/;
}
