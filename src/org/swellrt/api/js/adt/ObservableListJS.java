package org.swellrt.api.js.adt;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.swellrt.api.SwellRTUtils;
import org.waveprotocol.wave.model.adt.ObservableElementList;



@SuppressWarnings("rawtypes")
public class ObservableListJS extends JavaScriptObject implements ObservableElementList.Listener {



  public native static ObservableListJS create(ObservableElementList<?, ?> delegate, AdapterJS adapterJS) /*-{

  var jso = {

       callbackMap: new Object(),

       eventHandlers: new Object(),

       registerEventHandler: function(event, handler) {
        this.eventHandlers[event] = handler;
       },

       unregisterEventHandler: function(event, handler) {
        this.eventHandlers[event] = null;
       },

       remove: function(index) {
         @org.swellrt.api.js.adt.ObservableListJS::remove(Lorg/waveprotocol/wave/model/adt/ObservableElementList;I)(delegate, index);
       },

       add: function(initstate) {
          var _initstate = adapterJS.@org.swellrt.api.js.adt.AdapterJS::initFromJS(Lcom/google/gwt/core/client/JavaScriptObject;)(initstate);
          delegate.@org.waveprotocol.wave.model.adt.ObservableElementList::add(Ljava/lang/Object;)(_initstate);
       }

    }; // jso


    var _values = delegate.@org.waveprotocol.wave.model.adt.ObservableElementList::getValues()();
    jso.values = @org.swellrt.api.js.adt.ObservableListJS::adapt(Ljava/lang/Iterable;Lorg/swellrt/api/js/adt/AdapterJS;)(_values, adapterJS);

    jso.adapter = adapterJS;

    return jso;

  }-*/;


  protected ObservableListJS() {

  }

  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
    this.eventHandlers[event](parameter);
    }

  }-*/;

  protected static final JsArray<JavaScriptObject> adapt(Iterable<Object> values, AdapterJS adapter) {

    JsArray<JavaScriptObject> array = SwellRTUtils.createJsArray();

    for (Object o : values)
      array.push(adapter.adaptToJS(o));

    return array;
  }

  protected final native JsArray<JavaScriptObject> getArray() /*-{
    return this.values;
  }-*/;

  protected final native AdapterJS getAdapter() /*-{
    return this.adapter;
  }-*/;

  @SuppressWarnings("unchecked")
  protected final static void remove(ObservableElementList list, int index) {
    if (index >= 0 && index < list.size()) {
      Object o = list.get(index);
      list.remove(o);
    }
  }

  @Override
  public final void onValueAdded(Object entry) {

    JsArray<JavaScriptObject> array = getArray();
    AdapterJS adapter = getAdapter();

    // Mutation of the JS array
    JavaScriptObject entryJS = adapter.adaptToJS(entry);
    array.push(entryJS);

    // Fire event to registered handlers
    fireEvent("ITEM_ADDED", entryJS);
  }


  @Override
  public final void onValueRemoved(Object entry) {

    // Mutation of the JS array
    JavaScriptObject removedObject = getAdapter().adaptToJS(entry);
    for (int i = 0; i < getArray().length(); i++) {
      if (getArray().get(i).equals(removedObject)) {
        SwellRTUtils.removeJsArrayElement(getArray(), i);
        break;
      }
    }

    // Fire event to registered handlers
    fireEvent("ITEM_REMOVED", removedObject);
  }




}
