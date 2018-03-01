package org.swellrt.beta.client.platform.web.editor;

import org.waveprotocol.wave.client.editor.playback.DocHistory;
import org.waveprotocol.wave.client.editor.playback.DocRevision;
import org.waveprotocol.wave.client.editor.playback.PlaybackDocument;
import org.waveprotocol.wave.model.util.CollectionUtils;
import org.waveprotocol.wave.model.util.Preconditions;
import org.waveprotocol.wave.model.util.StringMap;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.DOM;

import jsinterop.annotations.JsOptional;
import jsinterop.annotations.JsType;

/**
 * A viewer is a non-editable but interactive canvas rendering a
 * {@link STextWeb} and any revision of its change history. A single instance
 * can be reused to render different text instances but it is always attached to
 * the same DOM element.
 *
 * @author pablojan@gmail.com
 *
 */
@JsType(namespace = "swell", name = "Viewer")
public class SViewer {

  private static StringMap<SViewer> viewers = CollectionUtils.createStringMap();


  /** create a editor instance of get an existing one */
  public static SViewer get(String elementId) {

    if (viewers.containsKey(elementId)) {
      return viewers.get(elementId);
    }

    Element containerElement = DOM.getElementById(elementId);

    if (containerElement == null) {
      throw new IllegalStateException("Element with id=" + elementId + " not found");
    }

    if (!containerElement.getNodeName().equalsIgnoreCase("div")) {
      throw new IllegalStateException("Element with id=" + elementId + " is not a DIV");
    }

    SViewer editor = new SViewer(containerElement);
    viewers.put(elementId, editor);

    return editor;
  }

  private final Element element;

  private STextWeb text;
  private PlaybackDocument playbackDoc;

  private boolean isViewAttached = false;

  protected SViewer(Element element) {
    this.element = element;
  }


  private void setupPlaybackDoc() {
    DocHistory docHistory = text.getDocHistory();
    this.playbackDoc = new PlaybackDocument(docHistory);
  }

  public void attachDocView() {
    if (isViewAttached)
      return;

    if (playbackDoc != null) {
      element.appendChild(playbackDoc.getElement());
    } else {
      element.appendChild(text.getElement());
    }

    isViewAttached = true;
  }

  private void refreshDocView() {
    deattachDocView();
    attachDocView();
  }

  public void deattachDocView() {
    if (!isViewAttached)
      return;

    if (element.hasChildNodes()) {
      element.removeAllChildren();
    }

    isViewAttached = false;
  }

  private boolean isTextRender() {
    return element.getFirstChild() != null;
  }


  private void clear() {
    if (this.text != null) {

      this.playbackDoc = null;
      deattachDocView();

      this.text = null;
    }
  }

  /**
   * Attach a {@link STextWeb} instance to this viewer. After calling this
   * method no content will be rendered yet, use render() methods like
   * {@link #render(DocRevision)} for that.
   * <p>
   * <br>
   * After setting a text within the Viewer, the effective text's history for
   * the viewer will be the one at that time. New changes in the text after a
   * call to set() will be ignored.
   *
   * @param text
   *          a STextWeb instance
   */
  public void set(STextWeb text) {

    if (text != null && this.text != null && text.equals(this.text))
      return;

    if (text.getDocHistory() != null) {
      clear();
      this.text = text;
      setupPlaybackDoc();
      attachDocView();

    }

  }

  /** Render a specific revision or the most recent */
  public void render(@JsOptional DocRevision revision) {

    if (revision == null) {

      if (playbackDoc != null) {

        DocHistory.Iterator it = playbackDoc.getHistoryIterator();
        it.reset();
        it.prev(rev -> {
          playbackDoc.render(rev, doc -> {
            refreshDocView();
          });

        });

      } else {
        refreshDocView();
      }

    } else {
      checkHasHistory();
      playbackDoc.render(revision, doc -> {
        refreshDocView();
      });
    }
  }

  private void checkHasHistory() {
    Preconditions.checkNotNull(playbackDoc, "The SText doesn't provide a change history.");
  }

  /** Render diff between revisions */
  public void renderDiff(DocRevision baseRevision, DocRevision targetRevision) {
    checkHasHistory();
    Preconditions.checkNotNull(baseRevision, "Missing base revision");
    Preconditions.checkNotNull(targetRevision, "Missing target revision");
    playbackDoc.renderDiff(baseRevision, targetRevision, doc -> {
      refreshDocView();
    });
  }

  /**
   * @return an iterator of the document's history at the revision shown in the
   *         viewer.
   */
  public DocHistory.Iterator getHistoryIterator() {
    checkHasHistory();
    return playbackDoc.getHistoryIterator();
  }

  public void renderNext() {
    checkHasHistory();
    if (isTextRender()) {
      playbackDoc.renderNext(doc -> {
      });
    }
  }

  public void renderPrev() {
    checkHasHistory();
    if (isTextRender()) {
      playbackDoc.renderPrev(doc -> {
      });
    }
  }

  public DocRevision getCurrentDocRevision() {
    checkHasHistory();
    return playbackDoc.getHistoryIterator().current(null);
  }

  /**
   * Clear the viewer, clearing the display of a already set text.
   */
  public void unset() {
    clear();
  }

  public void renderContribs(boolean on) {
    if (text == null)
      return;
    if (on) {
      text.getInteractiveDocument().startShowDiffs();
    } else {
      text.getInteractiveDocument().stopShowDiffs();
    }

  }

}
