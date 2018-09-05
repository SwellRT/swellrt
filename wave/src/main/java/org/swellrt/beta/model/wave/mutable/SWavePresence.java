package org.swellrt.beta.model.wave.mutable;

import java.util.HashMap;
import java.util.Map;

import org.swellrt.beta.client.ServiceConfig;
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
 * Manages the participant's online/offline state. This class can be used in two
 * modes:
 * <p>
 * <br>
 * Passive mode: <br>
 * Participants update periodically a presence (transient) map with a timestamp.
 * Periodically each participant checks the presence map to determine which
 * participants are online or not, depending if their timestamp is not expired
 * (inactive time). <br>
 * This mode is controlled with methods {@link #start()} and {@link #stop()}
 * <p>
 * <br>
 * Active mode: <br>
 * Participants inform they are online setting a timestamp in the presence map
 * (method {@link #setOnline()}) or they are offline removing it
 * ({@link #setOffline()})
 *
 *
 * @author pablojan@gmail.com (Pablo Ojanguren)
 *
 */
public class SWavePresence {

  public enum Mode {
    ACTIVE, PASSIVE
  };

  private int PASSIVE_REFRESH_TIME_MS = 10000;
  private int PASSIVE_MAX_INACTIVE_TIME_MS = 15000;


  private static final String PRESENCE_NODE = "presence";
  protected static final String LAST_ACTIVITY_TIME = "time";

  /** A local cache of online users */
  private Map<String, Boolean> onlineStateMap = new HashMap<String, Boolean>();

  /** The shared map with presence status of all connected users */
  private final SMap presenceSharedMap;

  /** Local user's sessions using the object */
  private final SSessionManager sessionManager;

  /** Handler listening to remote presence changes */
  private SPresenceEvent.Handler eventHandler;

  /** mode of presence tracking */
  private Mode mode;

  /** presence handlers enabled? */
  private boolean started = false;

  /**
   * Detect changes in the shared map for online-offline changes.
   */
  private final SMutationHandler presenceMutationHandler = new SMutationHandler() {

    @Override
    public boolean exec(SEvent e) {

      SPrimitive presenceRecord = (SPrimitive) e.getNode();
      SSession eventSession = SWavePresence.getSSession(presenceRecord);
      long lastActiveTime = SWavePresence.getPresenceValue(presenceRecord);

      // Skip our own status
      if (eventSession.getSessionId().equals(sessionManager.get().getSessionId())) {
        return false;
      }

      if (e.isAddEvent() || e.isUpdateEvent()) {
        // if the session was offline, go online
        if (!onlineStateMap.getOrDefault(eventSession.getSessionId(), false)) {
          onlineStateMap.put(eventSession.getSessionId(), true);
          if (eventHandler != null) {
            eventHandler.exec(
                new SPresenceEvent(eventSession, SPresenceEvent.EVENT_ONLINE, lastActiveTime));
          }
        }
      } else if (e.isRemoveEvent()) {
        onlineStateMap.put(eventSession.getSessionId(), false);
        if (eventHandler != null) {
          eventHandler
              .exec(new SPresenceEvent(eventSession, SPresenceEvent.EVENT_OFFLINE, lastActiveTime));
        }
      }

      return false;
    }
  };

  /*
   * ------------------------ Scheduler Tasks ---------------------------
   */

  /**
   * (Passive Mode) Sends periodically a keep alive signal (update our entry in
   * the status map) and check for inactive sessions.
   */
  private final Scheduler.IncrementalTask presenceUpdateTask = new Scheduler.IncrementalTask() {

    @Override
    public boolean execute() {

      long now = System.currentTimeMillis();

      // update our entry in the shared map
      setPresenceStateValue(presenceSharedMap, sessionManager.get(), now);

      // check for inactive sessions
      try {
        String[] keys = presenceSharedMap.keys();
        for (int i = 0; i < keys.length; i++) {

          String sessionId = keys[i];

          // ignore our own status
          if (sessionId.equals(sessionManager.get().getSessionId())) {
            continue;
          }

          long lastActiveTime = SWavePresence.getPresenceStateValue(presenceSharedMap, sessionId);

          if (now - lastActiveTime > PASSIVE_MAX_INACTIVE_TIME_MS
              && onlineStateMap.getOrDefault(sessionId, false)) {
            onlineStateMap.put(sessionId, false);
            if (eventHandler != null) {
              eventHandler
                  .exec(new SPresenceEvent(SWavePresence.getSSession(presenceSharedMap, sessionId),
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

  /** Listen for changes in user name... */
  private final SSessionManager.UpdateHandler sessionHandler = new UpdateHandler() {

    @Override
    public void onUpdate(SSession session) {
      setPresenceStateValue(presenceSharedMap, session, System.currentTimeMillis());
    }
  };



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

    Preconditions.checkNotNull(presenceMap, "A transient map to store online state is required");

    return new SWavePresence(presenceMap, sessionMgr);
  }

  protected SWavePresence(SMap presenceStateMap, SSessionManager sessionManager) {
    this.presenceSharedMap = presenceStateMap;
    this.sessionManager = sessionManager;
    this.mode = Mode.ACTIVE;
  }

  /*
   * ---------------------------------------------------------------------------
   */

  protected static void setPresenceStateValue(SMap presenceStateMap, SSession session,
      long timeValue) {

    SJsonObject sjObject = session.toSJson();
    sjObject.addLong(LAST_ACTIVITY_TIME, timeValue);
    try {
      presenceStateMap.put(session.getSessionId(), sjObject);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }

  protected static void clearPresenceStateValue(SMap presenceStateMap, String sessionId) {
    try {
      presenceStateMap.remove(sessionId);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }

  protected static long getPresenceStateValue(SMap presenceStateMap, String sessionId) {
    try {
      SPrimitive state = (SPrimitive) presenceStateMap.pick(sessionId);
      return state.asSJson().getLong(LAST_ACTIVITY_TIME);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }

  protected static SSession getSSession(SMap presenceStateMap, String sessionId) {

    try {
      SPrimitive state = (SPrimitive) presenceStateMap.pick(sessionId);
      return SSession.of(state.asSJson());
    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }

  protected static SSession getSSession(SPrimitive stateRecord) {
    return SSession.of(stateRecord.asSJson());
  }

  protected static long getPresenceValue(SPrimitive stateRecord) {
    return stateRecord.asSJson().getLong(LAST_ACTIVITY_TIME);
  }


  /*
   * ---------------------------------------------------------------------------
   */

  public void setEventHandler(SPresenceEvent.Handler eventHandler) {
    this.eventHandler = eventHandler;
    if (this.eventHandler != null)
      checkStateForAll();
  }

  /**
   *
   */
  private void checkStateForAll() {

    long now = System.currentTimeMillis();


    try {
      String[] keys = presenceSharedMap.keys();
      for (int i = 0; i < keys.length; i++) {

        String sessionId = keys[i];

        // ignore our own status
        if (sessionId.equals(sessionManager.get().getSessionId())) {
          continue;
        }

        long lastActiveTime = SWavePresence.getPresenceStateValue(presenceSharedMap, sessionId);

        if ((now - lastActiveTime <= PASSIVE_MAX_INACTIVE_TIME_MS) || mode == Mode.ACTIVE) {

          onlineStateMap.put(sessionId, true);
          if (eventHandler != null) {
            eventHandler
                .exec(new SPresenceEvent(SWavePresence.getSSession(presenceSharedMap, sessionId),
                    SPresenceEvent.EVENT_ONLINE, lastActiveTime));
          }

        }


      }

      if (mode == Mode.ACTIVE) {
        for (String sessionId: onlineStateMap.keySet()) {
          if (onlineStateMap.get(sessionId) && !presenceSharedMap.has(sessionId)) {
            if (eventHandler != null) {
              onlineStateMap.put(sessionId, false);
              eventHandler
                  .exec(new SPresenceEvent(SWavePresence.getSSession(presenceSharedMap, sessionId),
                      SPresenceEvent.EVENT_OFFLINE, 0));
            }
          }
        }
      }



    } catch (SException e) {
      throw new IllegalStateException(e);
    }

  }

  /*
   * ---------------------------------------------------------------------------
   */


  public void start(Mode mode) {

    Preconditions.checkNotNull(mode, "Presence module requires a explicit mode");

    // Set shared handlers for both modes

    try {
      this.presenceSharedMap.listen(presenceMutationHandler);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }

    this.sessionManager.registerHandler(sessionHandler);

    // Schedule tasks for passive mode

    if (mode == Mode.PASSIVE) {

      // Ignore for platforms not supported yet
      if (WaveDeps.lowPriorityTimer == null)
        return;

      if (started)
        return;

      PASSIVE_REFRESH_TIME_MS = ServiceConfig.presencePingRateMs();
      PASSIVE_MAX_INACTIVE_TIME_MS = PASSIVE_MAX_INACTIVE_TIME_MS
          + (PASSIVE_MAX_INACTIVE_TIME_MS / 2);
      WaveDeps.lowPriorityTimer.scheduleRepeating(presenceUpdateTask, 0, PASSIVE_REFRESH_TIME_MS);

    }

    started = true;

  }

  public void stop() {

    if (!started)
      return;

    // Unset shared handlers for both modes

    try {
      this.presenceSharedMap.unlisten(presenceMutationHandler);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }

    try {
      this.presenceSharedMap.remove(sessionManager.get().getSessionId());
    } catch (SException e) {
      throw new IllegalStateException(e);
    }

    this.sessionManager.unregisterHandler(sessionHandler);

    // Cancel schedules for tasks in passive mode

    if (mode == Mode.PASSIVE) {

      // Ignore for platforms not supported yet
      if (WaveDeps.lowPriorityTimer == null)
        return;

      WaveDeps.lowPriorityTimer.cancel(presenceUpdateTask);
    }

    started = false;

  }

  public void setOnline() {

    if (this.mode != Mode.ACTIVE)
      return;

    SWavePresence.setPresenceStateValue(presenceSharedMap, sessionManager.get(),
        System.currentTimeMillis());

  }

  public void setOffline() {

    if (this.mode != Mode.ACTIVE)
      return;

    SWavePresence.clearPresenceStateValue(presenceSharedMap, sessionManager.get().getId());

  }

  /*
   * ---------------------------------------------------------------------------
   */

}
