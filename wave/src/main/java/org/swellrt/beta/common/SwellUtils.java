package org.swellrt.beta.common;

import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.wave.ParticipantId;

public class SwellUtils {

  public static String addDomainToParticipant(String participantStr, String domain) {

    Preconditions.checkNotNull(domain, "Domain can't be null");

    if (participantStr == null)
      return null;

    if (!participantStr.contains(ParticipantId.DOMAIN_PREFIX))
      participantStr = participantStr + ParticipantId.DOMAIN_PREFIX + domain;

    return participantStr;
  }

}
