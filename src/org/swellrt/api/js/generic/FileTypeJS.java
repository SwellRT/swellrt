package org.swellrt.api.js.generic;


import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

import org.swellrt.api.SwellRTUtils;
import org.swellrt.api.js.WaveClientJS;
import org.swellrt.model.generic.FileType;
import org.waveprotocol.wave.media.model.AttachmentId;


public class FileTypeJS extends JavaScriptObject implements FileType.Listener {



  public native static FileTypeJS create(FileType delegate) /*-{

      var jso = {

        _delegate: delegate,

        callbackMap: new Object(),

        eventHandlers: new Object(),

        registerEventHandler: function(event, handler) {
          this.eventHandlers[event] = handler;
        },

        getDelegate: function() {
          return this._delegate;
        },

        unregisterEventHandler: function(event, handler) {
          this.eventHandlers[event] = null;
        },

        getValue: function() {
          return this.value();
        },

        value: function() {
          return delegate.@org.swellrt.model.generic.FileType::getValue()();
        },

        fileId: function() {
          return delegate.@org.swellrt.model.generic.FileType::getFileId()();
        },

        contentType: function() {
          return delegate.@org.swellrt.model.generic.FileType::getContentType()();
        },

        url: function() {
          return @org.swellrt.api.SwellRTUtils::buildAttachmentUrl(Lorg/swellrt/model/generic/FileType;)(this._delegate);
        },

        getUrl: function() {
          return this.url();
        },

        setValue: function(file) {
          this.set(file);
        },

        set: function(file) {
          if (file && file.type && file.type() == "FileType") {
            this.clear();
            delegate.@org.swellrt.model.generic.FileType::setValue(Lorg/waveprotocol/wave/media/model/AttachmentId;Ljava/lang/String;)(file.value(), file.contentType());
            }
        },

        clearValue: function() {
          this.clear();
        },

        clear: function() {

          var url = this.url();

          delegate.@org.swellrt.model.generic.FileType::clearValue()();

          var request = new XMLHttpRequest();

          request.onload = function(event) {
            if (request.status == 200) {
             console.log("Attachment delete from server");
            } else {
             console.log("Error, attachment not delete from server");
            }
          };


          request.open("DELETE", url);
          @org.swellrt.api.SwellRTUtils::addCommonRequestHeaders(Lcom/google/gwt/core/client/JavaScriptObject;)(request);
          request.send();

        },

        type: function() {
          return delegate.@org.swellrt.model.generic.FileType::getType()();
        }

      }; // jso

      return jso;

  }-*/;


  protected FileTypeJS() {

  }

  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      if (!parameter.constructor === Array){
        this.eventHandlers[event](parameter);
      } else {
      this.eventHandlers[event].apply(this,parameter);
      }
    }

  }-*/;

  private final native FileType getDelegate() /*-{
    return this._delegate;
  }-*/;




  @Override
  public final void onValueChanged(AttachmentId oldValue, AttachmentId newValue) {
    JsArrayString values = SwellRTUtils.createJsArrayString();
    values.push(newValue.serialise());
    values.push(oldValue.serialise());
    fireEvent(WaveClientJS.ITEM_CHANGED, values);
  }


}
