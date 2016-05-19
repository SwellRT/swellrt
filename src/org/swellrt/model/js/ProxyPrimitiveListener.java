package org.swellrt.model.js;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.model.generic.StringType.Listener;

public class ProxyPrimitiveListener extends ProxyListener implements Listener {

  public static final String ON_VALUE_CHANGED = "onValueChanged";

  public static native ProxyPrimitiveListener create(JavaScriptObject jsListener) /*-{
    return jsListener;
  }-*/;

  protected ProxyPrimitiveListener() {

  }

  @Override
  public final void onValueChanged(String oldValue, String newValue) {
    trigger(ON_VALUE_CHANGED, getAdapter().ofPrimitive(oldValue), getAdapter().ofPrimitive(newValue));
  }



}
