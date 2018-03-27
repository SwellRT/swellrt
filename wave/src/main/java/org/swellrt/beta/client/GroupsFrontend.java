package org.swellrt.beta.client;

import org.swellrt.beta.client.ServiceFrontend.AsyncResponse;
import org.swellrt.beta.common.SException;
import org.waveprotocol.wave.model.account.group.Group;
import org.waveprotocol.wave.model.account.group.ReadableGroup;
import org.waveprotocol.wave.model.wave.ParticipantId;

import jsinterop.annotations.JsType;

@JsType(namespace = "swell", name = "GroupsFrontend")
public interface GroupsFrontend {

  /**
   * Open a group to perform live updates. Returns null if it doesn't exist.
   * Throw exception if participant doesn't have enough permissions.
   *
   * @throws SException
   */
  void open(ParticipantId groupId, AsyncResponse<Group> callback) throws SException;

  /**
   * Create a group. Returns null if it already exists.
   */
  void create(ParticipantId groupId, AsyncResponse<Group> callback) throws SException;

  /**
   * Query groups of the current participant
   */
  void get(AsyncResponse<ReadableGroup[]> groupIds) throws SException;

  /**
   * Delete a group if current participant has enough permissions.
   * <p>
   * This operation must ensure that group id is removed from all
   * objects/wavelets where it is a participant.
   *
   * @param groupId
   */
  void delete(ParticipantId groupId) throws SException;
}
