package org.swellrt.model.js;

import com.google.gwt.core.client.JavaScriptObject;

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


  public native Type fromJs(Type parent, JavaScriptObject o) /*-{

    if (!o) return null;


    if (o instanceof Array) {

        // list = createList(parent)

        // foreach (item in o)
        // list.add(fromJs(item))

        // return list;

    }

    if (typeof o === "string") {

        // return createString(parent, o.value);
    }

    if (typeof o === "number") {

        // return createNumber(parent, o.value);

    }

    if (typeof o === "boolean") {

       // return createBoolean(parent, o.value)

    }

    if (typeof o === "object") {

       // map = createMap(parent, o);

       // for (k,v in o)
       // map.put(k, fromJs(parent, v);

       // return map;
    }



  }-*/;


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

            get: function(t, n) {

              if (n == "model") {
                console.log("Model");
               } else {
                 return t[n];
               }
            }

    });

    return proxy;

  }-*/;


  public native JavaScriptObject ofMap(MapType delegate) /*-{

    var _this = this;
    var proxy = new $wnd.Proxy(
    {
      _delegate: delegate
    },
    {

      get: function(t, n) {

        var vtype = t._delegate.@org.swellrt.model.generic.MapType::get(Ljava/lang/String;)(n);

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
