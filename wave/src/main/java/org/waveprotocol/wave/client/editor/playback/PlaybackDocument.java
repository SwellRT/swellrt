package org.waveprotocol.wave.client.editor.playback;

import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.wave.DocOpTracker;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

/**
 *
 * A document that can playback a document history {@link DocHistory}<br>
 * <p>
 * <br>
 *
 * @author pablojan@gmail.com
 *
 */
public class PlaybackDocument {

  private final Registries registries;

  private final DocumentSchema schema;

  private final DocHistory history;

  private final DocOpTracker docOpTracker;

  /** The actual content document */
  private ContentDocument doc;

  /** A filtered sink for the content document, to add diff marks as annotations */
  private DiffHighlightingFilter diffFilter;

  /** Enable or disable the diff filter */
  private boolean useDiffFilter = false;

  private DocHistory.Iterator revisionIterator;

  public PlaybackDocument(Registries registries, DocumentSchema schema, DocHistory history,
      DocOpTracker docOpTracker) {
    this.registries = registries;
    this.schema = schema;
    this.history = history;
    this.docOpTracker = docOpTracker;
    initContent(null);
  }

  /**
   * Initialize the content document.
   */
  protected void initContent(DocInitialization docInit) {
    this.useDiffFilter = false;
    if (docInit == null)
      docInit = DocOpUtil.asInitialization(new DocOpBuilder().build());

    this.doc = new ContentDocument(registries, docInit, schema);
    this.diffFilter = new DiffHighlightingFilter(this.doc.getDiffTarget(), docOpTracker);
    this.revisionIterator = history.getIterator();
  }

  public ContentDocument getDocument() {
    return doc;
  }

  private void consume(DocRevision revision) {

    revision.getDocOps(ops -> {

      for (int i = 0; i < ops.length; i++) {
        if (useDiffFilter) {
          try {
            diffFilter.consume(ops[i]);
          } catch (OperationException e) {
            throw new IllegalStateException(e);
          }
        } else
          doc.consume(ops[i]);
      }

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


  /**
   * Render the document at the specific version.
   */
  public void render(DocRevision revision) {
    history.getSnapshot(revision, docInit -> {
      initContent(docInit);
      revisionIterator = history.getIteratorAt(revision);
    });
  }

  protected void cosumeNextUntilRevision(DocRevision toRevision) {

    if (revisionIterator.hasNext())
      revisionIterator.next(revision -> {
        consume(revision);
        if (!revision.resultingVersion.equals(toRevision.resultingVersion))
          cosumeNextUntilRevision(toRevision);
    });

  }

  /**
   * Render the document at base version, highlighting differences with the
   * target version.
   *
   * @param baseRevision
   * @param targetRevision
   */
  public void renderDiff(DocRevision baseRevision, DocRevision targetRevision) {

    // if both versions are the same, no diffs to show
    if (baseRevision.resultingVersion.equals(targetRevision.resultingVersion)) {
      render(baseRevision);
      return;
    }

    // baseRevision must be always less that targetRevision
    if (baseRevision.resultingVersion.getVersion() > targetRevision.resultingVersion.getVersion()) {
      DocRevision aux = baseRevision;
      baseRevision = targetRevision;
      targetRevision = aux;
    }

    final DocRevision base = baseRevision;
    final DocRevision target = targetRevision;

    history.getSnapshot(baseRevision, docInit -> {
      initContent(docInit);
      revisionIterator = history.getIteratorAt(base);
      useDiffFilter = true;
      cosumeNextUntilRevision(target);
    });

  }

  public DocHistory.Iterator getHistoryIterator() {
    return history.getIterator();
  }

}
