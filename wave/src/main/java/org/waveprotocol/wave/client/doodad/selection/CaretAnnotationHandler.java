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

package org.waveprotocol.wave.client.doodad.selection;

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
import org.waveprotocol.wave.client.doodad.selection.CaretView.CaretViewFactory;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.BoundaryFunction;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.PainterRegistry;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.scheduler.Scheduler;
import org.waveprotocol.wave.client.scheduler.SchedulerInstance;
import org.waveprotocol.wave.client.scheduler.TimerService;
import org.waveprotocol.wave.model.conversation.AnnotationConstants;
import org.waveprotocol.wave.model.document.AnnotationMutationHandler;
import org.waveprotocol.wave.model.document.MutableDocument;
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
 * This class relies on a {@link ProfileManager} instance to render properly carets.
 * <p>
 * Modified version of original SelectionAnnotationHandler class.
 * <p>
 * See {@link SelectionExtractor} to know how following annotatations are generated:
 * <p>
 * Currently, a user's selection is defined as a group of two or three annotations.
 * <br>
 *  - Data annotation, with the prefix {@link #AnnotationConstants.USER_DATA}
 *    This annotation always covers the entire document.
 *    Its value is of the form "address,timestamp[,compositionstate]" where address is
 *    the user's id, timestamp is the number of milliseconds since the Epoch, UTC.
 *    An optional composition state may also be included, for indicating uncommitted
 *    IME composition text.
 *  - Hotspot annotation, with the prefix {@link #AnnotationConstants.USER_END}
 *    This annotation starts from where the user's blinking caret would be, and
 *    extends to the end of the document.
 *    Its value is their address
 *  - Range annotation, with the prefix {@link #AnnotationConstants.USER_RANGE}
 *    This annotation extends over the user's selected range. if their selection
 *    is collapsed, this annotation is not present.
 *    Its value is their address.
 *
 * Each key is suffixed with a globally unique value identifying the current session
 * (e.g. one value per browser tab).
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
      registerHandler(AnnotationConstants.USER_PREFIX, selection);
    
    return selection;
  }

  /**
   * Handy method for getting the full annotation key, given a session id
   *
   * Session id does not have to be THE session id - it can just be any
   * globally unique key for the current client.
   *
   * @param sessionId
   * @return full annotation key
   */
  public static String rangeKey(String sessionId) {
    return AnnotationConstants.USER_RANGE + sessionId;
  }

  public static String endKey(String sessionId) {
    return AnnotationConstants.USER_END + sessionId;
  }

  public static String dataKey(String sessionId) {
    return AnnotationConstants.USER_DATA + sessionId;
  }

  public static String rangeSuffix(String rangeKey) {
    return rangeKey.substring(AnnotationConstants.USER_RANGE.length());
  }

  public static String endSuffix(String endKey) {
    return endKey.substring(AnnotationConstants.USER_END.length());
  }

  public static String dataSuffix(String dataKey) {
    return dataKey.substring(AnnotationConstants.USER_DATA.length());
  }

  private final PainterRegistry painterRegistry;

  private final TimerService scheduler;

  // Used for getting profiles, which are needed for choosing names.
  private ProfileManager profileManager;

  private final StringMap<String> highlightCache = CollectionUtils.createStringMap();

  private final CaretViewFactory markerFactory;

 
  /**
   * Information required for book-keeping and managing the logic of rendering
   * each session's caret marker and selection.
   */
  class CaretData {
    
    /** UI for rendering the marker associated with this user session */
    private final CaretView ui;

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

    CaretData(CaretView ui, ProfileSession profileSession, String address, String sessionId) {
      
      if (sessions.containsKey(sessionId)) {
        throw new IllegalArgumentException("Session data already exists");
      }

      this.address = address;

      sessions.put(sessionId, this);

      this.profileSession = profileSession;
      
      this.ui = ui;
      this.sessionId = sessionId;
      this.color = profileSession.getColor();
      this.ui.setColor(this.color);
      
      updateProfile(profileSession.getProfile());
    }

    public void updateProfile(Profile profile) {      
      this.name = profile.getName();
      ui.setName(name);      
    }

    public void compositionStateUpdated(String newState) {
      ui.setCompositionState(newState);
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
        
        if (entry.getKey().startsWith(AnnotationConstants.USER_RANGE)) {
        
          String sessionId = endSuffix(entry.getKey());
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
        
        if (entry.getKey().startsWith(AnnotationConstants.USER_END)) {
          // get the user's address:
          String address = (String) entry.getValue();
          if (address == null) {
            continue;
          }

          // get the session ID:
          String sessionId = endSuffix(entry.getKey());
          CaretData data = getActiveCaretData(sessionId);
          if (data == null) {
            continue;
          }

          // if needed, first create a simple container to put caret DOMs into:
          if (usersContainer == null) {
            ret = localDoc.transparentCreate(
                CaretMarkerRenderer.FULL_TAGNAME, Collections.<String, String>emptyMap(),
                parent, nodeAfter);
            usersContainer = ret;

          }

          markerFactory.setMarker(usersContainer, data.ui);
        }
      }
      return ret;
    }
  };

  private CaretData getActiveCaretData(String sessionId) {
    CaretData data = sessions.get(sessionId);
    return data != null && !data.isStale() ? data : null;
  }

  /** Seed the annotation handler with all required config objects. */
  public CaretAnnotationHandler(PainterRegistry registry,      
      TimerService timer, 
      CaretViewFactory markerFactory) {
    
    this.painterRegistry = registry;
    this.scheduler = timer;
    this.markerFactory = markerFactory;
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
    
    sessions.clear();
    expiries.clear();
    
    this.profileManager = profileManager;
    
    if (profileManager != null) {    
      this.profileManager.addListener(this);    
    }
  }
  
  
  private void updateCaretData(String sessionId, String value, DocumentContext<?, ?, ?> doc, boolean isCurrentUser) {

    String[] components = value.split(",");
    if (components.length < 2) {
      return; // invalid input
    }

    double timeStamp;
    
    try {
      // split into session address and time
      timeStamp = Double.parseDouble(components[1]);
    } catch (NumberFormatException nfe) {
      return; // invalid input
    }

    String address = components[0];
        
    ParticipantId participantId;
    try {
      participantId = ParticipantId.of(address);
    } catch (InvalidParticipantAddress e) {
      return;
    }
    
    
    String name = components.length >= 4 ? components[3] : null;
    
    // Access directly from the map because the high level getter filters stale carets,
    // and this could result in memory leaks.
    CaretData data = sessions.get(sessionId);

    
    
    if (data == null) {
      ProfileSession profile = profileManager.getSession(sessionId, participantId);
      data = new CaretData(markerFactory.createMarker(), profile, address, sessionId);
      if (name != null)
        data.getProfileSession().getProfile().setName(name);
    }
    
    // Avoid update this for the current user, it is not necessary
    if (!isCurrentUser) {
    
      double lastActivityTime = Math.min(timeStamp, scheduler.currentTimeMillis());
      double expiry = lastActivityTime + STALE_CARET_TIMEOUT_MS;
      activate(data, expiry, doc);
  
      data.compositionStateUpdated(components.length >= 3 ? components[2] : "");
  
      data.getProfileSession().trackActivity(lastActivityTime);
      
      // update the name of remote anonymous users
      if (name != null) {
        data.getProfileSession().getProfile().setName(name);
      }
    }
  }

  
  private void activate(CaretData data, double expiry, DocumentContext<?, ?, ?> doc) {
     
    data.expiry = expiry;
    data.originallyScheduledExpiry = expiry;

    if (data.bundle == null) {
      expiries.add(data);
    }

    data.bundle = doc;

    if (!scheduler.isScheduled(expiryTask)) {
      scheduler.scheduleRepeating(expiryTask, MINIMUM_STALE_CHECK_GAP_MS,
          MINIMUM_STALE_CHECK_GAP_MS);
    }
  }
  
  
  
  private final Scheduler.IncrementalTask expiryTask = new Scheduler.IncrementalTask() {
    
    @Override
    public boolean execute() {
      
      while (!expiries.isEmpty()) {
      
        CaretData data = expiries.element();

        if (data.originallyScheduledExpiry > scheduler.currentTimeMillis()) {
          return true;
        }

        expiries.remove();

        if (data.expiry > scheduler.currentTimeMillis()) {
        
          data.originallyScheduledExpiry = data.expiry;
          expiries.add(data);
        
        } else {
          expire(data);
        }
      }

      return false;
    }
  };

  /**
   * Cleanup any state associated with expired selection annotation data
   *
   * @param data expired data
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void expire(CaretData data) {
    
    DocumentContext<?, ?, ?> bundle = data.bundle;
    MutableDocument<?, ?, ?> document = bundle.document();

    data.bundle = null;
    
    painterRegistry.unregisterBoundaryFunction(
        CollectionUtils.newStringSet(AnnotationConstants.USER_END + data.sessionId), boundaryFunc);
    
    painterRegistry.unregisterPaintFunction(
        CollectionUtils.newStringSet(AnnotationConstants.USER_RANGE + data.sessionId), spreadFunc);

    int size = document.size();
    int rangeStart = document.firstAnnotationChange(0, size, AnnotationConstants.USER_RANGE + data.sessionId, null);
    int rangeEnd = document.lastAnnotationChange(0, size, AnnotationConstants.USER_RANGE + data.sessionId, null);
    int hotSpot = document.firstAnnotationChange(0, size, AnnotationConstants.USER_END + data.sessionId, null);

    if (rangeStart == -1) {
      rangeStart = rangeEnd = hotSpot;
    }

    /*
    TODO(danilatos): Enable this code. Problems to resolve:
    1. It causes mutations just from the renderer. Rather the cleanup is best done
       in the same place the annotations are set - move it to another class.
    2. It could result in a large number of operations being generated at the same time
       by multiple clients
    3. It will cause the handleAnnotationChange method to get called, which will
       re-register the paint functions we just cleaned up.

    if (data.address.equals(currentUserAddress)) {
      document.setAnnotation(0, size, AnnotationConstants.USER_DATA + data.sessionId, null);
      if (rangeStart >= 0) {
        assert rangeEnd > rangeStart;
        document.setAnnotation(rangeStart, rangeEnd, AnnotationConstants.USER_RANGE + data.sessionId, null);
      }
      if (hotSpot >= 0) {
        document.setAnnotation(hotSpot, size, AnnotationConstants.USER_END + data.sessionId, null);
      }
    }
    */

    if (hotSpot >= 0) {
      AnnotationPainter.maybeScheduleRepaint((DocumentContext) bundle, rangeStart, rangeEnd);
    }
  }



  private final StringMap<CaretData> sessions = CollectionUtils.createStringMap();

  private final Queue<CaretData> expiries = new PriorityQueue<CaretData>(10,
      
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

    if (key.startsWith(AnnotationConstants.USER_DATA) && newValue != null) {
      
      // User activity
      updateCaretData(dataSuffix(key), (String) newValue, bundle, isCurrentUser);     
      
    } else if (key.startsWith(AnnotationConstants.USER_RANGE) && !isCurrentUser) {
      
      // The selection
      
      painterRegistry.registerPaintFunction(
          CollectionUtils.newStringSet(key), spreadFunc);
      painterRegistry.getPainter().scheduleRepaint(bundle, start, end);

    } else if (key.startsWith(AnnotationConstants.USER_END) && !isCurrentUser) {
      
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
    expiries.clear();
    sessions.clear();
  }

  //
  // Profile events.
  //

  @Override
  public void onUpdated(final Profile profile) {
    
    final String profileAddress = profile.getAddress();
    
    sessions.each(new ProcV<CaretData>() {
      
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
