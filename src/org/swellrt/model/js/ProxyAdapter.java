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
   * Add a Javascript object into a {@ListType} instance in the
   * specified index. This can trigger a recursive process to attach a new
   * subtree of Javascript objects into the collaborative object.
   *
   * Collaborative list semantics differs from javascript's, provided index must
   * be in the bounds of the list.
   *
   * @param list
   * @param value
   * @return
   */
  protected boolean add(ListType list, int index, JavaScriptObject value) {

    Type tvalue = fromJs(value);
    if (tvalue == null) return false;

    tvalue = list.add(index, tvalue);

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
        return _this.@org.swellrt.model.js.ProxyAdapter::put(Lorg/swellrt/model/generic/MapType;Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(target._delegate, propKey, value);
      },


      defineProperty: function(target, propKey, propDesc) {
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
    var _array = new Array();
    _array._delegate = delegate;

    var proxy = new $wnd.Proxy(_array,{

     get: function(target, propKey, receiver) {

        var length = target._delegate.@org.swellrt.model.generic.ListType::size()();
        var index = Number(propKey);


        if (index >=0 && index < length) {

          //
          // get
          //

          var value = target._delegate.@org.swellrt.model.generic.ListType::get(I)(index);

          if (!value) {
            return undefined;
          } else {
            return _this.@org.swellrt.model.js.ProxyAdapter::of(Lorg/swellrt/model/generic/Type;)(value);
          }


        } else if (propKey == "length") {

          //
          // length
          //

          return target._delegate.@org.swellrt.model.generic.ListType::size()();

        } else if (propKey == "push") {

          //
          // push
          //

          return function() {

            for(var i in arguments) {
              _this.@org.swellrt.model.js.ProxyAdapter::add(Lorg/swellrt/model/generic/ListType;Lcom/google/gwt/core/client/JavaScriptObject;)(target._delegate, arguments[i]);
            }
            return target._delegate.@org.swellrt.model.generic.ListType::size()();
          }


       } else if (propKey == "pop") {

           //
           // pop
           //

          return function() {

            var length = target._delegate.@org.swellrt.model.generic.ListType::size()();

            if (length > 0) {

              var value = target._delegate.@org.swellrt.model.generic.ListType::get(I)(length-1);
              var proxy = null;
              if (!value) {
                return undefined;
              } else {
                 proxy = _this.@org.swellrt.model.js.ProxyAdapter::of(Lorg/swellrt/model/generic/Type;)(value);
              }

              target._delegate.@org.swellrt.model.generic.ListType::remove(I)(length-1);

              return proxy;
            }
          }
       }


      },


      set: function(target, propKey, value, receiver) {

         var length = target._delegate.@org.swellrt.model.generic.ListType::size()();
         var index = Number(propKey);

         // Should check here index out of bounds?
         // Collaborative list doesn't support inserting out of bounds
         if (index >=0 && index <= length) {
           return _this.@org.swellrt.model.js.ProxyAdapter::add(Lorg/swellrt/model/generic/ListType;ILcom/google/gwt/core/client/JavaScriptObject;)(target._delegate, propKey, value);
         } else {
           // Should reflect non array properties set?
           return Reflect.set(target, propKey, value);
         }


      },


      has: function(target, propKey) {

        var length = target._delegate.@org.swellrt.model.generic.ListType::size()();
        var index = Number(propKey);

        if (index >=0 && index < length)
          return true;
        else if (propKey === 'length')
          return true;
        else Reflect.has(target, propKey);

      },

      ownKeys: function(target) {

        // keys is just a contiguos list of indexes, because collaborative lists doesn't allow gaps on indexes

        var length = target._delegate.@org.swellrt.model.generic.ListType::size()();
        var keys = new Array();

        for (var i = 0; i < length; i++)
          keys.push(""+i);

        keys.push("length");

        return keys;
      },

      getOwnPropertyDescriptor: function(target, propKey) {

        var length = target._delegate.@org.swellrt.model.generic.ListType::size()();
        var index = Number(propKey);

        if (index >=0 && index < length) {

          var descriptor = {
            value: this.get(target, propKey),
            writable: true,
            enumerable: true,
            configurable: true
          };

          return descriptor;

        } else if (propKey == 'length') {

          return {
            value: this.get(target, 'length'),
            writable: true,
            enumerable: false,
            configurable: false
          };

        } else {
          return Reflect.getOwnPropertyDescriptor(target, propKey);
        }
      }

    });

    return proxy;

  }-*/;


  public native  JavaScriptObject ofString(StringType delegate) /*-{

    return delegate.@org.swellrt.model.generic.StringType::getValue()();

  }-*/;

}
