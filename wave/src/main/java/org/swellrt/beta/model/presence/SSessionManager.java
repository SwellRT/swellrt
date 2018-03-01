package org.swellrt.beta.model.presence;

import java.util.ArrayList;
import java.util.List;

import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Provides live updates of the {@link SSession} for the current logged in
 * participant.
 */
public class SSessionManager {

  /** Handles updates of the session */
  public interface UpdateHandler {
    public void onUpdate(SSession session);
  }

  private List<UpdateHandler> handlers = new ArrayList<UpdateHandler>();
  private SSession session = null;

  public SSessionManager() {
  }

  public SSessionManager(SSession session) {
    this.session = session;
  }

  protected void check() {
    if (session == null)
      throw new IllegalStateException("Not Swell Session is present. Not login yet?");
  }

  /** @return the current session data */
  public SSession get() {
    check();
    return session;
  }

  /** Update the session data and trigger update events */
  public void update(SSession session) {
    Preconditions.checkNotNull(session, "Can't update a null session");

    // not sure if color value changes, so ensure we keep it on updates.
    if (this.session != null) {
      this.session = new SSession(this.session.getSessionId(), this.session.getParticipantId(),
          this.session.getColor(), session.getName(), session.getNickname());
    } else {
      this.session = session;
    }

    handlers.forEach(handler -> {
      handler.onUpdate(session);
    });
  }

  /** Register an update handler */
  public void registerHandler(UpdateHandler handler) {
    Preconditions.checkArgument(handler != null, "Can't register a null session update handler");
    if (!handlers.contains(handler)) {
      handlers.add(handler);
    }
  }

  /** Unregister an update handler */
  public void unregisterHandler(UpdateHandler handler) {
    Preconditions.checkArgument(handler != null, "Can't unregister a null session update handler");
    handlers.remove(handler);
  }

}
