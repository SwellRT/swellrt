package org.swellrt.api.js.editor;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.api.WaveClient;
import org.swellrt.client.editor.TextEditor;


public class TextEditorJS extends JavaScriptObject {

  public native static TextEditorJS create(TextEditor delegate, WaveClient client) /*-{

    var jsWrapper = {

      setElement: function(elementId) {
        delegate.@org.swellrt.client.editor.TextEditor::setElement(Ljava/lang/String;)(elementId);
        return this;
      },

      edit: function(text) {
        var _text = text.getDelegate();
        client.@org.swellrt.api.WaveClient::configureTextEditor(Lorg/swellrt/client/editor/TextEditor;Lorg/swellrt/model/generic/TextType;)(delegate, _text);
        delegate.@org.swellrt.client.editor.TextEditor::edit(Lorg/swellrt/model/generic/TextType;)(_text);
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
