package org.waveprotocol.wave.client.editor.playback;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.waveprotocol.wave.client.editor.content.ContentDocument;
import org.waveprotocol.wave.client.editor.content.DiffHighlightingFilter;
import org.waveprotocol.wave.client.editor.content.Registries;
import org.waveprotocol.wave.client.editor.playback.DocHistory.Delta;
import org.waveprotocol.wave.model.document.operation.DocOp;
import org.waveprotocol.wave.model.document.operation.algorithm.DocOpInverter;
import org.waveprotocol.wave.model.document.operation.automaton.DocumentSchema;

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

  public static Delta invert(Delta delta) {

    List<DocOp> invertedOps = new ArrayList<DocOp>(delta.ops.size());

    for (int i = delta.ops.size() - 1; i >= 0; i--) {
      invertedOps.add(DocOpInverter.invert(delta.ops.get(i)));
    }

    return new Delta(invertedOps, delta.context);
  }

  private ContentDocument doc;

  private DiffHighlightingFilter diffFilter;

  private DocHistory deltas;

  private boolean useDiffFilter = false;

  public PlaybackDocument(Registries registries, DocumentSchema schema, DocHistory deltas) {
    this.doc = new ContentDocument(schema);
    this.doc.setRegistries(registries);
    this.deltas = deltas;
    this.diffFilter = new DiffHighlightingFilter(this.doc.getDiffTarget());
  }

  public ContentDocument getDocument() {
    return doc;
  }

  public void renderDiffs(boolean isOn) {
    useDiffFilter = isOn;
  }

  private void consume(Delta delta) {
    delta.ops.forEach(op -> {
      doc.consume(op);
    });
  }

  public void nextDelta() {

    Optional<Delta> opDeltas = deltas.nextOpDelta();

    if (!opDeltas.isPresent())
      return;

    consume(opDeltas.get());

  }

  public void prevDelta() {

    Optional<Delta> opDeltas = deltas.prevOpDelta();

    if (!opDeltas.isPresent())
      return;

    consume(invert(opDeltas.get()));

  }


}
