package org.waveprotocol.mod.wavejs.js.adt;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.waveprotocol.mod.wavejs.WaveJSUtils;
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
         @org.waveprotocol.mod.wavejs.js.adt.ObservableListJS::remove(Lorg/waveprotocol/wave/model/adt/ObservableElementList;I)(delegate, index);
       },

       add: function(initstate) {
          var _initstate = adapterJS.@org.waveprotocol.mod.wavejs.js.adt.AdapterJS::initFromJS(Lcom/google/gwt/core/client/JavaScriptObject;)(initstate);
          delegate.@org.waveprotocol.wave.model.adt.ObservableElementList::add(Ljava/lang/Object;)(_initstate);
       }

    }; // jso


    var _values = delegate.@org.waveprotocol.wave.model.adt.ObservableElementList::getValues()();
    jso.values = @org.waveprotocol.mod.wavejs.js.adt.ObservableListJS::adapt(Ljava/lang/Iterable;Lorg/waveprotocol/mod/wavejs/js/adt/AdapterJS;)(_values, adapterJS);

    jso.adapter = adapterJS;

    return jso;

  }-*/;


  protected ObservableListJS() {

  }

  protected static final JsArray<JavaScriptObject> adapt(Iterable<Object> values, AdapterJS adapter) {

    JsArray<JavaScriptObject> array = WaveJSUtils.createJsArray();

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
    array.push(adapter.adaptToJS(entry));
  }


  @Override
  public final void onValueRemoved(Object entry) {
    JavaScriptObject removedObject = getAdapter().adaptToJS(entry);
    for (int i = 0; i < getArray().length(); i++) {
      if (getArray().get(i).equals(removedObject)) {
        WaveJSUtils.removeJsArrayElement(getArray(), i);
        break;
      }
    }
  }




}
