package org.swellrt.model.js;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.model.generic.ListType.Listener;
import org.swellrt.model.generic.Type;

public class ProxyListListener extends ProxyListener implements Listener {

  public static final String ON_VALUE_ADDED = "onValueAdded";
  public static final String ON_VALUE_REMOVED = "onValueRemoved";

  public static native ProxyListListener create(JavaScriptObject jsListener) /*-{
    return jsListener;
  }-*/;

  protected ProxyListListener() {

  }

  @Override
  public final void onValueAdded(Type entry) {
    trigger(ON_VALUE_ADDED, getAdapter().of(entry));
  }

  @Override
  public final void onValueRemoved(Type entry) {
    trigger(ON_VALUE_REMOVED, getAdapter().of(entry));
  }

}
