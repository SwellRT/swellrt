package org.waveprotocol.wave.client.wave;

import java.util.Optional;

import org.waveprotocol.wave.client.editor.playback.PlaybackDocument;
import org.waveprotocol.wave.model.document.operation.DocOp;

/**
 * Provide a caché of metadada for operations.
 * <p>
 * {@link PlaybackDocument} and {@link StaticChannelBinder} add elements to the
 * caché.
 * </p>
 * <p>
 * {@link DiffHighlightFilter} is a consumer of this caché.
 * </p>
 */
public interface DocOpTracker {

  public static final DocOpTracker VOID = new DocOpTracker() {

    @Override
    public Optional<DocOpContext> fetch(DocOp op) {
      return Optional.empty();
    }

    @Override
    public void add(DocOp op, DocOpContext opCtx) {
      // Nothing to do
    }

  };

  /**
   * Add to the cache a context for a DocOp.
   *
   * @param op the doc op
   * @param opCtx the context
   */
  public void add(DocOp op, DocOpContext opCtx);

  /**
   * Get context information for the op. Implementations can forget the context
   * once it has been retrieved.
   */
  public Optional<DocOpContext> fetch(DocOp op);

}
