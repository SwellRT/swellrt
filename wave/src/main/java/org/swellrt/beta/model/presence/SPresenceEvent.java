package org.swellrt.beta.model.presence;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "PresenceEvent")
public class SPresenceEvent {

  @JsFunction
  public interface Handler {
    void exec(SPresenceEvent e);
  }

  public final static String EVENT_ONLINE = "online";
  public final static String EVENT_OFFLINE = "offline";

  private SSession session;
  private String type;
  private double time;

  @JsIgnore
  public SPresenceEvent(SSession session, String type, long time) {
    super();
    this.session = session;
    this.type = type;
    this.time = time;
  }

  @JsProperty
  public SSession getSession() {
    return session;
  }

  @JsProperty
  public String getType() {
    return type;
  }

  @JsProperty
  public double getTime() {
    return time;
  }

}
