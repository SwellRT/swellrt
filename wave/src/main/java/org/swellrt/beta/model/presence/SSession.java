package org.swellrt.beta.model.presence;

import org.swellrt.beta.model.json.SJsonObject;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

/**
 * A Swell Session object contains account and session data for the current
 * logged in participant.
 *
 */
@JsType(namespace = "swell", name = "Session")
public class SSession {

  @JsIgnore
  public static SSession of(SJsonObject jso) {

    SSession session = new SSession(jso.getString("session"),
        ParticipantId.ofUnsafe(jso.getString("participant")),
        new RgbColor(jso.getString("color")), jso.getString("name"), jso.getString("nickname"));

    session.lastAccessTime = jso.getDouble("lasttime");
    session.firstAccessTime = jso.getDouble("firsttime");

    return session;
  }

  private String sessionId;
  private ParticipantId participantId;
  private RgbColor color;
  private String name;
  private String nickname;
  private Double lastAccessTime;
  private Double firstAccessTime;

  @JsIgnore
  protected SSession() {

  }

  @JsIgnore
  public SSession(String sessionId, ParticipantId participantId, RgbColor color, String name,
      String nickname) {
    super();
    this.sessionId = sessionId;
    this.participantId = participantId;
    this.color = color;
    this.name = name;
    this.nickname = nickname;
  }

  @JsProperty
  public String getSessionId() {
    return sessionId;
  }

  @JsIgnore
  public ParticipantId getParticipantId() {
    return participantId;
  }

  @JsProperty
  public String getId() {
    return participantId.getAddress();
  }

  @JsProperty
  public RgbColor getColor() {
    return color;
  }

  @JsProperty
  public String getName() {
    return name;
  }

  @JsProperty
  public String getNickname() {
    return nickname;
  }

  @JsProperty
  public double getLastAccessTime() {
    return lastAccessTime;
  }

  @JsProperty
  public double getFirstAccessTime() {
    return firstAccessTime;
  }

  @JsIgnore
  public void setFirstAccessTime(double time) {
    this.firstAccessTime = time;
  }

  @JsIgnore
  public void setLastAccessTime(double time) {
    this.lastAccessTime = time;
  }

  @JsIgnore
  public SJsonObject toSJson() {

    SJsonObject jso = SJsonObject.create();
    jso.addString("session", sessionId);
    jso.addString("participant", participantId.getAddress());
    jso.addString("color", color.getHexColor());
    jso.addString("name", name);
    jso.addString("nickname", nickname);

    if (lastAccessTime != null)
      jso.addDouble("lasttime", lastAccessTime);

    if (firstAccessTime != null)
      jso.addDouble("firsttime", firstAccessTime);

    return jso;

  }

}
