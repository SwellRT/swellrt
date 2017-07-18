package org.swellrt.beta.client.js.editor.annotation;

import com.google.gwt.user.client.Event;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "AnnotationEvent")
public class AnnotationEvent {

  @JsFunction
  public interface Handler {

    public boolean exec(AnnotationEvent annotationEvent);
  }

  public static final int EVENT_DOM_ADDED = 1;
  public static final int EVENT_DOM_MUTATED = 2;
  public static final int EVENT_DOM_REMOVED = 3;
  public static final int EVENT_DOM_EVENT = 4;

  public static final int EVENT_CREATED = 5;
  public static final int EVENT_REMOVED = 6;

  public static AnnotationEvent build(int type, AnnotationValue annotation, Event domEvent) {
    AnnotationEvent ae = new AnnotationEvent();

    ae.type = type;
    ae.annotation = annotation;
    ae.domEvent = domEvent;

    return ae;
  }

  public int type;
  public String newValue;
  public Event domEvent;
  public AnnotationValue annotation;

}
