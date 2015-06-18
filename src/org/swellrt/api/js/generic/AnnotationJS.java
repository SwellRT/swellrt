package org.swellrt.api.js.generic;

import com.google.gwt.core.client.JavaScriptObject;


public class AnnotationJS extends JavaScriptObject {


  public native static AnnotationJS create(int _start, int _end, String _key, String _value) /*-{

    var annotation = {
      start: _start,
      end: _end,
      key: _key,
      value: _value
    }; // annotation

    return annotation;

  }-*/;

  protected AnnotationJS() {

  }


}
