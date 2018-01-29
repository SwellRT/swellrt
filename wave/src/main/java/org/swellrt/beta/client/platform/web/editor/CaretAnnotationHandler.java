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

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;

import org.waveprotocol.wave.client.common.util.RgbColor;
import org.waveprotocol.wave.client.common.util.RgbColorUtil;
import org.waveprotocol.wave.client.doodad.selection.CaretMarkerRenderer;
import org.waveprotocol.wave.client.doodad.selection.CaretView;
import org.waveprotocol.wave.client.doodad.selection.CaretView.CaretViewFactory;
import org.waveprotocol.wave.client.doodad.selection.CaretWidget;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.BoundaryFunction;
import org.waveprotocol.wave.client.editor.content.AnnotationPainter.PaintFunction;
import org.waveprotocol.wave.client.editor.content.ContentElement;
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
import org.waveprotocol.wave.model.util.StringMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.gwt.dom.client.Element;

/**
 * Deals with rendering of remote carets (and selections).
 * <p>
 * This is a modified version of legacy SelectionAnnotationHandler class.
 * <p>
 * {@link CaretManager} sets following local annotations and this handler uses
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
 * session (e.g. one value per browser tab).
 * <p>
 * <br>
 * Annotation values are {@link CaretInfo} objects. They contain all the
 * required info about the user to render carets.
 * <p>
 * <br>
 * TODO: write unit test based on SelectionAnnotationHandlerTest
 *
 * @author danilatos@google.com (Daniel Danilatos)
 * @author pablojan@gmail.com (Pablo Ojanguren)
 */
public class CaretAnnotationHandler implements AnnotationMutationHandler {
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

  private final CaretViewFactory caretViewFactory;


  /**
   * Information required for book-keeping and managing the logic of rendering
   * each session's caret marker and selection.
   */
  class Caret {

    /** The session of this caret */
    private final String sessionId;

    /** UI for rendering the marker associated with this user session */
    private final CaretView caretView;

    /** All the necessary caret and its session */
    private CaretInfo info;

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


    Caret(String sessionId) {
      this.sessionId = sessionId;
      this.caretView = new CaretWidget();
    }

    private void updateView() {
      this.caretView.setColor(this.info.getSession().getColor());
      this.caretView.setName(this.info.getSession().getName());
    }

    public void update(CaretInfo info) {
      this.info = info;
      this.updateView();
    }

    public void compositionStateUpdated(String newState) {
      caretView.setCompositionState(newState);
    }

    public boolean isStale() {
      return scheduler.currentTimeMillis() > expiry;
    }

    public RgbColor getColour() {
      return info.getSession().getColor();
    }

    public void updateExpiryTime() {
      double lastActivityTime = scheduler.currentTimeMillis();
      expiry = lastActivityTime + STALE_CARET_TIMEOUT_MS;
      originallyScheduledExpiry = expiry;
    }

    public void renderAt(Element e) {
      caretView.attachToParent(e);
    }

  }


  private RgbColor getSelectionColor(List<RgbColor> regionColors) {
    RgbColor lighter = RgbColorUtil
        .average(Arrays.asList(RgbColorUtil.average(regionColors), RgbColor.WHITE));
    return lighter;
  }


  private final PaintFunction spreadFunc = new PaintFunction() {

    public Map<String, String> apply(Map<String, Object> from, boolean isEditing) {

      // discover which sessions have highlighted this range:

      List<RgbColor> colors = CollectionUtils.newArrayList();

      for (Map.Entry<String, Object> entry : from.entrySet()) {
        if (CaretAnnotationConstants.isRangeKey(entry.getKey())) {
          CaretInfo caretInfo = (CaretInfo) entry.getValue();
          colors.add(caretInfo.getSession().getColor());
        }
      }

      // combine them together and highlight the range accordingly:
      if (!colors.isEmpty()) {
        return Collections.singletonMap("backgroundColor", getSelectionColor(colors).getCssColor());
      } else {
        return Collections.emptyMap();
      }
    }
  };


  private final BoundaryFunction boundaryFunc = new BoundaryFunction() {

    public <N, E extends N, T extends N> E apply(LocalDocument<N, E, T> localDoc, E parent,
        N nodeAfter, Map<String, Object> before, Map<String, Object> after, boolean isEditing) {

      E ret = null;
      E caretElement = null;

      for (Map.Entry<String, Object> entry : after.entrySet()) {

        if (CaretAnnotationConstants.isEndKey(entry.getKey())) {

          String sessionId = CaretAnnotationConstants.endSuffix(entry.getKey());
          Caret caret = getCaretIfValid(sessionId);
          if (caret == null) {
            continue;
          }

          if (caretElement == null) {
            // if needed, first create a simple container to put caret DOMs into:
            ret = localDoc.transparentCreate(CaretMarkerRenderer.FULL_TAGNAME,
                Collections.<String, String> emptyMap(), parent, nodeAfter);
            caretElement = ret;
          }

          // attach caret view to the anchor DOM element
          caret.renderAt(((ContentElement) caretElement).getImplNodelet());

        }
      }

      return ret;
    }
  };

