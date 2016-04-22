package org.swellrt.api.js.generic;

import com.google.gwt.core.client.JavaScriptObject;

import org.swellrt.api.js.WaveClientJS;
import org.swellrt.model.generic.Model;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * A JavaScript Wrapper for the CommunityWavelet class
 *
 * @author pablojan@gmail.com
 *
 */
public class ModelJS extends JavaScriptObject implements Model.Listener {


  //private final static AdapterJS itemAdapterJS = new ItemAdapterJS();


  public native static ModelJS create(Model delegate) /*-{

    var jso = {

     callbackMap: new Object(),

     eventHandlers: new Object(),

     registerEventHandler: function(event, handler) {
      this.eventHandlers[event] = handler;
     },

     unregisterEventHandler: function(event, handler) {
      this.eventHandlers[event] = null;
     },

     id: function() {
       return delegate.@org.swellrt.model.generic.Model::getId()();
     },


     getParticipants: function() {
      var _participants = delegate.@org.swellrt.model.generic.Model::getParticipants()();
      return @org.swellrt.api.SwellRTUtils::participantIterableToJs(Ljava/lang/Iterable;)(_participants);
     },

     addParticipant: function(address) {
      return delegate.@org.swellrt.model.generic.Model::addParticipant(Ljava/lang/String;)(address);
     },

     removeParticipant: function(address) {
      return delegate.@org.swellrt.model.generic.Model::removeParticipant(Ljava/lang/String;)(address);
     },

     createMap: function() {
       var _map = delegate.@org.swellrt.model.generic.Model::createMap()();
       return @org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_map);
     },

     createString: function(strValue) {
       var _str = delegate.@org.swellrt.model.generic.Model::createString(Ljava/lang/String;)(strValue);
       return @org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_str);
     },

     createList: function() {
       var _list = delegate.@org.swellrt.model.generic.Model::createList()();
       return @org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_list);
     },

     createText: function(textOrXml) {
       var _txt = delegate.@org.swellrt.model.generic.Model::createText(Ljava/lang/String;)(textOrXml);
       return @org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_txt);
     },

     createFile: function(file, onComplete, progressListener) {


        if (!file instanceof File) {
          throw "INVALID_ARGUMENT";
        }

        if (typeof onComplete !== "function") {
          throw "INVALID_ARGUMENT";
        }

        if (progressListener && typeof progressListener !== "function") {
          throw "INVALID_ARGUMENT";
        }


        var attachmentId = delegate.@org.swellrt.model.generic.Model::generateAttachmentId()();
        var id = attachmentId.@org.waveprotocol.wave.media.model.AttachmentId::getId()();

        var waveRef = @org.swellrt.api.SwellRTUtils::encodeWaveRefUri(Lorg/swellrt/model/generic/Model;)(delegate);
        var url = @org.swellrt.api.SwellRTUtils::buildAttachmentUploadUrl(Ljava/lang/String;)(id);

        var formData = new FormData();

        formData.append("attachmentId", id);
        formData.append("waveRef", waveRef);
        formData.append("uploadFormElement", file, file.name);

        var request = new XMLHttpRequest();

        request.onload = function(event) {
          if (request.status == 201) {
            var _file = delegate.@org.swellrt.model.generic.Model::createFile(Lorg/waveprotocol/wave/media/model/AttachmentId;)(attachmentId);
            onComplete(@org.swellrt.api.js.generic.AdapterTypeJS::adapt(Lorg/swellrt/model/generic/Type;)(_file));
          } else {
            onComplete({ error : "SERVICE_EXCEPTION" });
          }
        };

        if (progressListener) {
          request.addEventListener("progress", progressListener);
        }

        request.open("POST", url);
        request.setRequestHeader("Accept", "text/plain");
        @org.swellrt.api.SwellRTUtils::addCommonRequestHeaders(Lcom/google/gwt/core/client/JavaScriptObject;)(request);
        request.send(formData);

        return true;
     },

     // For debug purpose only
     getModelDocuments: function() {
       var _docs = delegate.@org.swellrt.model.generic.Model::getModelDocuments()();
       return @org.swellrt.api.SwellRTUtils::stringIterableToJs(Ljava/lang/Iterable;)(_docs);
     },

     getModelDocument: function(docId) {
       return delegate.@org.swellrt.model.generic.Model::getModelDocument(Ljava/lang/String;)(docId);
     }

    }; // jso

    // Initialize the JS root map
    jso.root = @org.swellrt.api.js.generic.ModelJS::createRootJs(Lorg/swellrt/model/generic/Model;)(delegate);

    return jso;

  }-*/;


  protected ModelJS() {

  }


  private final static MapTypeJS createRootJs(Model delegate) {

    MapTypeJS mapJs = MapTypeJS.create(delegate.getRoot());
    delegate.getRoot().addListener(mapJs);

    return mapJs;
  }


  private final native void fireEvent(String event, Object parameter) /*-{

    if (this.eventHandlers[event] != null) {
      this.eventHandlers[event](parameter);
    }

  }-*/;


  @Override
  public final void onAddParticipant(ParticipantId participant) {
    fireEvent(WaveClientJS.PARTICIPANT_ADDED, participant.getAddress());
  }


  @Override
  public final void onRemoveParticipant(ParticipantId participant) {
    fireEvent(WaveClientJS.PARTICIPANT_REMOVED, participant.getAddress());
  }


}
