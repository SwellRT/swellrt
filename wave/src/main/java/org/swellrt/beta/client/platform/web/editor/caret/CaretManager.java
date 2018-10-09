package org.swellrt.beta.client.platform.web.editor.caret;

import org.swellrt.beta.common.SException;
import org.swellrt.beta.model.SEvent;
import org.swellrt.beta.model.SMap;
import org.swellrt.beta.model.SMutationHandler;
import org.swellrt.beta.model.SPrimitive;
import org.swellrt.beta.model.presence.SSessionManager;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent;
import org.waveprotocol.wave.client.editor.EditorUpdateEvent.EditorUpdateListener;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.model.document.MutableAnnotationSet;
import org.waveprotocol.wave.model.document.util.FocusedRange;

import com.google.gwt.core.client.GWT;

/**
 * Carets positions are stored in a transient map (in the transient wavelet) by
 * this class when editor's selection changes.
 * <p>
 * <br>
 * When transient map is updated, a local annotation is set to represent the
 * caret within the document. This annotation is of type
 * {@link CaretAnnotationConstants.USER_END}.
 * <p>
 * <br>
 * Carets are eventually rendered by annotation painters configured in
 * {@link CaretAnnotationHandler}.
 * <p>
 * <br>
 * Currently only live carets are managed. Live selections are not activated as
 * long as they don't seem a practical feature. See
 * {@link CaretAnnotationConstants.USER_RANGE} for more info.
 * <p>
 * <br>
 * This manager controls the rate of editor events passed as caret updates in
 * order to avoid overflow of deltas in transient storage.
 *
 */
public class CaretManager implements EditorUpdateListener {


  private final SMutationHandler caretsListener = new SMutationHandler() {

    @Override
    public boolean exec(SEvent e) {

      if (e.isAddEvent() || e.isUpdateEvent()) {

        CaretInfo caretInfo = CaretInfo.of(((SPrimitive) e.getNode()).asSJson());

        if (!caretInfo.getSession().getSessionId().equals(session.get().getSessionId()))
          updateAnnotations(caretInfo);
      }

      return false;
    }
  };

  private final SSessionManager session;
  private final SMap carets;
  private final Editor editor;

  public CaretManager(SSessionManager session, SMap carets,
      Editor editor) {

    this.session = session;
    this.carets = carets;
    this.editor = editor;

  }

  public void start() {
    editor.addUpdateListener(this);
    try {
      carets.listen(caretsListener);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }
  }

  public void stop() {
    try {
      carets.unlisten(caretsListener);
    } catch (SException e) {
      throw new IllegalStateException(e);
    }
    editor.removeUpdateListener(this);
  }

  /**
   * Update local annotations in the document according to provided caret info.
   *
   * @param caretInfo
   *          info about the caret to update annotations.
   */
  private void updateAnnotations(CaretInfo caretInfo) {

    ContentDocument content = editor.getContent();
    MutableAnnotationSet.Local annotations = content.getLocalAnnotations();

    updateCaretAnnotation(content, annotations, caretInfo);

    // updateSelectionAnnotation(content, annotations, caretInfo);
  }

  /**
   * Update the annotation that marks the caret position
   * ({@link CaretAnnotationConstants.USER_END}) for the session of a
   * {@link CaretInfo}
   *
   * @param content
   * @param annotations
   * @param caretInfo
   */
  private void updateCaretAnnotation(ContentDocument content,
      MutableAnnotationSet.Local annotations, CaretInfo caretInfo) {

    String key = CaretAnnotationConstants.endKey(caretInfo.getSession().getSessionId());
    Object value = caretInfo;

    int size = content.getMutableDoc().size();
    int currentFocusPos = annotations.firstAnnotationChange(0, size, key, null);
    int newFocusPos = caretInfo.getPosition();

    try {

      if (currentFocusPos == -1) {
        // New USER_END annotation
        annotations.setAnnotation(newFocusPos, size, key, value);

      } else {
        // Update annotation

        if (newFocusPos < currentFocusPos) {
          // New caret is before the current one. Extend the annotation.
          annotations.setAnnotation(newFocusPos, currentFocusPos, key, value);

        } else if (newFocusPos > currentFocusPos) {
          // New caret is after current, remove annotation in before the caret
          annotations.setAnnotation(currentFocusPos, newFocusPos, key, null);
        }

      }

    } catch (Exception e) {
      GWT.log("Exception setting caret annotation", e);
    }

  }

  /**
   * Update the annotation that marks the text selected
   * ({@link CaretAnnotationConstants.USER_RANGE}) by the provided caret info.
   *
   * TODO tbc
   *
   * @param content
   * @param annotations
   * @param caretInfo
   */
  @SuppressWarnings("unused")
  private void updateSelectionAnnotation(ContentDocument content,
      MutableAnnotationSet.Local annotations, CaretInfo caretInfo) {

    String rangeKey = CaretAnnotationConstants.rangeKey(caretInfo.getSession().getSessionId());
    Object value = caretInfo;

    int size = content.getMutableDoc().size();
    int currentStart = annotations.firstAnnotationChange(0, size, rangeKey, null);
    int currentEnd = annotations.lastAnnotationChange(0, size, rangeKey, null);

    // TODO to be completed

  }

  private EditorUpdateEvent lastEditorEvent = null;
  private long lastEditorEventTime = 0;

  /**
   * don't trigger a new caret update if last editor event happened before this
   * amount of time
   */
  private int caretUpdateRateMs = 2000;

  @Override
  public void onUpdate(EditorUpdateEvent event) {

    lastEditorEvent = event;
    long now = System.currentTimeMillis();
    if (now - lastEditorEventTime > caretUpdateRateMs) {
      doCaretUpdate();
    } else {
      // TBC schedule a task to update caret if a last event exists
    }
    lastEditorEventTime = now;

  }

  private void doCaretUpdate() {

    if (lastEditorEvent == null)
      return;

    EditorUpdateEvent event = lastEditorEvent;

    FocusedRange selection = event.context().getSelectionHelper().getSelectionRange();

    if (selection != null) {

      String compositionState = event.context().getImeCompositionState();
      int caretPos = selection.asRange().getStart();
      CaretInfo caretInfo = new CaretInfo(session.get(), System.currentTimeMillis(), caretPos,
          compositionState);

      try {
        carets.put(session.get().getSessionId(), caretInfo.toSJson());
      } catch (SException e) {
        throw new IllegalStateException(e);
      }
    }

    lastEditorEvent = null;
    lastEditorEventTime = 0;
  }

}
