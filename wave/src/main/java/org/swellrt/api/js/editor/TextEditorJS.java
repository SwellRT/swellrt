package org.swellrt.api.js.editor;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.api.WaveClient;
import org.swellrt.client.editor.TextEditor;


public class TextEditorJS extends JavaScriptObject {

  public native static TextEditorJS create(TextEditor delegate, WaveClient client) /*-{

    var jso = {

      onSelectionChanged: function(handler) {
        var _handler = @org.swellrt.api.js.editor.TextEditorJSListener::create(Lcom/google/gwt/core/client/JavaScriptObject;)(handler);
        delegate.@org.swellrt.client.editor.TextEditor::setListener(Lorg/swellrt/client/editor/TextEditorListener;)(_handler);
      },

      edit: function(text) {

        // TODO check for cleanUp();

        var _text = text.getDelegate();
        client.@org.swellrt.api.WaveClient::configureTextEditor(Lorg/swellrt/client/editor/TextEditor;Lorg/swellrt/model/generic/TextType;)(delegate, _text);
        delegate.@org.swellrt.client.editor.TextEditor::edit(Lorg/swellrt/model/generic/TextType;)(_text);
      },

      cleanUp: function() {
        delegate.@org.swellrt.client.editor.TextEditor::cleanUp()();
        return this;
      },

      setEditing: function(editing) {
        delegate.@org.swellrt.client.editor.TextEditor::setEditing(Z)(editing);
        return this;
      },

      toggleDebug: function() {
        delegate.@org.swellrt.client.editor.TextEditor::toggleDebug()();
      },

      addWidget: function(name, state) {
        return delegate.@org.swellrt.client.editor.TextEditor::addWidget(Ljava/lang/String;Ljava/lang/String;)(name,state);
      },
      
      getWidget: function(element) {
      	return delegate.@org.swellrt.client.editor.TextEditor::getWidget(Lcom/google/gwt/dom/client/Element;)(element);
      },

      setAnnotation: function(name, value) {
         delegate.@org.swellrt.client.editor.TextEditor::setAnnotation(Ljava/lang/String;Ljava/lang/String;)(name, value);
      },

      getSelection: function() {
        return delegate.@org.swellrt.client.editor.TextEditor::getSelection()();
      },
      
      getAnnotationSet: function(name) {
      	return delegate.@org.swellrt.client.editor.TextEditor::getAnnotationSet(Ljava/lang/String;)(name);
      },
      
      getAnnotation: function(range, key) {
      	return delegate.@org.swellrt.client.editor.TextEditor::getAnnotation(Lorg/waveprotocol/wave/client/doodad/annotation/jso/JsoEditorRange;Ljava/lang/String;)(range, key);      
      },

      clearAnnotation: function(a, b) {
      
      	 if (a && b && typeof a == 'string' && typeof b == 'object') {      	       	  
		  
		  	// a -> annotation key
		    // b -> editor range
		    delegate.@org.swellrt.client.editor.TextEditor::clearAnnotation(Lorg/waveprotocol/wave/client/doodad/annotation/jso/JsoEditorRange;Ljava/lang/String;)(b, a);			  

      	 } else if (a && typeof a == 'string') {

  	  		// a -> annotation key
  	  		delegate.@org.swellrt.client.editor.TextEditor::clearAnnotation(Ljava/lang/String;)(a);
  	  		      	  		
      	  } else if (a && typeof a == 'object') {
      	  	
      	  	// a -> editor range
			delegate.@org.swellrt.client.editor.TextEditor::clearAnnotation(Lorg/waveprotocol/wave/client/doodad/annotation/jso/JsoEditorRange;)(a);      	  	
      	 }
      
      },

    }; 

    return jso;

  }-*/;


  protected TextEditorJS() {

  }

}
