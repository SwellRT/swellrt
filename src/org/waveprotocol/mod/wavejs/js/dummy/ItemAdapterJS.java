package org.waveprotocol.mod.wavejs.js.dummy;

import com.google.gwt.core.client.JavaScriptObject;

import org.waveprotocol.mod.wavejs.js.adt.AdapterJS;
import org.waveprotocol.wave.model.adt.BasicValue;

public class ItemAdapterJS implements AdapterJS {

  @Override
  public JavaScriptObject adaptToJS(Object o) {
    @SuppressWarnings("unchecked")
    BasicValue<String> value = (BasicValue<String>) o;
    return stringToJS(value.get());
  }

  public native final JavaScriptObject stringToJS(String s) /*-{
                                                            return s;
                                                            }-*/;

  @Override
  public native String initFromJS(JavaScriptObject initialState) /*-{
    return initialState.toString();
   }-*/;



}