  private Caret getCaretIfValid(String sessionId) {
    Caret caretData = carets.get(sessionId);
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
   * Update a caret so it can be rendered with fresh data.
   *
   * @param caretSessionId
   *          the session id for the caret
   * @param caretInfo
   *          optional (nullable) the caret data
   * @param doc
   *          the document where caret is rendered
   */
  private void updateCaret(String caretSessionId, CaretInfo caretInfo,
      DocumentContext<?, ?, ?> doc) {

    // Access directly from the map because the high level getter filters stale
    // carets, and this could result in memory leaks.
    Caret caret = carets.get(caretSessionId);

    if (caret == null) {
      caret = new Caret(caretSessionId);
      carets.put(caretSessionId, caret);
    }

    if (caretInfo != null)
      caret.update(caretInfo);

    scheduleCaretExpiration(caret, doc);

  }


  private void scheduleCaretExpiration(Caret caret, DocumentContext<?, ?, ?> doc) {

    caret.updateExpiryTime();

    if (caret.bundle == null) {
      caretsToExpire.add(caret);
    }

    caret.bundle = doc;

    if (!scheduler.isScheduled(expiryTask)) {
      scheduler.scheduleRepeating(expiryTask, MINIMUM_STALE_CHECK_GAP_MS,
      MINIMUM_STALE_CHECK_GAP_MS);
    }

  }



  private final Scheduler.IncrementalTask expiryTask = new Scheduler.IncrementalTask() {

    @Override
    public boolean execute() {

      while (!caretsToExpire.isEmpty()) {

        Caret caret = caretsToExpire.element();

        if (caret.originallyScheduledExpiry > scheduler.currentTimeMillis()) {
          return true;
        }

        caretsToExpire.remove();

        if (caret.expiry > scheduler.currentTimeMillis()) {

          caret.originallyScheduledExpiry = caret.expiry;
          caretsToExpire.add(caret);

        } else {
          expireCaret(caret);
        }
      }

      return false;
    }
  };

  /**
   * Cleanup any state associated with expired selection annotation data
   *
   * @param caret
   *          expired data
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  private void expireCaret(Caret caret) {

    if (caret.bundle == null)
      return;

    String sessionId = caret.info.getSession().getSessionId();
    DocumentContext<?, ?, ?> bundle = caret.bundle;
    MutableAnnotationSet.Local annotations = bundle.localAnnotations();

    caret.bundle = null;

    painterRegistry.unregisterBoundaryFunction(
        CollectionUtils
            .newStringSet(CaretAnnotationConstants.endKey(sessionId)),
        boundaryFunc);

    painterRegistry.unregisterPaintFunction(
        CollectionUtils.newStringSet(CaretAnnotationConstants.rangeKey(sessionId)),
        spreadFunc);

    int size = bundle.document().size();
    int rangeStart = annotations.firstAnnotationChange(0, size,
        CaretAnnotationConstants.rangeKey(sessionId), null);
    int rangeEnd = annotations.lastAnnotationChange(0, size,
        CaretAnnotationConstants.rangeKey(sessionId), null);
    int hotSpot = annotations.firstAnnotationChange(0, size,
        CaretAnnotationConstants.endKey(sessionId), null);

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



  private final StringMap<Caret> carets = CollectionUtils.createStringMap();

  private final Queue<Caret> caretsToExpire = new PriorityQueue<Caret>(10,

      new Comparator<Caret>() {

        @Override
        public int compare(Caret o1, Caret o2) {
          return (int) Math.signum(o1.originallyScheduledExpiry - o2.originallyScheduledExpiry);
        }
      });


  @Override
  public <N, E extends N, T extends N> void handleAnnotationChange(DocumentContext<N, E, T> doc,
      int start, int end, String key, Object newValue) {

    String localSessionId = SEditorStatics.getSSession().get().getSessionId();
    boolean isLocalSession = key.endsWith("/" + localSessionId);

    if (isLocalSession)
      return;

    if (CaretAnnotationConstants.isRangeKey(key)) {

      // The selection

      painterRegistry.registerPaintFunction(
          CollectionUtils.newStringSet(key), spreadFunc);
      painterRegistry.getPainter().scheduleRepaint(doc, start, end);

    } else if (CaretAnnotationConstants.isEndKey(key)) {

      // The caret

      // newValue can be null, don't use it to get the caret's session id
      CaretInfo caretInfo = newValue != null ? (CaretInfo) newValue : null;

      // always get the session id from the annotation key
      String caretSessionId = CaretAnnotationConstants.endSuffix(key);

      updateCaret(caretSessionId, caretInfo, doc);

      painterRegistry.registerBoundaryFunction(
          CollectionUtils.newStringSet(key), boundaryFunc);

      painterRegistry.getPainter().scheduleRepaint(doc, start, start + 1);

      if (end == doc.document().size()) {
        end--;
      }
      painterRegistry.getPainter().scheduleRepaint(doc, end, end + 1);
    }

  }

  public void clear() {
    caretsToExpire.clear();
    carets.clear();
  }


}
