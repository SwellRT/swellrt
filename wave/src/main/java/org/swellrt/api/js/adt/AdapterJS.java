package org.swellrt.api.js.adt;

import com.google.gwt.core.client.JavaScriptObject;

public interface AdapterJS {

  JavaScriptObject adaptToJS(Object o);

  Object initFromJS(JavaScriptObject initialState);

}
