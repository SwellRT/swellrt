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
 * <p>
 * <br>
 * Stores a log of all participants connected to the object. Object's instances
 * must call <code>logSession()</code> every time the object is opened.
 *
 * @author pablojan@gmail.com
 *
 */
public class SWaveMetadata {

  private static final String SESSIONS_NODE = "sessions";

  protected final SMap metadataMap;

  protected SWaveMetadata(SMap metadataMap) {
    this.metadataMap = metadataMap;
  }

  protected SMap getSessionsMap() {
    try {

      if (!metadataMap.has(SESSIONS_NODE)) {
        metadataMap.put(SESSIONS_NODE, SMap.create());
      }

      return metadataMap.pick(SESSIONS_NODE).asMap();

    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * @return array of all participants that were connected to the object at
   *         least once. Some could not have access permissions currently.
   */
  public SSession[] getSessions() {

    try {
      return Arrays.stream(getSessionsMap().values()).map(node -> {
        return SSession.of(((SPrimitive) node).asSJson());
      }).toArray(SSession[]::new);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * This register method should be called every time a participant opens the
   * object. Sessions are indexed by participant's Id.
   *
   * @param session
   */
  protected void logSession(SSession session) {
    SMap participantsMap = getSessionsMap();
    long now = System.currentTimeMillis();
    String participantIdKey = getSafeKeyFromParticipantId(session.getParticipantId());
    try {

      if (participantsMap.has(participantIdKey)) {
        SPrimitive participantNode = (SPrimitive) participantsMap.get(participantIdKey);
        SSession current = SSession.of(participantNode.asSJson());
        session.setFirstAccessTime(current.getFirstAccessTime());
      } else {
        session.setFirstAccessTime(now);
      }

      session.setLastAccessTime(now);
      participantsMap.put(participantIdKey,
          new SPrimitive(session.toSJson(), new SNodeAccessControl()));

    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }


  private static String getSafeKeyFromParticipantId(ParticipantId participantId) {
    String address = participantId.getAddress();
    return address.replace(".", "-");
  }

}
