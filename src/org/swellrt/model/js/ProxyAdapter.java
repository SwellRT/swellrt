package org.swellrt.model.js;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.JsArrayString;

import org.swellrt.model.generic.BooleanType;
import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.Model;
import org.swellrt.model.generic.NumberType;
import org.swellrt.model.generic.StringType;
import org.swellrt.model.generic.Type;
import org.waveprotocol.wave.model.wave.ParticipantId;

import java.util.Set;

/**
 *
 * Adapt SwellRT objects from/to JavaScript proxy objects.
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class ProxyAdapter {


  /**
   * Converts a Java iterable of strings to a Javascript array.
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

  /**
   * Converts a Java set of ParticipantId objects to a Javascript array of
   * strings.
   *
   * @param participants
   * @return
   */
  private static JsArrayString participantsToArray(Set<ParticipantId> participants) {
    JsArrayString array = (JsArrayString) JavaScriptObject.createArray();
    for (ParticipantId p : participants)
      array.push(p.getAddress());

    return array;
  }


  private native static boolean isJsArray(JavaScriptObject jso) /*-{
    return jso != null && (Array.isArray(jso));
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

  private native static boolean isJsFunction(JavaScriptObject jso) /*-{
    return jso != null && (typeof jso == "function");
  }-*/;

  private native static String asString(JavaScriptObject jso) /*-{
    return ""+jso;
  }-*/;

  private native static Integer asInteger(JavaScriptObject value) /*-{
    var x;
    if (isNaN(value)) {
      return null;
    }
    x = parseFloat(value);
    if ((x | 0) === x) {
      return value;
    }

    return null;
  }-*/;

  private native static Double asDouble(JavaScriptObject value) /*-{
    return value;
  }-*/;

  private native static boolean asBoolean(JavaScriptObject value) /*-{
    return value;
  }-*/;

  private native static JsArrayMixed asArray(JavaScriptObject jso) /*-{
    return jso;
  }-*/;

  private native static void log(String m, JavaScriptObject obj) /*-{
    if (!$wnd._traces) {
      $wnd._traces = new Array();
    }
    $wnd._traces.push({ trace: m, data: obj });
    console.log(m);
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

      // Using the string representation of the number to avoid
      // issues converting JS number to Java number with toString() methods
      t = model.createNumber(asString(value));

    } else if (isJsString(value)) {
      t = model.createString(asString(value));

    } else if (isJsBoolean(value)) {
      t = model.createBoolean(asString(value));

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


  /**
   * Sets an event handler in a node of the collaborative object. Handler must
   * be a function.
   *
   * @param handler
   * @param target
   * @return the listener
   */
  protected ProxyListener setListener(Type target, JavaScriptObject handler) {

    if (!isJsFunction(handler)) return null;


    if (target instanceof MapType) {
      ProxyMapListener listener = ProxyMapListener.create(handler);
      listener.setAdapter(this);
      ((MapType) target).addListener(listener);

      return listener;

    } else if (target instanceof ListType) {
      ProxyListListener listener = ProxyListListener.create(handler);
      listener.setAdapter(this);
      ((ListType) target).addListener(listener);

      return listener;

    } else if (target instanceof StringType) {
      ProxyPrimitiveListener listener = ProxyPrimitiveListener.create(handler);
      listener.setAdapter(this);
      ((StringType) target).addListener(listener);

      return listener;
    }

    return null;
  }

  /**
   * Remove an event handler from a node of the collaborative object.
   *
   * TODO verify that this implementation works
   *
   * @param target
   * @param handler
   */
  protected boolean removeListener(Type target, ProxyListener handler) {

    if (handler instanceof ProxyMapListener) {
      ((MapType) target).removeListener((ProxyMapListener) handler);

    } else if (handler instanceof ProxyListListener) {
      ((ListType) target).removeListener((ProxyListListener) handler);

    } else if (handler instanceof ProxyPrimitiveListener) {
      ((StringType) target).removeListener((ProxyPrimitiveListener) handler);

    }

    return true;

  }


  /**
   * Creates a Javascript object proxing a collaborative object. The create
   * object allows a native JS syntax to work with the collab. object.
   *
   * It also provide syntax sugar to inner properties by path: <br>
   *
   * "object[listprop.3.field]" is equivalent to "object.listprop[3].field"
   *
   *
   * <br>
   * but the first expression is more efficient.
   *
   * @param delegate
   * @param root
   * @return
   */
  public native JavaScriptObject getJSObject(Model delegate, MapType root) /*-{

    var _this = this;

    var target = this.@org.swellrt.model.js.ProxyAdapter::of(Lorg/swellrt/model/generic/Type;)(root);
    target._object = delegate;


    var proxy = new $wnd.Proxy(target, {

           get: function(target, propKey) {

             if (typeof propKey == "string" && propKey.indexOf(".") > 0) {

               var value = target._object.@org.swellrt.model.generic.Model::fromPath(Ljava/lang/String;)(propKey);
               if (value) {
                 return _this.@org.swellrt.model.js.ProxyAdapter::of(Lorg/swellrt/model/generic/Type;)(value);
               }

             } else if (propKey == "addCollaborator") {

                return function(c) {
                  target._object.@org.swellrt.model.generic.Model::addParticipant(Ljava/lang/String;)(c);
                };

             } else if (propKey == "removeCollaborator") {

                return function(c) {
                  target._object.@org.swellrt.model.generic.Model::removeParticipant(Ljava/lang/String;)(c);
                };

             } else if (propKey == "collaborators") {

                var collaboratorSet = target._object.@org.swellrt.model.generic.Model::getParticipants()();
                return @org.swellrt.model.js.ProxyAdapter::participantsToArray(Ljava/util/Set;)(collaboratorSet);

             } else if (propKey == "_nodes") {

                //
                // For debug purposes: list of Wavelet documents storing collaborative object's nodes
                //

                var parts = target._object.@org.swellrt.model.generic.Model::getModelDocuments()();
                return @org.swellrt.model.js.ProxyAdapter::iterableToArray(Ljava/lang/Iterable;)(parts);

             } else if (propKey == "_node") {

                //
                // For debug purposes: return a Wavelet document storing a collaborative object's node
                //

                return function(node) {
                  return target._object.@org.swellrt.model.generic.Model::getModelDocument(Ljava/lang/String;)(node);
                };

             } else if (propKey == "_oid") {

                // TODO make _id property read-only
                return target._object.@org.swellrt.model.generic.Model::getId()();

             } else {
                return target[propKey];
             }
           }

    });

    return proxy;

  }-*/;


  public JavaScriptObject of(Type delegate) {

    if (delegate instanceof MapType)
      return ofMap((MapType) delegate);

    if (delegate instanceof ListType)
      return ofList((ListType) delegate);

    if (delegate instanceof StringType)
      return ofString((StringType) delegate);

    if (delegate instanceof NumberType)
      return ofNumber((NumberType) delegate);

    if (delegate instanceof BooleanType)
      return ofBoolean((BooleanType) delegate);

    return null;
  }

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


        if (propKey == 'addListener' || propKey == 'on' || propKey == 'onEvent') {

          return function(listener, property) {

            if (!listener)
              return false;

            var eventTarget = target._delegate;

            if (property != null && typeof property == 'string') {
              eventTarget = target._delegate.@org.swellrt.model.generic.MapType::get(Ljava/lang/String;)(propKey);

              if (!eventTarget)
                return false;
            }

            var proxyListener = _this.@org.swellrt.model.js.ProxyAdapter::setListener(Lorg/swellrt/model/generic/Type;Lcom/google/gwt/core/client/JavaScriptObject;)(eventTarget, listener);

            // Return an object which can remove the listener
            return {
              dispose: function() {
                _this.@org.swellrt.model.js.ProxyAdapter::removeListener(Lorg/swellrt/model/generic/Type;Lorg/swellrt/model/js/ProxyListener;)(eventTarget, proxyListener);
              }

            };

          };


        } else if (propKey == '_object') {

          // bypass the _object property
          return target[propKey];

         } else {

          var value = target._delegate.@org.swellrt.model.generic.MapType::get(Ljava/lang/String;)(propKey);
          if (!value)
            return undefined;

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

        // bypass a special property _object
        if (propKey == '_object') {
          return target[propKey] = value;
        }

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

       } else if (propKey == 'addListener' || propKey == 'on' || propKey == 'onEvent') {

          return function(listener, index) {

            if (!listener)
              return false;

            var eventTarget = target._delegate;

            index = Number(index);
            if (index >=0 && index < length)
              eventTarget = target._delegate.@org.swellrt.model.generic.ListType::get(I)(index);

            if (!eventTarget)
              return false;

            var proxyListener = _this.@org.swellrt.model.js.ProxyAdapter::setListener(Lorg/swellrt/model/generic/Type;Lcom/google/gwt/core/client/JavaScriptObject;)(eventTarget, listener);

            // Return an object which can remove the listener
            return {
              dispose: function() {
                _this.@org.swellrt.model.js.ProxyAdapter::removeListener(Lorg/swellrt/model/generic/Type;Lorg/swellrt/model/js/ProxyListener;)(eventTarget, proxyListener);
              }

            };

          };

        }

      },


      set: function(target, propKey, value, receiver) {

         var length = target._delegate.@org.swellrt.model.generic.ListType::size()();
         var index = Number(propKey);

         // Should check here index out of bounds?
         // Collaborative list doesn't support inserting out of bounds
         if (index >=0 && index <= length) {

           if (value === undefined || value === null) {

              var deletedValue = target._delegate.@org.swellrt.model.generic.ListType::remove(I)(index);

              if (deletedValue)
                return _this.@org.swellrt.model.js.ProxyAdapter::of(Lorg/swellrt/model/generic/Type;)(deletedValue);
              else
                return false;

           } else {
             return _this.@org.swellrt.model.js.ProxyAdapter::add(Lorg/swellrt/model/generic/ListType;ILcom/google/gwt/core/client/JavaScriptObject;)(target._delegate, propKey, value);
           }

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
      },


      deleteProperty: function(target, propKey) {

        var length = target._delegate.@org.swellrt.model.generic.ListType::size()();
        var index = Number(propKey);

        if (index >=0 && index < length) {

          var deletedValue = target._delegate.@org.swellrt.model.generic.ListType::remove(I)(index);

          if (deletedValue)
            return _this.@org.swellrt.model.js.ProxyAdapter::of(Lorg/swellrt/model/generic/Type;)(deletedValue);
          else
            return false;

        } else {
          return Reflect.deleteProperty(target, propKey);
        }

      }

    });

    return proxy;

  }-*/;


  public native  JavaScriptObject ofString(StringType delegate) /*-{
    return delegate.@org.swellrt.model.generic.StringType::getValue()();
  }-*/;


  public native  JavaScriptObject ofNumber(NumberType delegate) /*-{
    var value = delegate.@org.swellrt.model.generic.NumberType::getValueDouble()();
    if (value != null) {
      return value.@java.lang.Double::doubleValue()();
    }

    return null;
  }-*/;

  public native  JavaScriptObject ofBoolean(BooleanType delegate) /*-{
    return delegate.@org.swellrt.model.generic.BooleanType::getValue()();
  }-*/;

  public final native JavaScriptObject ofParticipant(ParticipantId participant) /*-{
    return participant.@org.waveprotocol.wave.model.wave.ParticipantId::getAddress()()
  }-*/;

  public final native JavaScriptObject ofPrimitive(String value) /*-{
    return value;
  }-*/;

}
