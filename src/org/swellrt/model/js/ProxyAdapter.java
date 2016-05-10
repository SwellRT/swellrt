package org.swellrt.model.js;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.api.js.generic.ModelJS;
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


  private Model model;

  public ProxyAdapter(Model model) {
    this.model = model;
  }

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


  public native JavaScriptObject of(Object delegate) /*-{
    return undefined;
  }-*/;


  public native JavaScriptObject of(ModelJS delegate) /*-{


  }-*/;


  public native JavaScriptObject of(MapType delegate) /*-{

    var proxy = new $wnd.Proxy({},{

      get: function(t, n) {

        var vtype = delegate.@org.swellrt.model.generic.MapType::get(Ljava/lang/String;)(n);

        if (!vtype)
          return undefined;

        var vproxy = this.@org.swellrt.model.js.ProxyAdapter::of(Lorg/swellrt/model/generic/Type;)(vtype);
        return vproxy;

      },

      set: function(t, n, v) {

         // delegate.put(n, fromJs(v));

      }


    });

    return proxy;

  }-*/;




  public native JavaScriptObject of(ListType delegate) /*-{

    let proxy = new $wnd.Proxy([],{



    });

    return proxy;

  }-*/;


  public native  JavaScriptObject of(StringType delegate) /*-{

    return delegate.getValue();

  }-*/;

}
