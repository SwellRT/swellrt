package org.swellrt.beta.model;

import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "StatusEvent")
public class SStatusEvent {

  public static final int UNKNOW = -1;
  public static final int ERROR = 1;
  public static final int UPDATE = 2;
  public static final int CLOSED = 3;
  public static final int PARTICIPANT_ADDED = 4;
  public static final int PARTICIPANT_REMOVED = 5;
  
  private final String objectId;
  private final int type;
  
  // Type error
  private SException exception;
  
  // Type update
  private int inflight;
  private int unacknowledge;
  private int uncommitted;
  private long lastAckVersion;
  private long lastCommittedVersion;
  
  // Type close
  private boolean allDataCommitted;
  
  // Type participant add / remove
  private String participantId;
  
  @JsIgnore
  public SStatusEvent(String objectId, SException e) {
    this.objectId = objectId;
    this.type = ERROR;
    this.exception = e;
  }

  @JsIgnore
  public SStatusEvent(String objectId, int inflight, int unacknowdledge, int uncommitted, long lastAckVersion, long lastCommittedVersion) {
    this.objectId = objectId;
    this.type = UPDATE;
  
    this.inflight = inflight;
    this.unacknowledge = unacknowdledge;
    this.uncommitted = uncommitted;
    
    this.lastAckVersion = lastAckVersion;
    this.lastCommittedVersion = lastCommittedVersion;
  }
  

  @JsIgnore
  public SStatusEvent(String objectId, boolean allDataCommitted) {
    this.objectId = objectId;
    this.type = CLOSED;
    this.allDataCommitted = allDataCommitted;
  }

  @JsIgnore
  public SStatusEvent(String objectId, ParticipantId participantId, boolean isRemoved) {
    this.objectId = objectId;
    this.type = isRemoved ? PARTICIPANT_REMOVED : PARTICIPANT_ADDED;
    this.participantId = participantId.getAddress();
  }
  
  @JsProperty
  public String getObjectId() {
    return objectId;
  }

  @JsProperty
  public int getType() {
    return type;
  }

  @JsProperty
  public SException getException() {
    return exception;
  }

  @JsProperty
  public int getInflight() {
    return inflight;
  }

  @JsProperty
  public int getUnacknowledge() {
    return unacknowledge;
  }

  @JsProperty
  public int getUncommitted() {
    return uncommitted;
  }

  @JsProperty
  public double getLastAckVersion() {
    return lastAckVersion;
  }

  @JsProperty
  public double getLastCommittedVersion() {
    return lastCommittedVersion;
  }

  @JsProperty
  public boolean getAllDataCommitted() {
    return allDataCommitted;
  }
  
  public String getParticipantId() {
    return participantId;
  }
  
}
