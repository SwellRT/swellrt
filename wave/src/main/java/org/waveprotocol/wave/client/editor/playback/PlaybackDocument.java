package org.waveprotocol.wave.client.editor.playback;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.waveprotocol.wave.client.common.util.LogicalPanel;
import org.waveprotocol.wave.client.editor.Editor;
import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.wave.DocOpContext;
import org.waveprotocol.wave.client.wave.DocOpTracker;
import org.waveprotocol.wave.model.document.operation.DocInitialization;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;
import org.waveprotocol.wave.model.document.operation.impl.DocOpBuilder;
import org.waveprotocol.wave.model.document.operation.impl.DocOpUtil;
import org.waveprotocol.wave.model.document.util.Annotations;
import org.waveprotocol.wave.model.operation.OperationException;
import org.waveprotocol.wave.model.util.ReadableStringSet.Proc;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;

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

  @FunctionalInterface
  public interface RenderCallback {
    void onRenderCompleted(ContentDocument contentDocument);
  }

  private final Registries registries;

  private final DocumentSchema schema;

  private final DocHistory history;

  /** The actual content document */
  private ContentDocument doc;

  /** A filtered sink for the content document, to add diff marks as annotations */
  private DiffHighlightingFilter diffFilter;

  /** Enable or disable the diff filter */
  private boolean useDiffFilter = false;

  private DocHistory.Iterator revisionIterator;

  private boolean isInteractive = false;

  /** This tracker allows to pass DocOp metadata from DocHistory to DiffHighlightFilter */
  private final DocOpTracker opTracker = new DocOpTracker() {

    private Map<DocOp, DocOpContext> opContextMap = new HashMap<DocOp, DocOpContext>();

    @Override
    public void add(DocOp op, DocOpContext opCtx) {
      if (op != null && opCtx != null)
        opContextMap.put(op, opCtx);
    }

    @Override
    public Optional<DocOpContext> fetch(DocOp op) {
      return Optional.ofNullable(opContextMap.remove(op));
    }

  };


  /**
   * Create a new PlaybackDocument instance.
   *
   * @param registries
   *          Wave's Editor registries.
   * @param schema
   *          Document's schema.
   * @param history
   *          document history manager instance.
   * @param docOpTracker
   *          a tracker where to put metadata of processed DocOp's.
   */
  public PlaybackDocument(Registries registries, DocumentSchema schema, DocHistory history) {
    this.registries = registries;
    this.schema = schema;
    this.history = history;
    initContent(null);
  }

  /**
   * Create a PlaybackDocument instance with default registers and no schema
   * constraints.
   *
   * @param history
   *          instance of a document history.
   */
  public PlaybackDocument(DocHistory history) {
    this.registries = Editor.ROOT_REGISTRIES;
    this.schema = DocumentSchema.NO_SCHEMA_CONSTRAINTS;
    this.history = history;
    this.revisionIterator = history.getIterator();
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
    this.diffFilter = new DiffHighlightingFilter(this.doc.getDiffTarget(), opTracker);
    this.isInteractive = false;
  }

  public Element getElement() {

    if (!isInteractive) {
      isInteractive = true;
      doc.setInteractive(new LogicalPanel.Impl() {
        {
          setElement(Document.get().createDivElement());
        }
      });
    }

    return doc.getFullContentView().getDocumentElement().getImplNodelet();
  }

  public ContentDocument getDocument() {
    return doc;
  }

  private void consume(DocRevision revision) {

    if (useDiffFilter) {
      try {
        diffFilter.consume(revision.getDocOp());
      } catch (OperationException e) {
        throw new IllegalStateException(e);
      }
    } else {
      doc.consume(revision.getDocOp());
    }

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
  public void render(DocRevision revision, RenderCallback callback) {
    history.getSnapshot(revision, docInit -> {
      initContent(docInit);
      revisionIterator = history.getIteratorAt(revision);
      if (callback != null)
        callback.onRenderCompleted(doc);
    });
  }

  protected void cosumeNextUntilRevision(DocRevision toRevision, RenderCallback finalCallback) {

    revisionIterator.next(revision -> {

      if (revision != null) {

        consume(revision);
        if (!revision.resultingVersion.equals(toRevision.resultingVersion))
          cosumeNextUntilRevision(toRevision, finalCallback);
        else if (finalCallback != null)
          finalCallback.onRenderCompleted(doc);

      } else if (finalCallback != null)
        finalCallback.onRenderCompleted(doc);

    });

  }

  /**
   * Render the document at base version, highlighting differences with the
   * target version.
   *
   * @param baseRevision
   * @param targetRevision
   */
  public void renderDiff(DocRevision baseRevision, DocRevision targetRevision,
      RenderCallback callback) {

    // if both versions are the same, no diffs to show
    if (baseRevision.resultingVersion.equals(targetRevision.resultingVersion)) {
      render(baseRevision, callback);
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
      cosumeNextUntilRevision(target, callback);
    });

  }

  /** Return a new iterator for the current history */
  public DocHistory.Iterator getHistoryIterator() {
    return history.getIterator();

  }

}
