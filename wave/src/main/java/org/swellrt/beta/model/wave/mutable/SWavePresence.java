package org.swellrt.beta.model.wave.mutable;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.wave.WaveDeps;
import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SMutationHandler;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.json.SJsonObject;
import org.swellrt.beta.model.local.SMapLocal;
import org.swellrt.beta.model.presence.SPresenceEvent;
import org.swellrt.beta.model.presence.SSession;
import org.swellrt.beta.model.presence.SSessionManager;
import org.swellrt.beta.model.presence.SSessionManager.UpdateHandler;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.model.util.Preconditions;

/**
 * Updates presence status of the local user and check presence status of remote
 * users. Status is stored in a map of the collaborative object (that should be
 * transient).
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SWavePresence {

  private static final String PRESENCE_NODE = "presence";

  private static final int REFRESH_TIME_MS = 5000;
  private static final int MAX_INACTIVE_TIME = 8000;

  private static final String LAST_ACTIVITY_TIME = "time";

  /** Convenience cache the online/offline state of remote users */
  private Map<String, Boolean> onlineStatus = new HashMap<String, Boolean>();

  private boolean hasStarted = false;

  /**
   * Perform periodically a keep a live signal (update our entry in the status
   * map) and check for inactive sessions.
   */

  private final Scheduler.IncrementalTask presenceUpdateTask = new Scheduler.IncrementalTask() {

    @Override
    public boolean execute() {

      // update our session status
      refreshSession();

      // check for inactive sessions
      long now = System.currentTimeMillis();

      try {
        String[] keys = presenceStatusMap.keys();
        for (int i=0; i < keys.length; i++) {

          String sessionId = keys[i];

          // ignore our own status
          if (sessionId.equals(sessionManager.get().getSessionId())) {
            continue;
          }

          SPrimitive status = (SPrimitive) presenceStatusMap.pick(sessionId);
          long lastActiveTime = status.asSJson().getLong(LAST_ACTIVITY_TIME);

          if (now - lastActiveTime > MAX_INACTIVE_TIME
              && onlineStatus.getOrDefault(sessionId, false)) {
            onlineStatus.put(sessionId, false);
            if (eventHandler != null) {
              eventHandler.exec(new SPresenceEvent(SSession.of(status.asSJson()),
                  SPresenceEvent.EVENT_OFFLINE, lastActiveTime));
            }
          }
        }


      } catch (SException e) {
        throw new IllegalStateException(e);
      }

      return true;
    }


  };

  /**
   * A status map mutation means an online or offline event.
   */
  private final SMutationHandler presenceUpdateHandler = new SMutationHandler() {

    @Override
    public boolean exec(SEvent e) {

      SPrimitive status = (SPrimitive) e.getNode();
      SSession eventSession = SSession.of(status.asSJson());
      long lastActiveTime = status.asSJson().getLong(LAST_ACTIVITY_TIME);

      // Skip our own status
      if (eventSession.getSessionId().equals(sessionManager.get().getSessionId())) {
        return false;
      }

      if (e.isAddEvent() || e.isUpdateEvent()) {
        // if the session was offline, go online
        if (!onlineStatus.getOrDefault(eventSession.getSessionId(), false)) {
          onlineStatus.put(eventSession.getSessionId(), true);
          if (eventHandler != null) {
            eventHandler.exec(
                new SPresenceEvent(eventSession, SPresenceEvent.EVENT_ONLINE, lastActiveTime));
          }
        }
      } else if (e.isRemoveEvent()) {
        onlineStatus.put(eventSession.getSessionId(), false);
        if (eventHandler != null) {
          eventHandler.exec(new SPresenceEvent(eventSession,
              SPresenceEvent.EVENT_OFFLINE, lastActiveTime));
        }
      }

      return false;
    }
  };

  /** Listen for changes in user name... */
  private final SSessionManager.UpdateHandler sessionHandler = new UpdateHandler() {

    @Override
    public void onUpdate(SSession session) {
      refreshSession();
    }
  };

  /** the shared map with presence status of all connected users */
  private final SMap presenceStatusMap;

  /** Local user's sessions using the object */
  private final SSessionManager sessionManager;

  /** the handler to receive presence events */
  private SPresenceEvent.Handler eventHandler;

  public static SWavePresence create(SMap transientMap, SSessionManager sessionMgr) {

    SMap presenceMap = null;

    try {

      if (!transientMap.has(PRESENCE_NODE)) {
        transientMap.put(PRESENCE_NODE, new SMapLocal());
      }

      presenceMap = transientMap.pick(PRESENCE_NODE).asMap();

    } catch (SException e) {
      throw new IllegalStateException(e);
    }

    return new SWavePresence(presenceMap, sessionMgr);
  }

  protected SWavePresence(SMap presenceStatusMap, SSessionManager sessionMgr) {
    Preconditions.checkNotNull(presenceStatusMap, "Presence requires a map for storage");
    this.presenceStatusMap = presenceStatusMap;
    this.sessionManager = sessionMgr;




  }

  /** Refresh our session's time stamp to inform that we are alive */
  private void refreshSession() {

    SJsonObject sjson = sessionManager.get().toSJson();
    sjson.addLong(LAST_ACTIVITY_TIME, System.currentTimeMillis());
    try {
      presenceStatusMap.put(sessionManager.get().getSessionId(), sjson);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }

  }

  /**
   * When this component is started, traverse the presence status map and check
   * which sessions are online.
   */
  private void checkAllStatuses() {

    long now = System.currentTimeMillis();

    try {
      String[] keys = presenceStatusMap.keys();
      for (int i = 0; i < keys.length; i++) {

        String sessionId = keys[i];

        // ignore our own status
        if (sessionId.equals(sessionManager.get().getSessionId())) {
          continue;
        }

        SPrimitive status = (SPrimitive) presenceStatusMap.pick(sessionId);
        SSession thisSession = SSession.of(status.asSJson());
        long lastActiveTime = status.asSJson().getLong(LAST_ACTIVITY_TIME);

        if (now - lastActiveTime <= MAX_INACTIVE_TIME) {

          onlineStatus.put(sessionId, true);
          if (eventHandler != null) {
            eventHandler
                .exec(new SPresenceEvent(thisSession, SPresenceEvent.EVENT_ONLINE, lastActiveTime));
          }

        }
      }

    } catch (SException e) {
      throw new IllegalStateException(e);
    }

  }

  public void registerHandler(SPresenceEvent.Handler eventHandler) {
    this.eventHandler = eventHandler;
    if (this.eventHandler != null)
      checkAllStatuses();
  }

  public void start() {

    // Ignore for platforms not supported yet
    if (WaveDeps.lowPriorityTimer == null)
      return;

    if (hasStarted)
      return;

    try {
      this.presenceStatusMap.listen(presenceUpdateHandler);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }

    this.sessionManager.registerHandler(sessionHandler);

    WaveDeps.lowPriorityTimer.scheduleRepeating(presenceUpdateTask, 0, REFRESH_TIME_MS);

    hasStarted = true;
  }

  public void stop() {

    // Ignore for platforms not supported yet
    if (WaveDeps.lowPriorityTimer == null)
      return;

    if (hasStarted) {

      WaveDeps.lowPriorityTimer.cancel(presenceUpdateTask);

      try {
        this.presenceStatusMap.unlisten(presenceUpdateHandler);
      } catch (SException e) {
        throw new IllegalStateException(e);
      }

      try {
        this.presenceStatusMap.remove(sessionManager.get().getSessionId());
      } catch (SException e) {
        throw new IllegalStateException(e);
      }

      this.sessionManager.unregisterHandler(sessionHandler);

      hasStarted = false;

    }

  }

  public boolean hasHandler() {
    return eventHandler != null;
  }

}
