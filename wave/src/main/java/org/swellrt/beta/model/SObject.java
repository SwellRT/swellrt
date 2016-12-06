package org.swellrt.beta.model;

import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;

import jsinterop.annotations.JsType;

@JsType(namespace = "swellrt", name = "Object")
public interface SObject extends SMap {
    
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
  
  /**
   * GWT only
   * <p>
   * Returns a JavaScript proxy providing a pure JavaScript view
   * of the object
   */
  public Object asNative();
  
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
