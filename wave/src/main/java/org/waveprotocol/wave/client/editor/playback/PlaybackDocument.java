package org.waveprotocol.wave.client.editor.playback;

import java.util.List;
import java.util.Optional;

import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.playback.DocHistory.Delta;
import org.waveprotocol.wave.client.editor.playback.DocHistory.RevCriteria;
import org.waveprotocol.wave.client.editor.playback.DocHistory.Revision;
import org.waveprotocol.wave.client.wave.DocOpTracker;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

/**
 *
 * A document that can playback document's history of changes. <br>
 * <p>
 * A playback document can replay document states iterating by each single delta
 * or by sets of deltas grouped by author or a period of time.
 * </p>
 * <br>
 * <p>
 * Document diffs can be rendered.
 * </p>
 * <br>
 * <p>
 * The initial state is the empty document. {@code PlaybackDocuments} are not
 * intended to be edited.
 * </p>
 *
 * *
 *
 * @author pablojan@apache.org
 *
 */
public class PlaybackDocument {


  private final DocHistory history;

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
  }

  public ContentDocument getDocument() {
    return doc;
  }

  public void renderDiffs(boolean isOn) {
    useDiffFilter = isOn;
  }

  private void consume(Delta delta) {
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


  public List<Revision> queryRevisions(RevCriteria criteria, long startVersion,
      int numOfRevisions) {
    // TODO Auto-generated method stub
    return null;
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
    history.reset();
    resetAnnotations();
    resetContent();
  }

  public void nextDelta() {

    Optional<Delta> opDeltas = history.nextDelta();

    if (!opDeltas.isPresent())
      return;

    currentVersion++;
    consume(opDeltas.get());
  }

  public void prevDelta() {

    Optional<Delta> opDeltas = history.prevDelta();

    if (!opDeltas.isPresent())
      return;

    currentVersion--;
    consume(opDeltas.get().invert());
  }

  public void toDelta(long version) {

    boolean backwards = version < currentVersion;

    history.toDelta(version).forEach(delta -> {

      if (backwards) {
        currentVersion--;
        consume(delta.invert());
      } else {
        currentVersion++;
        consume(delta);
      }

    });

  }


  public long getLastVersion() {
    return history.getLastVersion();
  }

}
