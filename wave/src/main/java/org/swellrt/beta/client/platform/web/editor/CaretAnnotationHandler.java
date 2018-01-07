/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.swellrt.beta.client.platform.web.editor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.waveprotocol.wave.client.account.Profile;
import org.waveprotocol.wave.client.account.ProfileListener;
import org.waveprotocol.wave.client.account.ProfileManager;
import org.waveprotocol.wave.client.account.ProfileSession;
import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.doodad.selection.CaretMarkerRenderer;
import org.waveprotocol.wave.client.doodad.selection.CaretView;
import org.waveprotocol.wave.client.doodad.selection.CaretView.CaretViewFactory;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.BoundaryFunction;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.util.DocumentContext;
import org.waveprotocol.wave.model.document.util.LocalDocument;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.ReadableStringMap.ProcV;
import org.waveprotocol.wave.model.util.StringMap;
import org.waveprotocol.wave.model.wave.InvalidParticipantAddress;
import org.waveprotocol.wave.model.wave.ParticipantId;

import com.google.common.annotations.VisibleForTesting;

/**
 * Deals with rendering of remote carets (and selections).
 * <p>
 * This class relies on a {@link ProfileManager} instance to render properly
 * carets.
 * <p>
 * Modified version of original SelectionAnnotationHandler class.
 * <p>
 * {@link CaretManager} sets following local annotations. This handler uses
 * these annotations to render carets and selection highlights:
 * <p>
 * {@link #CaretAnnotationConstants.USER_END} This annotation starts from where
 * the user's blinking caret would be, and extends to the end of the document.
 * Its value is their address - Range annotation, with the prefix
 * <p>
 * {@link #CaretAnnotationConstants.USER_RANGE} This annotation extends over the
 * user's selected range. if their selection is collapsed, this annotation is
 * not present. Its value is their address.
 * <p>
 * <br>
 * Each key is suffixed with a globally unique value identifying the current
 * session (e.g. one value per browser tab). The value of the annotation is the
 * participant id.
 *
 *
 * TODO: write unit test based on SelectionAnnotationHandlerTest
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public class CaretAnnotationHandler implements AnnotationMutationHandler, ProfileListener {
  /** Time out for not showing stale carets */
  public static final int STALE_CARET_TIMEOUT_MS = 6 * 1000;

  /**
   * Don't do a stale check more frequently than this
   */
  private static final int MINIMUM_STALE_CHECK_GAP_MS = Math.max(
      STALE_CARET_TIMEOUT_MS / 3, // More frequent than the stale timeout
      5 * 1000); // But, lower bound on the frequency, as this is not a high priority thing.

  public static final int MAX_NAME_LENGTH_FOR_SELECTION_ANNOTATION = 15;


  /**
   * Installs this doodad.
   */
  public static CaretAnnotationHandler register(Registries registries) {

    CaretMarkerRenderer carets = CaretMarkerRenderer.getInstance();

    registries.getElementHandlerRegistry().registerRenderer(
        CaretMarkerRenderer.FULL_TAGNAME, carets);

    return register(registries, SchedulerInstance.getLowPriorityTimer(), carets);
  }

  @VisibleForTesting
  static CaretAnnotationHandler register(Registries registries, TimerService timer,
      CaretViewFactory carets) {

    CaretAnnotationHandler selection = new CaretAnnotationHandler(
        registries.getPaintRegistry(), timer, carets);

    registries.getAnnotationHandlerRegistry().
        registerHandler(CaretAnnotationConstants.USER_PREFIX, selection);

    return selection;
  }



  private final PainterRegistry painterRegistry;

  private final TimerService scheduler;

  // Used for getting profiles, which are needed for choosing names.
  private ProfileManager profileManager;

  private final StringMap<String> highlightCache = CollectionUtils.createStringMap();

  private final CaretViewFactory caretViewFactory;


  /**
   * Information required for book-keeping and managing the logic of rendering
   * each session's caret marker and selection.
   */
  class CaretData {

    /** UI for rendering the marker associated with this user session */
    private final CaretView caretView;

    /** The address of the user session (1:n mapping of address:session) */
    private final String address;

    /** Session connected to this user. */
    private final String sessionId;

    /** Assigned colour */
    private RgbColor color;

    /** Time at which caret will expire */
    private double expiry;

    /**
     * Implementation detail, the value of {@link #expiry} when this object was
     * placed in the expiry queue, to ensure queue stability.
     */
    private double originallyScheduledExpiry;

    /**
     * Document in which the session's caret is currently rendered. This is used
     * for book-keeping and to be able to re-render relevant sections of the
     * document.
     */
    private DocumentContext<?, ?, ?> bundle;

    /**
     * Cache of the name reported in the UI - to avoid re-setting the name if it
     * does not change
     */
    private String name;

    private ProfileSession profileSession;

    CaretData(CaretView caretView, ProfileSession profileSession, String address, String sessionId) {

      if (carets.containsKey(sessionId)) {
        throw new IllegalArgumentException("Session data already exists");
      }

      this.address = address;

      carets.put(sessionId, this);

      this.profileSession = profileSession;

      this.caretView = caretView;
      this.sessionId = sessionId;
      this.color = profileSession.getColor();
      this.caretView.setColor(this.color);

      updateProfile(profileSession.getProfile());
    }

    public void updateProfile(Profile profile) {
      this.name = profile.getName();
      caretView.setName(name);
    }

    public void compositionStateUpdated(String newState) {
      caretView.setCompositionState(newState);
    }

    public boolean isStale() {
      return scheduler.currentTimeMillis() > expiry;
    }

    public RgbColor getColour() {
      return color;
    }

    public ProfileSession getProfileSession() {
      return profileSession;
    }
  }


  private static RgbColor average(Collection<RgbColor> colors) {

    int size = colors.size();
    int red = 0, green = 0, blue = 0;
    for (RgbColor color : colors) {
      red += color.red;
      green += color.green;
      blue += color.blue;
    }

    return size == 0 ? RgbColor.BLACK : new RgbColor(red / size, green / size, blue / size);
  }

  private RgbColor grey = new RgbColor(128, 128, 128);


  private String getUsersHighlight(String sessions) {

    if (!highlightCache.containsKey(sessions)) {

      // comma-split:
      String[] sessionIDs = sessions.split(",");

      List<RgbColor> colours = new ArrayList<RgbColor>();

      for (String id : sessionIDs) {

        if (!"".equals(id)) {
          ProfileSession session = profileManager.getSession(id, null);
          colours.add(session != null ? session.getColor() : grey);
        }
      }

      // average out the colours, then reduce opacity by averaging against white.
      RgbColor lighter = average(Arrays.asList(average(colours), RgbColor.WHITE));
      highlightCache.put(sessions, lighter.getCssColor());
    }

    return highlightCache.get(sessions);
  }


  private final PaintFunction spreadFunc = new PaintFunction() {

    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {

      // discover which sessions have highlighted this range:

      String sessions = "";

      for (Map.Entry<String, Object> entry : from.entrySet()) {

        if (CaretAnnotationConstants.isRangeKey(entry.getKey())) {

          String sessionId = CaretAnnotationConstants.endSuffix(entry.getKey());
          String address = (String) entry.getValue();

          if (address == null || getActiveCaretData(sessionId) == null) {
            continue;
          }

          sessions += sessionId + ",";
        }

      }

      // combine them together and highlight the range accordingly:
      if (!sessions.equals("")) {
        return Collections.singletonMap("backgroundColor", getUsersHighlight(sessions));
      } else {
        return Collections.emptyMap();
      }
    }
  };


  private final BoundaryFunction boundaryFunc = new BoundaryFunction() {

    public <N, E extends N, T extends N> E apply(LocalDocument<N, E, T> localDoc, E parent,
        N nodeAfter, Map<String, Object> before, Map<String, Object> after, boolean isEditing) {

      E ret = null;
      E usersContainer = null;

      for (Map.Entry<String, Object> entry : after.entrySet()) {

        if (CaretAnnotationConstants.isEndKey(entry.getKey())) {
          // get the user's address:
          String address = (String) entry.getValue();
          if (address == null) {
            continue;
          }

          // get the session ID:
          String sessionId = CaretAnnotationConstants.endSuffix(entry.getKey());
          CaretData caretData = getActiveCaretData(sessionId);
          if (caretData == null) {
            continue;
          }

          // if needed, first create a simple container to put caret DOMs into:
          if (usersContainer == null) {
            ret = localDoc.transparentCreate(
                CaretMarkerRenderer.FULL_TAGNAME, Collections.<String, String>emptyMap(),
                parent, nodeAfter);
            usersContainer = ret;

          }

          // attach caret view to the anchor DOM element
          caretViewFactory.setMarker(usersContainer, caretData.caretView);
        }
      }
      return ret;
    }
  };

  private CaretData getActiveCaretData(String sessionId) {
    CaretData caretData = carets.get(sessionId);
    return caretData != null && !caretData.isStale() ? caretData : null;
  }

  /** Seed the annotation handler with all required config objects. */
  public CaretAnnotationHandler(PainterRegistry registry,
      TimerService timer,
      CaretViewFactory markerFactory) {

    this.painterRegistry = registry;
    this.scheduler = timer;
    this.caretViewFactory = markerFactory;
  }


  /**
   * Set the profile manager dependency. The manager depends on
   * a service instance.
   *
   * @param profileManager
   */
  public void setProfileManager(ProfileManager profileManager) {

    if (this.profileManager != null)
      this.profileManager.removeListener(this);

    carets.clear();
    caretsToExpire.clear();

    this.profileManager = profileManager;

    if (profileManager != null) {
      this.profileManager.addListener(this);
    }
  }


  private void updateCaretData(String sessionId, String address, DocumentContext<?, ?, ?> doc,
      boolean isCurrentUser) {

    // Access directly from the map because the high level getter filters stale
    // carets, and this could result in memory leaks.
    CaretData caretData = carets.get(sessionId);

    if (caretData == null) {

      ParticipantId participantId;
      try {
        participantId = ParticipantId.of(address);
      } catch (InvalidParticipantAddress e) {
        return;
      }

      ProfileSession profile = profileManager.getSession(sessionId, participantId);
      caretData = new CaretData(caretViewFactory.create(), profile, address, sessionId);

    }

    double lastActivityTime = scheduler.currentTimeMillis();
    double expiry = lastActivityTime + STALE_CARET_TIMEOUT_MS;
    scheduleCaretExpiration(caretData, expiry, doc);

  }


  private void scheduleCaretExpiration(CaretData caretData, double expiry, DocumentContext<?, ?, ?> doc) {

    caretData.expiry = expiry;
    caretData.originallyScheduledExpiry = expiry;

    if (caretData.bundle == null) {
      caretsToExpire.add(caretData);
    }

    caretData.bundle = doc;

    if (!scheduler.isScheduled(expiryTask)) {
      scheduler.scheduleRepeating(expiryTask, MINIMUM_STALE_CHECK_GAP_MS,
          MINIMUM_STALE_CHECK_GAP_MS);
    }
  }



  private final Scheduler.IncrementalTask expiryTask = new Scheduler.IncrementalTask() {

    @Override
    public boolean execute() {

      while (!caretsToExpire.isEmpty()) {

        CaretData caretData = caretsToExpire.element();

        if (caretData.originallyScheduledExpiry > scheduler.currentTimeMillis()) {
          return true;
        }

        caretsToExpire.remove();

        if (caretData.expiry > scheduler.currentTimeMillis()) {

          caretData.originallyScheduledExpiry = caretData.expiry;
          caretsToExpire.add(caretData);

        } else {
          expireCaret(caretData);
        }
      }

      return false;
    }
  };

  /**
   * Cleanup any state associated with expired selection annotation data
   *
   * @param caretData expired data
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void expireCaret(CaretData caretData) {

    if (caretData.bundle == null)
      return;

    DocumentContext<?, ?, ?> bundle = caretData.bundle;
    MutableAnnotationSet.Local annotations = bundle.localAnnotations();

    caretData.bundle = null;

    painterRegistry.unregisterBoundaryFunction(
        CollectionUtils.newStringSet(CaretAnnotationConstants.endKey(caretData.sessionId)),
        boundaryFunc);

    painterRegistry.unregisterPaintFunction(
        CollectionUtils.newStringSet(CaretAnnotationConstants.rangeKey(caretData.sessionId)),
        spreadFunc);

    int size = bundle.document().size();
    int rangeStart = annotations.firstAnnotationChange(0, size,
        CaretAnnotationConstants.rangeKey(caretData.sessionId), null);
    int rangeEnd = annotations.lastAnnotationChange(0, size,
        CaretAnnotationConstants.rangeKey(caretData.sessionId), null);
    int hotSpot = annotations.firstAnnotationChange(0, size,
        CaretAnnotationConstants.endKey(caretData.sessionId), null);

    if (rangeStart == -1) {
      rangeStart = rangeEnd = hotSpot;
    }

    /*
     * TODO(danilatos): Enable this code. Problems to resolve: 1. It causes
     * mutations just from the renderer. Rather the cleanup is best done in the
     * same place the annotations are set - move it to another class. 2. It
     * could result in a large number of operations being generated at the same
     * time by multiple clients 3. It will cause the handleAnnotationChange
     * method to get called, which will re-register the paint functions we just
     * cleaned up.
     *
     * if (data.address.equals(currentUserAddress)) { document.setAnnotation(0,
     * size, CaretAnnotationConstants.USER_DATA + data.sessionId, null); if
     * (rangeStart >= 0) { assert rangeEnd > rangeStart;
     * document.setAnnotation(rangeStart, rangeEnd,
     * CaretAnnotationConstants.USER_RANGE + data.sessionId, null); } if
     * (hotSpot >= 0) { document.setAnnotation(hotSpot, size,
     * CaretAnnotationConstants.USER_END + data.sessionId, null); } }
     */

    if (hotSpot >= 0) {
      AnnotationPainter.maybeScheduleRepaint((DocumentContext) bundle, rangeStart, rangeEnd);
    }
  }



  private final StringMap<CaretData> carets = CollectionUtils.createStringMap();

  private final Queue<CaretData> caretsToExpire = new PriorityQueue<CaretData>(10,

      new Comparator<CaretData>() {

        @Override
        public int compare(CaretData o1, CaretData o2) {
          return (int) Math.signum(o1.originallyScheduledExpiry - o2.originallyScheduledExpiry);
        }
      });


  @Override
  public <N, E extends N, T extends N> void handleAnnotationChange(DocumentContext<N, E, T> bundle,
      int start, int end, String key, Object newValue) {

    // we can't render carets if we can get participants profile
    if (profileManager == null)
      return;

    boolean isCurrentUser = key.endsWith("/" + profileManager.getCurrentSessionId());

    if (key.startsWith(CaretAnnotationConstants.USER_RANGE) && !isCurrentUser) {

      // The selection

      painterRegistry.registerPaintFunction(
          CollectionUtils.newStringSet(key), spreadFunc);
      painterRegistry.getPainter().scheduleRepaint(bundle, start, end);

    } else if (CaretAnnotationConstants.isEndKey(key) && !isCurrentUser) {

      updateCaretData(CaretAnnotationConstants.endSuffix(key), (String) newValue, bundle,
          isCurrentUser);

      // The caret

      painterRegistry.registerBoundaryFunction(
          CollectionUtils.newStringSet(key), boundaryFunc);

      painterRegistry.getPainter().scheduleRepaint(bundle, start, start + 1);

      if (end == bundle.document().size()) {
        end--;
      }
      painterRegistry.getPainter().scheduleRepaint(bundle, end, end + 1);
    }

  }

  public void clear() {
    caretsToExpire.clear();
    carets.clear();
  }

  //
  // Profile events.
  //

  @Override
  public void onUpdated(final Profile profile) {

    final String profileAddress = profile.getAddress();

    carets.each(new ProcV<CaretData>() {

      @Override
      public void apply(String s, CaretData value) {
        if (value.address.equals(profileAddress) && !value.isStale()) {
          value.updateProfile(profile);
        }
      }

    });

  }

  @Override
  public void onOffline(ProfileSession profile) {

  }

  @Override
  public void onOnline(ProfileSession profile) {

  }

  @Override
  public void onLoaded(ProfileSession profile) {

  }

}
