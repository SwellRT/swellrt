package org.swellrt.beta.model.wave.mutable;

import java.util.Arrays;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SNodeAccessControl;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.presence.SSession;
import org.waveprotocol.wave.model.wave.ParticipantId;

/**
 * Object's meta data stored in the object permanent storage.
 *
 * @author pablojan@gmail.com
 *
 */
public class SWaveMetadata {

  private static final String PARTICIPANTS_NODE = "participants";

  protected final SMap metadataMap;

  protected SWaveMetadata(SMap metadataMap) {
    this.metadataMap = metadataMap;
  }

  protected SMap getParticipantsMap() {
    try {

      if (!metadataMap.has(PARTICIPANTS_NODE)) {
        metadataMap.put(PARTICIPANTS_NODE, SMap.create());
      }

      return metadataMap.pick(PARTICIPANTS_NODE).asMap();

    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * @return array of all participants of the object, whether they can currently
   *         access the object or not.
   */
  public SSession[] getParticipants() {

    try {
      return Arrays.stream(getParticipantsMap().values()).map(node -> {
        return SSession.of(((SPrimitive) node).asSJson());
      }).toArray(SSession[]::new);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * This register method should be called every time a participant opens the
   * object.
   *
   * @param participant
   */
  protected void logParticipant(SSession participant) {
    SMap participantsMap = getParticipantsMap();
    long now = System.currentTimeMillis();
    String participantIdKey = getSafeKeyFromParticipantId(participant.getParticipantId());
    try {

      if (participantsMap.has(participantIdKey)) {
        SPrimitive participantNode = (SPrimitive) participantsMap.get(participantIdKey);
        SSession current = SSession.of(participantNode.asSJson());
        participant.setFirstAccessTime(current.getFirstAccessTime());
      } else {
        participant.setFirstAccessTime(now);
      }

      participant.setLastAccessTime(now);
      participantsMap.put(participantIdKey,
          new SPrimitive(participant.toSJson(), new SNodeAccessControl()));

    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }


  private static String getSafeKeyFromParticipantId(ParticipantId participantId) {
    String address = participantId.getAddress();
    return address.replace(".", "-");
  }

}
