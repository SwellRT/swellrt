package org.swellrt.beta.model;

import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;

import jsinterop.annotations.JsFunction;
import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "Object")
public interface SObject extends SMap {
    
  @JsFunction
  public interface StatusHandler {
    void exec(SStatusEvent e);
  }
  
  /**
   * @return the global id of this object. Null for local objects.
   */
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
  public SMap getPrivateArea();
  
  /**
   * GWT only
   * <p>
   * Returns a JavaScript proxy providing a pure JavaScript view
   * of the object
   */
  public Object asNative();
  
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
  public String[] _debug_getBlipList();
  
  /**
   * Debug only. Return blip content.
   * 
   * @return
   */
  public String _debug_getBlip(String blipId);

}
