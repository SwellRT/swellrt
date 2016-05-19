package org.swellrt.model.js;


import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.model.generic.MapType.Listener;
import org.swellrt.model.generic.Type;

public class ProxyMapListener extends ProxyListener implements Listener {

  public static final String ON_VALUE_ADDED = "onValueAdded";
  public static final String ON_VALUE_UPDATED = "onValueUpdated";
  public static final String ON_VALUE_REMOVED = "onValueRemoved";


  public static native ProxyMapListener create(JavaScriptObject jsListener) /*-{
    return jsListener;
  }-*/;


  protected ProxyMapListener() {

  }

  @Override
  public final void onValueChanged(String key, Type oldValue, Type newValue) {

    if (oldValue == null) {
      trigger(ON_VALUE_ADDED, getAdapter().ofPrimitive(key), getAdapter().of(newValue));

    } else if (newValue == null) {
      trigger(ON_VALUE_REMOVED, getAdapter().ofPrimitive(key), getAdapter().of(oldValue));

    } else {
      // NOTE: getAdapter().of(oldValue)) returns always undefined, because the
      trigger(ON_VALUE_UPDATED, getAdapter().ofPrimitive(key), getAdapter().of(newValue),
          getAdapter().of(oldValue));
    }

  }

  @Override
  public final void onValueRemoved(String key, Type value) {
    trigger(ON_VALUE_REMOVED, getAdapter().ofPrimitive(key), getAdapter().of(value));
  }


}
