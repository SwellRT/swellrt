package org.swellrt.beta.client.platform.web.editor.caret;

import org.swellrt.beta.model.json.SJsonObject;
import org.swellrt.beta.model.presence.SSession;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * Caret information.
 */
@JsType(namespace = "swell", name = "Caret")
public class CaretInfo {

  @JsIgnore
  public static CaretInfo of(SJsonObject jso) {
    SSession session = SSession.of(jso.getObject("session"));
    return new CaretInfo(session, jso.getInt("timestamp"), jso.getInt("position"),
        jso.getString("compositionState"));
  }

  @JsIgnore
  public SJsonObject toSJson() {

    SJsonObject jso = SJsonObject.create();
    jso.addObject("session", session.toSJson());
    jso.addDouble("timestamp", timestamp);
    jso.addInt("position", position);
    jso.addString("compositionState", compositionState);
    return jso;

  }

  private final SSession session;
  private final double timestamp;
  private final int position;
  private final String compositionState;

  @JsIgnore
  public CaretInfo(SSession session, double timestamp, int position, String compositionState) {
    super();
    this.session = session;
    this.timestamp = timestamp;
    this.position = position;
    this.compositionState = compositionState;
  }

  @JsProperty
  public SSession getSession() {
    return this.session;
  }

  @JsProperty
  public double getTimestamp() {
    return this.timestamp;
  }

  @JsProperty
  public int getPosition() {
    return this.position;
  }

  @JsProperty
  public String getCompositionState() {
    return this.compositionState;
  }
}
