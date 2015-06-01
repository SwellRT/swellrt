package org.swellrt.api.js.generic;


import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.model.generic.TextType;


public class TextTypeJS extends JavaScriptObject implements TextType.Listener {



  public native static TextTypeJS create(TextType delegate) /*-{

  var jsWrapper = {

       _delegate: delegate,

       getDelegate: function() {
         return delegate;
       }

    }; // jsWrapper


    return jsWrapper;

  }-*/;


  protected TextTypeJS() {

  }




}
