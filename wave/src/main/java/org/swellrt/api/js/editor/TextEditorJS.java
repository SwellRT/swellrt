package org.swellrt.api.js.editor;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.api.BrowserSession;
import org.swellrt.api.WaveClient;
import org.swellrt.client.editor.TextEditor;
import org.swellrt.client.editor.TextEditor.Configurator;
import org.swellrt.model.generic.TextType;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.doodad.selection.SelectionAnnotationHandler.CaretListener;
import org.waveprotocol.wave.client.wave.InteractiveDocument;
import org.waveprotocol.wave.client.wave.WaveDocuments;
import org.waveprotocol.wave.model.id.WaveId;
import org.waveprotocol.wave.model.wave.ParticipantId;


public class TextEditorJS extends JavaScriptObject {
  
  private static class CaretListenerJS implements CaretListener {
    
    private final JavaScriptObject nativeCaretListener;
    
    public CaretListenerJS(JavaScriptObject nativeCaretListener) {
      this.nativeCaretListener = nativeCaretListener;
    }
    
    @Override
    public void onActive(String address, RgbColor color) {      
      onActiveNative(this.nativeCaretListener, address, color.getHexColor());
    }   

    @Override
    public void onExpire(String address) {
      onExpireNative(nativeCaretListener, address);
    }
    
    protected static native void onActiveNative(JavaScriptObject nativeListener, String address, String color) /*-{
    
      if (!nativeListener)
        return;
       
      if (typeof nativeListener.onActive == "function") {
        nativeListener.onActive(address, color);
      }
    
    }-*/;
    
    protected static native void onExpireNative(JavaScriptObject nativeListener, String address) /*-{

      if (!nativeListener)
        return;
       
      if (typeof nativeListener.onExpire == "function") {
        nativeListener.onExpire(address);
      }
    
    }-*/;
    
  }

  public native static TextEditorJS create(TextEditor delegate, WaveClient client) /*-{

    var jso = {

      onSelectionChanged: function(handler) {
        var _handler = @org.swellrt.api.js.editor.TextEditorJSListener::create(Lcom/google/gwt/core/client/JavaScriptObject;)(handler);
        delegate.@org.swellrt.client.editor.TextEditor::setListener(Lorg/swellrt/client/editor/TextEditorListener;)(_handler);
      },

      edit: function(text, caretListener) {
        var _text = text.getDelegate();
        @org.swellrt.api.js.editor.TextEditorJS::edit(Lorg/swellrt/client/editor/TextEditor;Lorg/swellrt/model/generic/TextType;Lorg/swellrt/api/WaveClient;Lcom/google/gwt/core/client/JavaScriptObject;)(delegate, _text, client, caretListener);
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

      setAnnotation: function(key, value) {
         return delegate.@org.swellrt.client.editor.TextEditor::setAnnotation(Ljava/lang/String;Ljava/lang/String;)(key, value);
      },
      
      setAnnotationInRange: function(range, key, value) {
         return delegate.@org.swellrt.client.editor.TextEditor::setAnnotationInRange(Lorg/waveprotocol/wave/client/doodad/annotation/jso/JsoRange;Ljava/lang/String;Ljava/lang/String;)(range, key, value);
      },

      getSelection: function() {
        return delegate.@org.swellrt.client.editor.TextEditor::getSelection()();
      },
      
      getAnnotationSet: function(key) {
      	return delegate.@org.swellrt.client.editor.TextEditor::getAnnotationSet(Ljava/lang/String;)(key);
      },
      
      getAnnotationInRange: function(range, key) {
      	return delegate.@org.swellrt.client.editor.TextEditor::getAnnotationInRange(Lorg/waveprotocol/wave/client/doodad/annotation/jso/JsoRange;Ljava/lang/String;)(range, key);      
      },
      
      clearAnnotation: function(keyPrefix) {
        delegate.@org.swellrt.client.editor.TextEditor::clearAnnotation(Ljava/lang/String;)(keyPrefix);
      },
      
      clearAnnotationInRange: function(range, keyPrefix) {
        delegate.@org.swellrt.client.editor.TextEditor::clearAnnotationInRange(Lorg/waveprotocol/wave/client/doodad/annotation/jso/JsoRange;Ljava/lang/String;)(range, keyPrefix);   
      },
      
      setText: function(range, text) {
        return delegate.@org.swellrt.client.editor.TextEditor::setText(Lorg/waveprotocol/wave/client/doodad/annotation/jso/JsoRange;Ljava/lang/String;)(range, text); 
      },
      
      getText: function(range) {
        return delegate.@org.swellrt.client.editor.TextEditor::getText(Lorg/waveprotocol/wave/client/doodad/annotation/jso/JsoRange;)(range); 
      },
      
      deleteText: function(range) {
         delegate.@org.swellrt.client.editor.TextEditor::deleteText(Lorg/waveprotocol/wave/client/doodad/annotation/jso/JsoRange;)(range); 
      }

    }; 

    return jso;

  }-*/;


  protected TextEditorJS() {

  }
  
  private static void edit(TextEditor editor, TextType text, WaveClient client, JavaScriptObject caretListener) {
    final WaveId waveId = text.getModel().getWaveId();
    
    editor.edit(text, new CaretListenerJS(caretListener),        
        new Configurator() {

      @Override
      public WaveDocuments<? extends InteractiveDocument> getDocumentRegistry() {
         return client.getDocumentRegistry(waveId);
      }

      @Override
      public ProfileManager getProfileManager() {
        return client.getProfileManager();
      }

      @Override
      public String getSessionId() {     
        return BrowserSession.getWindowSessionId();
      }

      @Override
      public ParticipantId getParticipantId() {
        return ParticipantId.ofUnsafe(BrowserSession.getUserAddress());
      }
      
    });
  }

}
