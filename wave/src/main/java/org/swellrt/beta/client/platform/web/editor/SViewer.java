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
  private DocHistory.Iterator historyIterator;
  private PlaybackDocument playbackDoc;

  protected SViewer(Element element) {
    this.element = element;
  }


  private void setupPlaybackDoc() {
    DocHistory docHistory = text.getDocHistory();
    this.playbackDoc = new PlaybackDocument(docHistory);
    this.historyIterator = playbackDoc.getHistoryIterator();
  }

  public void attachDocView() {
    if (playbackDoc != null) {
      element.appendChild(playbackDoc.getElement());
    }
  }

  private void refreshDocView() {
    deattachDocView();
    attachDocView();
  }

  public void deattachDocView() {
    if (element.getFirstChild() != null) {
      element.getFirstChild().removeFromParent();
    }
  }


  private void cleanUp() {
    if (this.text != null) {
      deattachDocView();
      this.historyIterator = null;
      this.playbackDoc = null;
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

    cleanUp();

    this.text = text;
    setupPlaybackDoc();
    attachDocView();

  }

  /** Render a specific revision or the most recent */
  public void render(@JsOptional DocRevision revision) {
    Preconditions.checkNotNull(playbackDoc, "Viewer doesn't have an attached SText");
    if (revision == null) {
      historyIterator.reset();
      historyIterator.prev(rev -> {
        playbackDoc.render(rev, doc -> {
          refreshDocView();
        });

      });
    } else {
      playbackDoc.render(revision, doc -> {
        refreshDocView();
      });
      refreshDocView();
    }
  }

  /** Render diff between revisions */
  public void renderDiff(DocRevision baseRevision, DocRevision targetRevision) {
    Preconditions.checkNotNull(playbackDoc, "Viewer doesn't have an attached SText");
    Preconditions.checkNotNull(baseRevision, "Missing base revision");
    Preconditions.checkNotNull(targetRevision, "Missing target revision");
    playbackDoc.renderDiff(baseRevision, targetRevision, doc -> {
      refreshDocView();
    });
  }

  public DocHistory.Iterator getHistoryIterator() {
    Preconditions.checkNotNull(playbackDoc, "Viewer doesn't have an attached SText");
    return playbackDoc.getHistoryIterator();
  }

  /**
   * Clear the viewer, clearing the display of a already set text.
   */
  public void unset() {
    cleanUp();
  }

}
