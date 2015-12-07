package org.swellrt.server.box.servlet;

import org.waveprotocol.wave.model.wave.ParticipantId;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class NotificationService implements SwellRTService {

  ParticipantId participantId;

  public NotificationService(ParticipantId participantId) {
    this.participantId = participantId;
  }

  @Override
  public void execute(HttpServletRequest req, HttpServletResponse response) throws IOException {


  }

  public static NotificationService get(ParticipantId participantId) {
    return new NotificationService(participantId);
  }

}
