package org.swellrt.api.js.generic;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.swellrt.api.SwellRTUtils;
import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.Type;


public class ListTypeJS extends JavaScriptObject implements ListType.Listener {



  public native static ListTypeJS create(ListType delegate) /*-{

    var jso = {

        _delegate: delegate,

        values: new Array(),

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

        add: function(value, index) {

           var _value = value.getDelegate();

           if ((index !== undefined) && (typeof index == "number"))
              _value = delegate.@org.swellrt.model.generic.ListType::add(ILorg/swellrt/model/generic/Type;)(index, _value);
           else
             _value = delegate.@org.swellrt.model.generic.ListType::add(Lorg/swellrt/model/generic/Type;)(_value);

           return @org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_value);
        },

        remove: function(index) {
           var _value = delegate.@org.swellrt.model.generic.ListType::remove(I)(index);
           return @org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_value);
        },

        get: function(index) {
           var _value = delegate.@org.swellrt.model.generic.ListType::get(I)(index);
           return @org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_value);
        },

        size: function() {
           return delegate.@org.swellrt.model.generic.ListType::size()();
        },

        type: function() {
          return delegate.@org.swellrt.model.generic.ListType::getType()();
        }

    }; // jso

    // Populate the JavaScript array of values
    var _values = delegate.@org.swellrt.model.generic.ListType::getValues()();
    if (_values != null) // Prevent errors in unattached delegates
      jso.values = @org.swellrt.api.SwellRTUtils::typeIterableToJs(Ljava/lang/Iterable;)(_values);


    return jso;

    }-*/;


  protected ListTypeJS() {

  }

  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;

  private final native ListType getDelegate() /*-{
    return this._delegate;
  }-*/;

  private final native JsArray<JavaScriptObject> getValues() /*-{
    return this.values;
  }-*/;



  @Override
  public final void onValueAdded(Type value) {

    ListType delegate = getDelegate();
    int index = delegate.indexOf(value);

    // Update the JS array
    JavaScriptObject newValue = AdapterTypeJS.adapt(value);

    JsArray<JavaScriptObject> values = getValues();
    values.set(index, newValue);

    // Fire JS Event
    fireEvent("ITEM_ADDED", newValue);
  }


  @Override
  public final void onValueRemoved(Type value) {

    JsArray<JavaScriptObject> values = getValues();
    int removedIndex = -1;

    for (int i = 0; i < values.length(); i++) {

      Type delegate = SwellRTUtils.getDelegate(values.get(i));
      if (value.equals(delegate)) {
        removedIndex = i;
        break;
      }
    }

    SwellRTUtils.removeJsArrayElement(values, removedIndex);

    // Fire JS Event
    fireEvent("ITEM_REMOVED", removedIndex);
  }




}
