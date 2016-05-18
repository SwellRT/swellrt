package org.swellrt.model.js;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.JsArrayString;

import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.StringType;
import org.swellrt.model.generic.Type;

/**
 *
 * Adapt SwellRT objects from/to JavaScript proxy objects.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ProxyAdapter {


  /**
   * Covnerts a Java iterable of strings to a Javascript array.
   *
   * @param strings@org.swellrt.model.js.ProxyAdapter
   * @return
   */
  private static JsArrayString iterableToArray(Iterable<String> strings) {
    JsArrayString array = (JsArrayString) JavaScriptObject.createArray();
    for (String s : strings)
      array.push(s);

    return array;
  }


  private native static boolean isJsArray(JavaScriptObject jso) /*-{
    return jso != null && ((typeof jso == "object") && (jso.constructor == Array));
  }-*/;

  private native static boolean isJsObject(JavaScriptObject jso) /*-{
    return jso != null && ((typeof jso == "object"));
  }-*/;

  private native static boolean isJsNumber(JavaScriptObject jso) /*-{
    return jso != null && ((typeof jso == "number"));
  }-*/;

  private native static boolean isJsBoolean(JavaScriptObject jso) /*-{
    return jso != null && ((typeof jso == "boolean"));
  }-*/;

  private native static boolean isJsString(JavaScriptObject jso) /*-{
    return jso != null && ((typeof jso == "string"));
  }-*/;

  private native static boolean isJsFile(JavaScriptObject jso) /*-{
    return jso != null && ((typeof jso == "object") && (jso.constructor == File));
  }-*/;

  private native static boolean isJsText(JavaScriptObject jso) /*-{
    return jso != null && ((typeof jso == "object") && (jso.constructor == Text));
  }-*/;

  private native static String asString(JavaScriptObject jso) /*-{
    return jso;
  }-*/;

  private native static JsArrayMixed asArray(JavaScriptObject jso) /*-{
    return jso;
  }-*/;


  private final Model model;

  public ProxyAdapter(Model model) {
    this.model = model;
  }

  /**
   * Generate a {@link Type} instance for a Javascript value. The new object is
   * NOT attached to a collaborative object.
   *
   * @param value
   * @return
   */
  protected Type fromJs(JavaScriptObject value) {

    Type t = null;

    if (isJsNumber(value)) {
      t = model.createString(asString(value));

    } else if (isJsString(value)) {
      t = model.createString(asString(value));

    } else if (isJsBoolean(value)) {
      t = model.createString(asString(value));

    } else if (isJsArray(value)) {
      t = model.createList();

    } else if (isJsText(value)) {
      t = model.createText();

    } else if (isJsFile(value)) {
      t = model.createList();

    } else if (isJsObject(value)) {
      t = model.createMap();

    }

    return t;
  }

  /**
   * Populate the content of a native Javascript object into its counterpart of
   * the collaborative object. Hence, types of both 'tObject' and 'jsObject'
   * arguments must be similar.
   *
   * If they are primitive values, values are not populated.
   *
   *
   * @param tObject
   * @param jsObject
   * @return
   */
  protected boolean populateValues(Type tObject, JavaScriptObject jsObject) {

    if (isJsNumber(jsObject)) {
      // Nothing to do
      return true;

    } else if (isJsString(jsObject)) {
      // Nothing to do
      return true;

    } else if (isJsBoolean(jsObject)) {
      // Nothing to do
      return true;

    } else if (isJsArray(jsObject)) {

      JsArrayMixed jsArray = asArray(jsObject);
      for (int i = 0; i < jsArray.length(); i++) {
        if (!add((ListType) tObject, jsArray.getObject(i))) {
          return false;
        }
      }


    } else if (isJsText(jsObject)) {
      // TODO add support for Text objects
      return true;

    } else if (isJsFile(jsObject)) {
      // TODO add support for File objects
      return true;

    } else if (isJsObject(jsObject)) {

      JsMap jsMap = JsMap.of(jsObject);

      JsArrayString keys = jsMap.keys();
      for (int i = 0; i < keys.length(); i++) {
        String key = keys.get(i);
        if (!put((MapType) tObject, key, jsMap.get(key))) {
          return false;
        }
      }

    }

    return false;
  }


  /**
   * Put a Javascript object into a {@MapType} instance. This can
   * trigger a recursive process to attach a new subtree of Javascript objects
   * into the collaborative object.
   *
   * @param map
   * @param key
   * @param value
   */
  protected boolean put(MapType map, String key, JavaScriptObject value) {

    Type tvalue = fromJs(value);
    if (tvalue == null) return false;

    tvalue = map.put(key, tvalue);

    return populateValues(tvalue, value);
  }


  /**
   * Add a Javascript object into a {@ListType} instance. This can
   * trigger a recursive process to attach a new subtree of Javascript objects
   * into the collaborative object.
   *
   * @param list
   * @param value
   * @return
   */
  protected boolean add(ListType list, JavaScriptObject value) {

    Type tvalue = fromJs(value);
    if (tvalue == null) return false;

    tvalue = list.add(tvalue);

    return populateValues(tvalue, value);
  }



  public JavaScriptObject of(Type delegate) {

    if (delegate instanceof MapType)
      return ofMap((MapType) delegate);

    if (delegate instanceof ListType)
      return ofList((ListType) delegate);

    if (delegate instanceof StringType)
      return ofString((StringType) delegate);


    return null;
  }


  public native JavaScriptObject getJSObject(Model delegate, MapType root) /*-{

    // Set the root map as default trap
    var target = this.@org.swellrt.model.js.ProxyAdapter::of(Lorg/swellrt/model/generic/Type;)(root);

    var proxy = new $wnd.Proxy(target, {

            get: function(target, propKey) {
                 return target[propKey];
            }

    });

    return proxy;

  }-*/;

  /**
   * Generate a JavaScript proxy object for an underlying collaborative map
   *
   * @param delegate
   * @return
   */
  public native JavaScriptObject ofMap(MapType delegate) /*-{

    var _this = this;
    var proxy = new $wnd.Proxy(
    {
      _delegate: delegate
    },
    {

      get: function(target, propKey, receiver) {

        var value = target._delegate.@org.swellrt.model.generic.MapType::get(Ljava/lang/String;)(propKey);

        if (!value) {
          return undefined;
        } else {
          var proxy = _this.@org.swellrt.model.js.ProxyAdapter::of(Lorg/swellrt/model/generic/Type;)(value);
          return proxy;
        }

      },

      has: function(target, propKey) {
        return target._delegate.@org.swellrt.model.generic.MapType::hasKey(Ljava/lang/String;)(propKey);
      },

      ownKeys: function(target) {
        var nativeKeys = target._delegate.@org.swellrt.model.generic.MapType::keySet()();
        var keys = @org.swellrt.model.js.ProxyAdapter::iterableToArray(Ljava/lang/Iterable;)(nativeKeys);
        return keys;
      },

      getOwnPropertyDescriptor: function(target, propKey) {

        var hasPropKey = target._delegate.@org.swellrt.model.generic.MapType::hasKey(Ljava/lang/String;)(propKey);

        if (hasPropKey) {
          var descriptor = {
            value: this.get(target, propKey),
            writable: true,
            enumerable: true,
            configurable: true
          };

          return descriptor;
        } else {
          return Reflect.getOwnPropertyDescriptor(target, propKey);
        }
      },

      set: function(target, propKey, value, receiver) {
        console.log("SET "+propKey+ " = "+value);
        return _this.@org.swellrt.model.js.ProxyAdapter::put(Lorg/swellrt/model/generic/MapType;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(target._delegate, propKey, value);
      },


      defineProperty: function(target, propKey, propDesc) {
        console.log("DEF PROP "+propKey);

        var value = propDesc.value;
        if (!value)
          return false;

        return _this.@org.swellrt.model.js.ProxyAdapter::put(Lorg/swellrt/model/generic/MapType;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(target._delegate, propKey, value);
      },

      deleteProperty: function(target, propKey) {
        target._delegate.@org.swellrt.model.generic.MapType::remove(Ljava/lang/String;)(propKey);
        var hasPropKey = target._delegate.@org.swellrt.model.generic.MapType::hasKey(Ljava/lang/String;)(propKey);
        return !hasPropKey;
      }



    });

    return proxy;

  }-*/;




  public native JavaScriptObject ofList(ListType delegate) /*-{

    var _this = this;
    var proxy = new $wnd.Proxy([],{

     get: function(t, n) {

        var vtype = delegate.@org.swellrt.model.generic.ListType::get(I)(n);

        if (!vtype)
          return undefined;

        var vproxy = _this.@org.swellrt.model.js.ProxyAdapter::of(Lorg/swellrt/model/generic/Type;)(vtype);
        return vproxy;

      },

      set: function(t, n, v) {
         console.log("set "+n+ " = "+v);
      }

    });

    return proxy;

  }-*/;


  public native  JavaScriptObject ofString(StringType delegate) /*-{

    return delegate.@org.swellrt.model.generic.StringType::getValue()();

  }-*/;

}
