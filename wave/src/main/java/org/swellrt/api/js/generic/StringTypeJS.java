package org.swellrt.api.js.generic;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import org.swellrt.api.SwellRTUtils;
import org.swellrt.api.js.WaveClientJS;
import org.swellrt.model.generic.StringType;


public class StringTypeJS extends JavaScriptObject implements StringType.Listener {



  public native static StringTypeJS create(StringType delegate) /*-{

      var jso = {

        _delegate: delegate,

        callbackMap: new Object(),

        eventHandlers: new Object(),

        registerEventHandler: function(event, handler) {
          this.eventHandlers[event] = handler;
        },

        getDelegate: function() {
          return this._delegate;
        },

        unregisterEventHandler: function(event, handler) {
          this.eventHandlers[event] = null;
        },

        getValue: function() {
          return this.get();
        },

        get: function() {
          return delegate.@org.swellrt.model.generic.StringType::getValue()();
        },

        value: function() {
          return this.get();
        },

        setValue: function(value) {
           this.set(value);
        },

        set: function(value) {
          delegate.@org.swellrt.model.generic.StringType::setValue(Ljava/lang/String;)(value);
        },

        type: function() {
          return delegate.@org.swellrt.model.generic.StringType::getType()();
        }

      }; // jso

      return jso;

  }-*/;


  protected StringTypeJS() {

  }

  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      if (!parameter.constructor === Array){
        this.eventHandlers[event](parameter);
      } else {
      this.eventHandlers[event].apply(this,parameter);
      }
    }

  }-*/;

  private final native StringType getDelegate() /*-{
    return this._delegate;
  }-*/;




  @Override
  public final void onValueChanged(String oldValue, String newValue) {
    JsArrayString values = SwellRTUtils.createJsArrayString();
    values.push(newValue);
    values.push(oldValue);
    fireEvent(WaveClientJS.ITEM_CHANGED, values);
  }


}
