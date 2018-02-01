package org.waveprotocol.wave.client.editor.playback;

import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.wave.DocOpTracker;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

/**
 *
 * A document that can playback a history of changes provided as
 * {@link DocHistory}<br>
 * <p>
 * <br>
 * Each {@link Version} represents a set of contiguous operations performed by
 * the same participant in certain interval of time. Initial version is always
 * 0. <br>
 * <p>
 * The navigation methods of this class, control the version to be rendered.
 * Highlighting of incremental diffs between versions is available.
 *
 * @author pablojan@gmail.com
 *
 */
public class PlaybackDocument {

  public class Version {

    public double version;
    public String participant;
    public double timestamp;
    public int numOfChanges;

  }

  /** history of doc ops */
  private final DocHistory history;

  /** current iterator of doc ops */
  private DocHistory.Iterator deltaIterator;

  /** Let's put in this cache any doc op sent to the content document */
  private final DocOpTracker docOpCache;

  /** The actual content document */
  private ContentDocument doc;

  /** A filtered sink for the content document, to add diff marks as annotations */
  private DiffHighlightingFilter diffFilter;

  /** the version the content document is at. */
  private long currentVersion = 0;

  /** Enable or disable the diff filter */
  private boolean useDiffFilter = false;


  public PlaybackDocument(Registries registries, DocumentSchema schema, DocHistory history,
      DocOpTracker docOpCache) {
    this.doc = new ContentDocument(schema);
    this.doc.setRegistries(registries);
    this.history = history;
    this.diffFilter = new DiffHighlightingFilter(this.doc.getDiffTarget(), docOpCache);
    this.docOpCache = docOpCache;

    this.deltaIterator = history.iterator();
  }

  public ContentDocument getDocument() {
    return doc;
  }

  public void enableDiffs(boolean isOn) {
    useDiffFilter = isOn;
  }

  private void consume(DocHistory.Delta delta) {
    delta.ops.forEach(op -> {

      docOpCache.add(op, delta.context);

      if (useDiffFilter) {
        try {
          diffFilter.consume(op);
        } catch (OperationException e) {
          throw new IllegalStateException(e);
        }
      } else
        doc.consume(op);
    });
  }



  private void resetContent() {
    doc.getMutableDoc().deleteRange(0, doc.getMutableDoc().size());
  }

  private void resetAnnotations() {

    doc.getLocalAnnotations().knownKeys().each(new Proc() {

      @Override
      public void apply(String annotationKey) {
        Annotations.guardedResetAnnotation(doc.getLocalAnnotations(), 0, doc.getMutableDoc().size(),
            annotationKey, null);
      }

    });

  }

  public void reset() {
    deltaIterator = history.iterator();
    resetAnnotations();
    resetContent();
  }


  /** Render the document in its next version. */
  public void renderNext() {

    if (!deltaIterator.hasNext())
      return;

    consume(deltaIterator.next());

  }

  /** Render the document in its previous version. */
  public void renderPrev() {

    if (deltaIterator.current() == null)
      return;

    consume(deltaIterator.current().invert());

    deltaIterator.prev();

  }


  /**
   * Render the document at the specific version.
   */
  public void render(Version version) {

  }

  /**
   * Render the document at a specific version, highlighting differences between
   * the provided versions.
   *
   * @param from
   * @param to
   */
  public void renderDiff(Version from, Version to) {

  }

  /**
   * Query version history.
   *
   * @param from first version number to return
   * @param count number of versions to return
   * @return array of available {@link Versions}
   */
  public Version[] getVersions(double from, int count) {
    return null;
  }

  /**
   * Query version history filtering by a period of time.
   *
   * @param from first version number to return
   * @param timeSpan only returns version in a span after the first version's time.
   * @return array of available {@link Versions}
   */
  public Version[] getVersionsByTime(double from, double timeSpan) {
    return null;
  }


}
