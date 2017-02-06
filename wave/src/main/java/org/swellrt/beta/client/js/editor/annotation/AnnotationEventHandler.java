package org.swellrt.beta.client.js.editor.annotation;

import org.waveprotocol.wave.client.editor.content.ContentElement;
import org.waveprotocol.wave.model.document.util.Range;

import com.google.gwt.user.client.Event;

import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt.Editor", name = "AnnotationEventHandler", isNative = true)
public class AnnotationEventHandler {

  public native void onMutation(AnnotationInstance annotation);
  
  public native void onAdded(AnnotationInstance annotation);
  
  public native void onRemoved(AnnotationInstance annotation);
  
  public native void onEvent(Event e, AnnotationInstance annotation);
  
}
