package org.swellrt.beta.model;

import org.swellrt.beta.client.rest.ServiceOperation;
import org.swellrt.beta.common.Operation;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsProperty;
import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "Object")
public interface SObject extends SMap, Operation.Response, ServiceOperation.Response {

  @JsFunction
  public interface StatusHandler {
    void exec(SStatusEvent e);
  }

  /**
   * @return the global id of this object. Null for local objects.
   */
  @JsProperty
  public String getId();

  /**
   * Adds a participant.
   * @param participantId
   * @throws InvalidParticipantAddress
   */
  public void addParticipant(String participantId) throws InvalidParticipantAddress;


  /**
   * Removes a participant.
   * @param participantId
   * @throws InvalidParticipantAddress
   */
  public void removeParticipant(String participantId) throws InvalidParticipantAddress;

  /**
   * @return static array of current participants of this object.
   */
  public String[] getParticipants();


  /** Make this object to be public to any user */
  public void setPublic(boolean isPublic);

  /** @return root map of the user's private area */
  public SMap getUserObject();


  /**
   * Register a status handler for this object.
   *
   * @param h
   */
  public void setStatusHandler(StatusHandler h);

  /**
   * Debug only. Return list of blips.
   *
   * @return
   */
  public String[] _getBlips();

  /**
   * Debug only. Return blip content.
   *
   * @return
   */
  public String _getBlipXML(String blipId);

}
