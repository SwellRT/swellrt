package org.swellrt.api.js.generic;


import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.model.generic.TextType;


public class TextTypeJS extends JavaScriptObject implements TextType.Listener {



  public native static TextTypeJS create(TextType delegate) /*-{

  var jsWrapper = {

       _delegate: delegate,

       getDelegate: function() {
         return delegate;
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
       }

    }; // jsWrapper


    return jsWrapper;

  }-*/;


  protected TextTypeJS() {

  }




}
