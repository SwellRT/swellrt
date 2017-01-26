package org.swellrt.beta.client.js.editor.annotation;

import com.google.gwt.user.client.Event;

import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt.Editor", name = "AnnotationEventHandler", isNative = true)
public class AnnotationEventHandler {

  public native void onMutation(AnnotationInstance annotation);
  
  public native void onAdded(AnnotationInstance annotation);
  
  public native void onRemoved(AnnotationInstance annotation);
  
  public native void onEvent(Event e, AnnotationInstance annotation);
  
}
