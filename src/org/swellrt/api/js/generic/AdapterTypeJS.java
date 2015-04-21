package org.swellrt.api.js.generic;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.model.generic.ListType;
import org.swellrt.model.generic.MapType;
import org.swellrt.model.generic.StringType;
import org.swellrt.model.generic.Type;

public class AdapterTypeJS {


  public static JavaScriptObject adapt(Type instance) {


    if (instance instanceof StringType) {

      StringType str = (StringType) instance;
      StringTypeJS strJs = StringTypeJS.create(str);
      str.addListener(strJs);

      return strJs;

    } else if (instance instanceof MapType) {
      MapType map = (MapType) instance;
      MapTypeJS mapJs = MapTypeJS.create(map);
      map.addListener(mapJs);

      return mapJs;

    } else if (instance instanceof ListType) {
      ListType list = (ListType) instance;
      ListTypeJS listJs = ListTypeJS.create(list);
      list.addListener(listJs);

      return listJs;
    }

    return null;
  }

}
