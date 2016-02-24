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
          return delegate.@org.swellrt.model.generic.FileType::getValue()();
        },

        getUrl: function() {
          return @org.swellrt.api.SwellRTUtils::buildAttachmentUrl(Lorg/swellrt/model/generic/FileType;)(this._delegate);
        },

        setValue: function(file) {
          if (!file || !file.type || file.type() != "FileType")
          return false;

          delegate.@org.swellrt.model.generic.FileType::setValue(Lorg/waveprotocol/wave/media/model/AttachmentId;)(file.getValue());
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
