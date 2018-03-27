package org.waveprotocol.wave.model.account.group;

import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "ReadableGroup")
public interface ReadableGroup {

  public ParticipantId getId();

  public String getName();

  public ParticipantId[] getParticipants();

}
