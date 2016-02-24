package org.swellrt.api.js.generic;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.swellrt.api.SwellRTUtils;
import org.swellrt.api.js.WaveClientJS;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.Type;


public class MapTypeJS extends JavaScriptObject implements MapType.Listener {



  public native static MapTypeJS create(MapType delegate) /*-{

                                                          var jso = {

                                                          _delegate: delegate,

                                                          callbackMap: new Object(),

                                                          eventHandlers: new Object(),

                                                          registerEventHandler: function(event, handler) {
                                                          this.eventHandlers[event] = handler;
                                                          },

                                                          unregisterEventHandler: function(event, handler) {
                                                          this.eventHandlers[event] = null;
                                                          },

                                                          getDelegate: function() {
                                                          return this._delegate;
                                                          },

                                                          get: function(key) {

                                                          var _value = delegate.@org.swellrt.model.generic.MapType::get(Ljava/lang/String;)(key);
                                                          if (_value == null)
                                                          return undefined;

                                                          return @org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_value);
                                                          },

                                                          put: function(key, value) {

                                                          // Direct String creation
                                                          if (typeof value === "string")  {

                                                          var _value = delegate.@org.swellrt.model.generic.MapType::put(Ljava/lang/String;Ljava/lang/String;)(key, value);
                                                          if (_value == null)
                                                          return undefined;

                                                          return @org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_value);

                                                          } else {

                                                          var _value = value.getDelegate();

                                                          if (_value === "undefined" || _value == null)
                                                          return undefined;

                                                          _value = delegate.@org.swellrt.model.generic.MapType::put(Ljava/lang/String;Lorg/swellrt/model/generic/Type;)(key, _value);

                                                          return @org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_value);

                                                          }
                                                          },
                                                          

                                                          keySet: function() {
                                                          var _keyset = delegate.@org.swellrt.model.generic.MapType::keySet()();
                                                          return @org.swellrt.api.SwellRTUtils::stringIterableToJs(Ljava/lang/Iterable;)(_keyset);
                                                          },

                                                          remove: function(key) {
                                                          delegate.@org.swellrt.model.generic.MapType::remove(Ljava/lang/String;)(key);
                                                          },

                                                          type: function() {
                                                          return delegate.@org.swellrt.model.generic.MapType::getType()();
                                                          }


                                                          }; // jso


                                                          return jso;

                                                          }-*/;


  protected MapTypeJS() {

  }

  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;

  private final native MapType getDelegate() /*-{
    return this._delegate;
  }-*/;




  @Override
  public final void onValueChanged(String key, Type oldValue, Type newValue) {

    JsArray<JavaScriptObject> values = SwellRTUtils.createJsArray();

    if (oldValue == null) {

      // Update the JS map
      JavaScriptObject newValueJs = AdapterTypeJS.adapt(newValue);

      SwellRTUtils.addStringToJsArray(values, key);
      values.push(newValueJs);

      // Fire JS event
      fireEvent(WaveClientJS.ITEM_ADDED, values);

    } else {

      // Update the JS map
      JavaScriptObject oldValueJs = AdapterTypeJS.adapt(oldValue);
      JavaScriptObject newValueJs = AdapterTypeJS.adapt(newValue);

      SwellRTUtils.addStringToJsArray(values, key);
      values.push(newValueJs);
      values.push(oldValueJs);

      // Fire JS event
      fireEvent(WaveClientJS.ITEM_CHANGED, values);

    }
  }


  @Override
  public final void onValueRemoved(String key, Type value) {

    JsArray<JavaScriptObject> values = SwellRTUtils.createJsArray();
    // Update the JS map
    JavaScriptObject oldValue = AdapterTypeJS.adapt(value);
    SwellRTUtils.addStringToJsArray(values, key);
    values.push(oldValue);
    // Fire JS Event
    fireEvent(WaveClientJS.ITEM_REMOVED, values);
  }




}
