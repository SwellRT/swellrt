package org.swellrt.api.js.editor;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.client.editor.TextEditor;


public class TextEditorJS extends JavaScriptObject {

  public native static TextEditorJS create(TextEditor delegate) /*-{

    var jsWrapper = {

      _delegate: delegate,

      setElement: function(elementId) {
        delegate.@org.swellrt.client.editor.TextEditor::setElement(Ljava/lang/String;)(elementId);
        return this;
      },

      edit: function(text) {
        var textDelegate = text.getDelegate();
        delegate.@org.swellrt.client.editor.TextEditor::edit(Lorg/swellrt/model/generic/TextType;)(textDelegate);
      },

      cleanUp: function() {
        delegate.@org.swellrt.client.editor.TextEditor::cleanUp()();
        return this;
      }


    }; // jsWrapper

    return jsWrapper;

  }-*/;


  protected TextEditorJS() {

  }

}
