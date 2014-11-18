package org.waveprotocol.mod.wavejs.js.generic;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.waveprotocol.mod.model.generic.MapType;
import org.waveprotocol.mod.model.generic.Type;
import org.waveprotocol.mod.wavejs.WaveJSUtils;


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

         var _value = delegate.@org.waveprotocol.mod.model.generic.MapType::get(Ljava/lang/String;)(key);
         if (_value == null)
             return undefined;

         return @org.waveprotocol.mod.wavejs.js.generic.AdapterTypeJS::adapt(Lorg/waveprotocol/mod/model/generic/Type;)(_value);
       },

       put: function(key, value) {

          // Direct String creation
          if (typeof value === "string")  {

            var _value = delegate.@org.waveprotocol.mod.model.generic.MapType::put(Ljava/lang/String;Ljava/lang/String;)(key, value);
            if (_value == null)
              return undefined;

            return @org.waveprotocol.mod.wavejs.js.generic.AdapterTypeJS::adapt(Lorg/waveprotocol/mod/model/generic/Type;)(_value);

          } else {

            var _value = value.getDelegate();

            if (_value === "undefined" || _value == null)
              return undefined;

            _value = delegate.@org.waveprotocol.mod.model.generic.MapType::put(Ljava/lang/String;Lorg/waveprotocol/mod/model/generic/Type;)(key, _value);

            return @org.waveprotocol.mod.wavejs.js.generic.AdapterTypeJS::adapt(Lorg/waveprotocol/mod/model/generic/Type;)(_value);

          }
       },

       keySet: function() {
          var _keyset = delegate.@org.waveprotocol.mod.model.generic.MapType::keySet()();
          return @org.waveprotocol.mod.wavejs.WaveJSUtils::stringIterableToJs(Ljava/lang/Iterable;)(_keyset)
       },

       remove: function(key) {
          delegate.@org.waveprotocol.mod.model.generic.MapType::remove(Ljava/lang/String;)(key);
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

    JsArray<JavaScriptObject> values = WaveJSUtils.createJsArray();

    if (oldValue == null) {

      // Update the JS map
      JavaScriptObject newValueJs = AdapterTypeJS.adapt(newValue);

      WaveJSUtils.addStringToJsArray(values, key);
      values.push(newValueJs);

      // Fire JS event
      fireEvent("ITEM_ADDED", values);

    } else {

      // Update the JS map
      JavaScriptObject oldValueJs = AdapterTypeJS.adapt(oldValue);
      JavaScriptObject newValueJs = AdapterTypeJS.adapt(newValue);

      WaveJSUtils.addStringToJsArray(values, key);
      values.push(newValueJs);
      values.push(oldValueJs);

      // Fire JS event
      fireEvent("ITEM_CHANGED", values);

    }
  }


  @Override
  public final void onValueRemoved(String key, Type value) {

    JsArray<JavaScriptObject> values = WaveJSUtils.createJsArray();
    // Update the JS map
    JavaScriptObject oldValue = AdapterTypeJS.adapt(value);
    WaveJSUtils.addStringToJsArray(values, key);
    values.push(oldValue);
    // Fire JS Event
    fireEvent("ITEM_REMOVED", values);
  }




}
