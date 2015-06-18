package org.swellrt.api.js.generic;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import org.swellrt.api.SwellRTUtils;
import org.swellrt.model.generic.TextType;
import org.waveprotocol.wave.model.document.AnnotationInterval;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;


public class TextTypeJS extends JavaScriptObject implements TextType.Listener {



  public native static TextTypeJS create(TextType delegate) /*-{

  var jsWrapper = {

       _delegate: delegate,

       getDelegate: function() {
         return this._delegate;
       },

       type: function() {
          return delegate.@org.swellrt.model.generic.TextType::getType()();
       },

       insert: function(location, text) {
         delegate.@org.swellrt.model.generic.TextType::insertText(ILjava/lang/String;)(location, text);
       },

       newLine: function(location) {
         delegate.@org.swellrt.model.generic.TextType::insertNewLine(I)(location);
       },

       remove: function(start, end) {
          delegate.@org.swellrt.model.generic.TextType::deleteText(II)(start, end);
       },

       size: function() {
          return delegate.@org.swellrt.model.generic.TextType::getSize()();
       },

       xml: function() {
          return delegate.@org.swellrt.model.generic.TextType::getXml()();
       },

       setAnnotation: function(start, end, key, value) {
          delegate.@org.swellrt.model.generic.TextType::setAnnotation(IILjava/lang/String;Ljava/lang/String;)(start, end, key, value);
       },

       getAnnotation: function(location, key) {
          if (key == null)
            return null;
          return delegate.@org.swellrt.model.generic.TextType::getAnnotation(ILjava/lang/String;)(location, key);
       },

       getAllAnnotations: function(start, end) {
          return @org.swellrt.api.js.generic.TextTypeJS::getAllAnnotations(Lorg/swellrt/model/generic/TextType;II)(delegate, start, end);
       }

    }; // jsWrapper


    return jsWrapper;

  }-*/;


  protected TextTypeJS() {

  }

  /**
   * A convinient proxy method for creating AnnotationJS with simple syntax.
   *
   * @param start
   * @param end
   * @return
   */
  protected final static JsArray<AnnotationJS> getAllAnnotations(TextType textType, int start,
      int end) {


    Iterable<AnnotationInterval<String>> iterableAnnotations =
        textType.getAllAnnotations(start, end);

    @SuppressWarnings("unchecked")
    final JsArray<AnnotationJS> annotations =
        (JsArray<AnnotationJS>) SwellRTUtils.createTypedJsArray();

    for (AnnotationInterval<String> interval : iterableAnnotations) {
      final AnnotationInterval<String> theInterval = interval;
      theInterval.annotations().each(new ProcV<String>() {

        @Override
        public void apply(String key, String value) {
          annotations.push(AnnotationJS.create(theInterval.start(), theInterval.end(), key, value));
        }

      });
    }

    return annotations;
  }

}
