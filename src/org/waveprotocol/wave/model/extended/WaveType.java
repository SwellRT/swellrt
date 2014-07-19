package org.waveprotocol.wave.model.extended;

import com.google.common.base.Preconditions;

import org.waveprotocol.wave.model.id.WaveId;



public enum WaveType {


  CONVERSATION("conversation","w"),
  CHAT("chat","c"), DOCUMENT("document","d"), UNKNOWN("unknown","u");

  private final String typeStrValue;
  private final String prefix;

  WaveType(String strValue, String prefix) {
    this.typeStrValue = strValue;
    this.prefix = prefix;
  }

  public String toString() {
    return this.typeStrValue;
  }

  protected static WaveType from(String s) {

    if (s == null) return UNKNOWN;

    if (s.equals("document"))
      return DOCUMENT;

    else if (s.equals("chat"))
      return CHAT;

    else if (s.equals("conversation")) return CONVERSATION;

    return UNKNOWN;
  }

  public String getWaveIdPrefix() {
    return prefix;
  }


  public static String serialize(WaveType wt) {
    return wt.toString();
  }


  public static WaveType deserialize(String s) {
    return from(s);
  }


  public static WaveType fromWaveId(WaveId waveId) {
    Preconditions.checkArgument(waveId.getId().contains("+"));

    String prefix = waveId.getId().split("\\+")[0];

    if (prefix.equals("w")) {
      return WaveType.CONVERSATION;
    }

    if (prefix.equals("c")) {
      return WaveType.CHAT;
    }


    return WaveType.UNKNOWN;

  }

}
