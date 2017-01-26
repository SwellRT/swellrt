package org.swellrt.beta.client.js.editor.annotation;

import org.waveprotocol.wave.model.document.util.Range;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Event;

import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt.Editor", name = "AnnotationEventHandler", isNative = true)
public class AnnotationEventHandler {

  public native void onMutation(Element node, String value, Range range);
  
  public native void onAdded(Element node, String value, Range range);
  
  public native void onRemoved(Element node, String value, Range range);
  
  public native void onEvent(Event e, Element node, String value, Range range);
  
}
