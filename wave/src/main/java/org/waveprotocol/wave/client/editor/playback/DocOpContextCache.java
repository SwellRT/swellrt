package org.waveprotocol.wave.client.editor.playback;

import java.util.Optional;

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
public interface DocOpContextCache {

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
